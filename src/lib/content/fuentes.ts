import type { TipoFuente } from "@/types/content";

export const TIPOS_FUENTE: TipoFuente[] = [
  "Informe",
  "Noticia",
  "Estudio",
  "Sitio oficial",
  "Otro",
];

export const MAX_FUENTES = 20;
const TITULO_MIN = 3;
const TITULO_MAX = 200;
const URL_MAX = 500;
const ORGANISMO_MAX = 120;

export function esTipoFuente(valor: unknown): valor is TipoFuente {
  return (
    typeof valor === "string" && (TIPOS_FUENTE as string[]).includes(valor)
  );
}

// Lo que viaja desde el formulario: una fuente del catálogo (solo `id`) o una
// nueva (sin `id`, con sus datos).
export type FuenteEntrada =
  | { id: string }
  | { titulo: string; url: string; tipo: TipoFuente; organismo?: string };

export type ResultadoFuentes =
  { fuentes: FuenteEntrada[]; error?: undefined } | { error: string };

// Solo http(s): una URL `javascript:` en un enlace público sería un XSS.
export function normalizarUrl(valor: string): string | null {
  try {
    const url = new URL(valor.trim());
    if (url.protocol !== "http:" && url.protocol !== "https:") return null;
    return url.href.length <= URL_MAX ? url.href : null;
  } catch {
    return null;
  }
}

export function leerFuentes(crudo: string): ResultadoFuentes {
  if (!crudo) return { fuentes: [] };

  let datos: unknown;
  try {
    datos = JSON.parse(crudo);
  } catch {
    return { error: "Las fuentes no tienen un formato válido." };
  }
  if (!Array.isArray(datos)) {
    return { error: "Las fuentes no tienen un formato válido." };
  }
  if (datos.length > MAX_FUENTES) {
    return { error: `Máximo ${MAX_FUENTES} fuentes por artículo.` };
  }

  const fuentes: FuenteEntrada[] = [];
  for (const item of datos) {
    if (typeof item !== "object" || item === null) {
      return { error: "Las fuentes no tienen un formato válido." };
    }
    const { id, titulo, url, tipo, organismo } = item as Record<
      string,
      unknown
    >;

    if (typeof id === "string" && id) {
      fuentes.push({ id });
      continue;
    }

    const tituloLimpio = typeof titulo === "string" ? titulo.trim() : "";
    if (tituloLimpio.length < TITULO_MIN || tituloLimpio.length > TITULO_MAX) {
      return {
        error: `El título de una fuente debe tener entre ${TITULO_MIN} y ${TITULO_MAX} caracteres.`,
      };
    }
    const urlNormalizada = typeof url === "string" ? normalizarUrl(url) : null;
    if (!urlNormalizada) {
      return {
        error: `La fuente «${tituloLimpio}» necesita una URL válida que empiece con http:// o https://.`,
      };
    }
    if (!esTipoFuente(tipo)) {
      return { error: `Elige un tipo para la fuente «${tituloLimpio}».` };
    }
    const organismoLimpio =
      typeof organismo === "string" ? organismo.trim() : "";
    if (organismoLimpio.length > ORGANISMO_MAX) {
      return {
        error: `El organismo no puede superar los ${ORGANISMO_MAX} caracteres.`,
      };
    }
    fuentes.push({
      titulo: tituloLimpio,
      url: urlNormalizada,
      tipo,
      ...(organismoLimpio ? { organismo: organismoLimpio } : {}),
    });
  }
  return { fuentes };
}
