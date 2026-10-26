import type { Metadata } from "next";
import { SITE_LOCALE, SITE_NAME, truncate } from "@/lib/seo/site";

const OG_SIZE = { width: 1200, height: 630 };

interface OpcionesPagina {
  /** Título de la página (el layout raíz agrega " | CCHIA"). */
  title: string;
  description: string;
  /** Ruta canónica, p. ej. "/articulos". */
  path: string;
  /** Ruta de la imagen social; por defecto la genérica del sitio. */
  image?: string;
  imageAlt?: string;
  type?: "website" | "article";
  publishedTime?: string;
  modifiedTime?: string;
  section?: string;
}

// `openGraph` y `twitter` de una página REEMPLAZAN a los del layout (no se
// fusionan), por eso se arman completos aquí.
export function construirMetadata(opciones: OpcionesPagina): Metadata {
  const {
    title,
    path,
    image = "/og",
    imageAlt = title,
    type = "website",
  } = opciones;
  const description = truncate(opciones.description, 160);

  return {
    title,
    description,
    alternates: { canonical: path },
    openGraph: {
      type,
      siteName: SITE_NAME,
      locale: SITE_LOCALE,
      url: path,
      title,
      description,
      images: [{ url: image, ...OG_SIZE, alt: imageAlt }],
      ...(type === "article"
        ? {
            publishedTime: opciones.publishedTime,
            modifiedTime: opciones.modifiedTime,
            section: opciones.section,
          }
        : {}),
    },
    twitter: {
      card: "summary_large_image",
      title,
      description,
      images: [image],
    },
  };
}

export const NO_INDEXAR: Metadata["robots"] = {
  index: false,
  follow: false,
  nocache: true,
  googleBot: { index: false, follow: false },
};
