"use server";

import { redirect } from "next/navigation";
import { verifyPassword } from "@/lib/auth/password";
import { vistasPermitidas } from "@/lib/auth/roles";
import {
  cerrarSesionCookie,
  crearSesion,
  usuarioActual,
} from "@/lib/auth/session";
import {
  credencialesPorCorreo,
  intentosFallidosDesde,
  invalidarSesiones,
  limpiarIntentosFallidos,
  registrarAcceso,
  registrarIntentoFallido,
} from "@/lib/db/queries";

export type LoginState = { error?: string };

const ERROR_GENERICO = "Correo o contraseña incorrectos.";

// Hash de relleno para que el tiempo de respuesta no delate si el correo existe.
const HASH_RELLENO =
  "scrypt$00000000000000000000000000000000$" + "0".repeat(128);

// Límite por correo, guardado en la base de datos: sobrevive a reinicios y
// despliegues y se comparte entre instancias. No es un WAF: no frena ataques
// distribuidos ni contra muchos correos distintos.
const INTENTOS_MAX = 5;
const VENTANA_MS = 10 * 60 * 1000;

async function bloqueado(clave: string) {
  const desde = new Date(Date.now() - VENTANA_MS);
  return (await intentosFallidosDesde(clave, desde)) >= INTENTOS_MAX;
}

async function registrarFallo(clave: string) {
  await registrarIntentoFallido(clave, new Date(Date.now() - VENTANA_MS));
}

function destinoSeguro(next: FormDataEntryValue | null): string | null {
  if (typeof next !== "string") return null;
  // Solo rutas internas del panel: evita redirecciones abiertas.
  if (!next.startsWith("/dashboard") || next.startsWith("//")) return null;
  return next;
}

export async function iniciarSesion(
  _previo: LoginState,
  formData: FormData,
): Promise<LoginState> {
  const correo = String(formData.get("correo") ?? "")
    .trim()
    .toLowerCase();
  const password = String(formData.get("password") ?? "");

  if (!correo || !password) {
    return { error: "Ingresa tu correo y contraseña." };
  }
  if (await bloqueado(correo)) {
    return { error: "Demasiados intentos. Espera unos minutos." };
  }

  const credenciales = await credencialesPorCorreo(correo);
  const valido = await verifyPassword(
    password,
    credenciales?.passwordHash ?? HASH_RELLENO,
  );

  if (!credenciales || !valido) {
    await registrarFallo(correo);
    return { error: ERROR_GENERICO };
  }

  // Solo se revela que la cuenta está deshabilitada si la contraseña es
  // correcta: así nadie puede averiguar qué correos existen o están inactivos.
  if (!credenciales.usuario.activo) {
    return {
      error:
        "Tu cuenta ha sido deshabilitada. Contacta a administración para volver a acceder.",
    };
  }

  await limpiarIntentosFallidos(correo);
  await crearSesion({
    ...credenciales.usuario,
    sesionVersion: credenciales.sesionVersion,
  });
  await registrarAcceso(credenciales.usuario.id);

  const permitidas = vistasPermitidas(credenciales.usuario.rol);
  const destino = destinoSeguro(formData.get("next"));
  const puedeVerDestino =
    destino && permitidas.some((vista) => destino.startsWith(vista.href));
  redirect(puedeVerDestino ? destino : (permitidas[0]?.href ?? "/"));
}

export async function cerrarSesion() {
  // Además de borrar la cookie, invalida el token por si alguien lo copió.
  const actual = await usuarioActual();
  if (actual) await invalidarSesiones(actual.id);
  await cerrarSesionCookie();
  redirect("/login");
}
