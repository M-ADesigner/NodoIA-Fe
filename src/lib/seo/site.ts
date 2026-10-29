// Datos del sitio usados por metadata, sitemap, robots y JSON-LD.

export const SITE_NAME = "CCHIA";
export const SITE_LEGAL_NAME = "Cámara Chilena de Inteligencia Artificial";
export const SITE_DESCRIPTION =
  "Cámara Chilena de Inteligencia Artificial (CCHIA): noticias, boletines y análisis de IA en Chile. Eventos, networking y recursos para socios.";
export const SITE_LOCALE = "es_CL";
export const SITE_LANG = "es-CL";
export const THEME_COLOR = "#1a365d";

/** Formulario de inscripción de socios (Google Forms de la CCHIA). */
export const HAZTE_SOCIO_URL =
  "https://docs.google.com/forms/d/e/1FAIpQLSdRNGNM10IHgnvAnfOl75xvsgVm1hqwepqN-uqEy4ZjFOiQEg/viewform?usp=send_form";

export const LOGO_PATH = "/images/Logo_CCHIA.png";
export const LOGO_SIZE = { width: 512, height: 212 };

/**
 * URL pública canónica del sitio, sin barra final.
 * Orden: SITE_URL (dominio propio) → NEXT_PUBLIC_SITE_URL →
 * RAILWAY_PUBLIC_DOMAIN (dominio generado por Railway) → localhost.
 */
export function siteUrl(): string {
  const explicita = process.env.SITE_URL ?? process.env.NEXT_PUBLIC_SITE_URL;
  const railway = process.env.RAILWAY_PUBLIC_DOMAIN;
  const bruta =
    explicita ?? (railway ? `https://${railway}` : "http://localhost:3000");
  const conProtocolo = /^https?:\/\//i.test(bruta) ? bruta : `https://${bruta}`;
  return conProtocolo.replace(/\/+$/, "");
}

export function absoluteUrl(path = "/"): string {
  return `${siteUrl()}${path.startsWith("/") ? path : `/${path}`}`;
}

/** Ruta pública de un artículo; el slug se codifica por si trae caracteres no válidos. */
export function rutaArticulo(slug: string): string {
  return `/articulos/${encodeURIComponent(slug)}`;
}

/** Recorta a `max` caracteres sin cortar palabras y limpia espacios. */
export function truncate(texto: string, max = 160): string {
  const limpio = texto.replace(/\s+/g, " ").trim();
  if (limpio.length <= max) return limpio;
  const corte = limpio.slice(0, max - 1);
  const ultimoEspacio = corte.lastIndexOf(" ");
  return `${(ultimoEspacio > max * 0.6 ? corte.slice(0, ultimoEspacio) : corte).replace(/[\s.,;:]+$/, "")}…`;
}
