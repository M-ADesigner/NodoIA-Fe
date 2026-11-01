"use server";

import { revalidatePath } from "next/cache";
import { requerirPermiso } from "@/lib/auth/session";
import { puedeTransicionar } from "@/lib/content/workflow";
import { articuloPorId, cambiarEstadoArticulo } from "@/lib/db/queries";
import type { EstadoContenido } from "@/types/content";

const COMENTARIO_MIN = 10;
const COMENTARIO_MAX = 500;

// Devolver un artículo exige explicar el motivo al autor.
const EXIGEN_MOTIVO: EstadoContenido[] = ["borrador", "en_revision"];

export type EstadoForm = { ok?: boolean; error?: string };

// Decisiones que puede tomar quien revisa, sobre artículos en revisión o aprobados.
const DECISIONES: EstadoContenido[] = [
  "aprobado",
  "borrador",
  "en_revision",
  "publicado",
];

export async function decidirArticulo(
  _previo: EstadoForm,
  formData: FormData,
): Promise<EstadoForm> {
  const actor = await requerirPermiso("revisar");

  const id = String(formData.get("id") ?? "");
  const hacia = String(formData.get("hacia") ?? "") as EstadoContenido;
  if (!DECISIONES.includes(hacia)) return { error: "Acción no válida." };

  const comentario = String(formData.get("comentario") ?? "").trim();
  if (comentario.length > COMENTARIO_MAX) {
    return { error: `El comentario supera los ${COMENTARIO_MAX} caracteres.` };
  }
  if (EXIGEN_MOTIVO.includes(hacia) && comentario.length < COMENTARIO_MIN) {
    return {
      error: `Indica el motivo (mínimo ${COMENTARIO_MIN} caracteres) para que el autor sepa qué ajustar.`,
    };
  }

  const articulo = id ? await articuloPorId(id) : undefined;
  if (!articulo) return { error: "El artículo ya no existe." };
  if (!puedeTransicionar(articulo.estado, hacia)) {
    return { error: "Esa acción no es válida desde el estado actual." };
  }

  try {
    if (
      !(await cambiarEstadoArticulo(
        id,
        articulo.estado,
        hacia,
        actor.id,
        comentario,
      ))
    ) {
      return { error: "El artículo cambió de estado. Recarga la página." };
    }
  } catch {
    return { error: "No se pudo actualizar el artículo." };
  }

  revalidatePath("/dashboard/revision");
  revalidatePath("/dashboard/contenido");
  if (hacia === "publicado") revalidatePath("/", "layout");
  return { ok: true };
}
