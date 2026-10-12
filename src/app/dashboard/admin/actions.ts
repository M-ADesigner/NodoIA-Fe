"use server";

import { revalidatePath } from "next/cache";
import { esRol } from "@/lib/auth/roles";
import { hashPassword } from "@/lib/auth/password";
import { requerirPermiso } from "@/lib/auth/session";
import {
  borrarUsuario,
  cantidadArticulosDeUsuario,
  correoEnUso,
  insertarUsuario,
  modificarUsuario,
  otrosAdministradoresActivos,
  usuarioActivoPorId,
} from "@/lib/db/queries";

export type EstadoForm = { ok?: boolean; error?: string };

const CORREO_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PASSWORD_MIN = 8;

function texto(formData: FormData, campo: string) {
  return String(formData.get(campo) ?? "").trim();
}

export async function guardarUsuario(
  _previo: EstadoForm,
  formData: FormData,
): Promise<EstadoForm> {
  // Toda Server Action revalida sesión y permiso: no depende de la UI.
  const actor = await requerirPermiso("gestionar_usuarios");

  const id = texto(formData, "id");
  const nombre = texto(formData, "nombre");
  const correo = texto(formData, "correo").toLowerCase();
  const rol = texto(formData, "rol");
  const password = String(formData.get("password") ?? "");
  const esEdicion = id !== "";
  const activo = esEdicion ? formData.get("activo") === "on" : true;

  if (nombre.length < 2 || nombre.length > 80) {
    return { error: "El nombre debe tener entre 2 y 80 caracteres." };
  }
  if (!CORREO_RE.test(correo)) return { error: "El correo no es válido." };
  if (!esRol(rol)) return { error: "Selecciona un rol válido." };
  if ((!esEdicion || password) && password.length < PASSWORD_MIN) {
    return {
      error: `La contraseña debe tener al menos ${PASSWORD_MIN} caracteres.`,
    };
  }
  if (await correoEnUso(correo, esEdicion ? id : undefined)) {
    return { error: "Ya existe un usuario con ese correo." };
  }

  try {
    if (!esEdicion) {
      await insertarUsuario({
        nombre,
        correo,
        rol,
        passwordHash: await hashPassword(password),
      });
    } else {
      const actual = await usuarioActivoPorId(id);
      if (!actual) return { error: "El usuario ya no existe." };

      const dejaDeSerAdminActivo =
        actual.rol === "administrador" &&
        actual.activo &&
        (rol !== "administrador" || !activo);

      if (dejaDeSerAdminActivo) {
        if (actual.id === actor.id) {
          return {
            error:
              "No puedes quitarte el rol de administrador ni desactivarte.",
          };
        }
        if ((await otrosAdministradoresActivos(id)) === 0) {
          return { error: "Debe quedar al menos un administrador activo." };
        }
      }

      await modificarUsuario(id, {
        nombre,
        correo,
        rol,
        activo,
        passwordHash: password ? await hashPassword(password) : undefined,
      });
    }
  } catch {
    return { error: "No se pudo guardar el usuario. Inténtalo de nuevo." };
  }

  revalidatePath("/dashboard/admin");
  return { ok: true };
}

export async function eliminarUsuario(
  _previo: EstadoForm,
  formData: FormData,
): Promise<EstadoForm> {
  const actor = await requerirPermiso("gestionar_usuarios");
  const id = texto(formData, "id");

  if (!id) return { error: "Usuario no válido." };
  if (id === actor.id) return { error: "No puedes eliminar tu propia cuenta." };

  const objetivo = await usuarioActivoPorId(id);
  if (!objetivo) return { error: "El usuario ya no existe." };

  if (
    objetivo.rol === "administrador" &&
    objetivo.activo &&
    (await otrosAdministradoresActivos(id)) === 0
  ) {
    return { error: "Debe quedar al menos un administrador activo." };
  }

  const articulos = await cantidadArticulosDeUsuario(id);
  if (articulos > 0) {
    return {
      error: `Tiene ${articulos} artículo(s) asociados. Desactívalo en vez de eliminarlo.`,
    };
  }

  try {
    await borrarUsuario(id);
  } catch {
    return { error: "No se pudo eliminar el usuario." };
  }

  revalidatePath("/dashboard/admin");
  return { ok: true };
}
