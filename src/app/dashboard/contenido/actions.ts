"use server";

import { revalidatePath } from "next/cache";
import { esCategoria } from "@/lib/content/categorias";
import { leerFuentes } from "@/lib/content/fuentes";
import { slugify } from "@/lib/content/slug";
import { puedeTransicionar } from "@/lib/content/workflow";
import { requerirPermiso } from "@/lib/auth/session";
import {
  articuloPorId,
  borrarArticulo,
  cambiarEstadoArticulo,
  insertarArticulo,
  modificarArticulo,
  reemplazarFuentesDeArticulo,
  slugEnUso,
} from "@/lib/db/queries";
import type { Articulo, EstadoContenido } from "@/types/content";
import type { Usuario } from "@/types/user";

export type EstadoForm = { ok?: boolean; error?: string };

const TITULO_MAX = 160;
const RESUMEN_MAX = 400;
const CONTENIDO_MAX = 50_000;

function texto(formData: FormData, campo: string) {
  return String(formData.get(campo) ?? "").trim();
}

// El autor solo toca lo suyo; el administrador, todo.
function esDuenoOAdmin(usuario: Usuario, articulo: Articulo) {
  return usuario.rol === "administrador" || articulo.autorId === usuario.id;
}

// Solo se edita mientras el artículo no entró a revisión.
function esEditable(articulo: Articulo) {
  return articulo.estado === "borrador" || articulo.estado === "asistido_ia";
}

async function slugDisponible(titulo: string) {
  const base = slugify(titulo) || "articulo";
  let slug = base;
  for (let n = 2; await slugEnUso(slug); n++) slug = `${base}-${n}`;
  return slug;
}

export async function guardarArticulo(
  _previo: EstadoForm,
  formData: FormData,
): Promise<EstadoForm> {
  // Toda Server Action revalida sesión y permiso: no depende de la UI.
  const actor = await requerirPermiso("gestionar_contenido");

  const id = texto(formData, "id");
  const titulo = texto(formData, "titulo");
  const categoria = texto(formData, "categoria");
  const resumen = texto(formData, "resumen");
  const contenido = texto(formData, "contenido");
  // Con "accion=enviar" se guarda y se manda a revisión en un solo paso.
  const enviar = texto(formData, "accion") === "enviar";

  if (titulo.length < 5 || titulo.length > TITULO_MAX) {
    return {
      error: `El título debe tener entre 5 y ${TITULO_MAX} caracteres.`,
    };
  }
  if (!esCategoria(categoria)) return { error: "Selecciona una categoría." };
  if (resumen.length < 10 || resumen.length > RESUMEN_MAX) {
    return {
      error: `El resumen debe tener entre 10 y ${RESUMEN_MAX} caracteres.`,
    };
  }
  if (contenido.length > CONTENIDO_MAX) {
    return { error: `El contenido supera los ${CONTENIDO_MAX} caracteres.` };
  }
  const fuentes = leerFuentes(texto(formData, "fuentes"));
  if (fuentes.error !== undefined) return { error: fuentes.error };
  if (enviar && contenido.length === 0) {
    return { error: "Escribe el contenido antes de enviarlo a revisión." };
  }

  let articuloId = id;
  let estadoActual: EstadoContenido = "borrador";

  try {
    if (!id) {
      articuloId = await insertarArticulo({
        titulo,
        categoria,
        resumen,
        contenido,
        slug: await slugDisponible(titulo),
        autorId: actor.id,
      });
    } else {
      const actual = await articuloPorId(id);
      if (!actual) return { error: "El artículo ya no existe." };
      if (!esDuenoOAdmin(actor, actual)) {
        return { error: "No puedes editar este artículo." };
      }
      if (!esEditable(actual)) {
        return { error: "El artículo ya no se puede editar en este estado." };
      }
      estadoActual = actual.estado;
      await modificarArticulo(id, { titulo, categoria, resumen, contenido });
    }
  } catch {
    return { error: "No se pudo guardar el artículo. Inténtalo de nuevo." };
  }

  try {
    if (
      !(await reemplazarFuentesDeArticulo(
        articuloId,
        fuentes.fuentes,
        actor.id,
      ))
    ) {
      return {
        error:
          "Se guardó el artículo, pero una fuente elegida ya no existe. Ábrelo de nuevo y revisa las fuentes.",
      };
    }
  } catch {
    return {
      error: "Se guardó el artículo, pero no se pudieron guardar las fuentes.",
    };
  }

  if (enviar) {
    if (!puedeTransicionar(estadoActual, "en_revision")) {
      return {
        error: "El artículo no se puede enviar desde su estado actual.",
      };
    }
    try {
      if (
        !(await cambiarEstadoArticulo(
          articuloId,
          estadoActual,
          "en_revision",
          actor.id,
        ))
      ) {
        return { error: "El artículo cambió de estado. Recarga la página." };
      }
    } catch {
      return { error: "Se guardó, pero no se pudo enviar a revisión." };
    }
    revalidatePath("/dashboard/revision");
  }

  revalidatePath("/dashboard/contenido");
  return { ok: true };
}

// Solo el administrador elimina, en cualquier estado (también publicados).
export async function eliminarArticulo(
  _previo: EstadoForm,
  formData: FormData,
): Promise<EstadoForm> {
  const actor = await requerirPermiso("gestionar_contenido");
  if (actor.rol !== "administrador") {
    return { error: "Solo un administrador puede eliminar artículos." };
  }

  const id = texto(formData, "id");
  if (!id || !(await articuloPorId(id))) {
    return { error: "El artículo ya no existe." };
  }

  try {
    await borrarArticulo(id);
  } catch {
    return { error: "No se pudo eliminar el artículo." };
  }

  revalidatePath("/dashboard/contenido");
  revalidatePath("/dashboard/revision");
  revalidatePath("/", "layout");
  return { ok: true };
}
