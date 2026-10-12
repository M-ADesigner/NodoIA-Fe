import type { Articulo } from "@/types/content";
import {
  absoluteUrl,
  LOGO_PATH,
  LOGO_SIZE,
  rutaArticulo,
  SITE_DESCRIPTION,
  SITE_LANG,
  SITE_LEGAL_NAME,
  SITE_NAME,
  siteUrl,
  truncate,
} from "@/lib/seo/site";

type Datos = Record<string, unknown>;

// `<` se escapa para que un texto con "</script>" no pueda romper el bloque.
export function JsonLd({ data }: { data: Datos | Datos[] }) {
  return (
    <script
      type="application/ld+json"
      dangerouslySetInnerHTML={{
        __html: JSON.stringify(data).replace(/</g, "\\u003c"),
      }}
    />
  );
}

const ORGANIZATION_ID = () => `${siteUrl()}/#organization`;
const WEBSITE_ID = () => `${siteUrl()}/#website`;

export function organizacionLd(): Datos {
  return {
    "@context": "https://schema.org",
    "@type": "Organization",
    "@id": ORGANIZATION_ID(),
    name: SITE_LEGAL_NAME,
    alternateName: SITE_NAME,
    url: absoluteUrl("/"),
    description: SITE_DESCRIPTION,
    logo: {
      "@type": "ImageObject",
      url: absoluteUrl(LOGO_PATH),
      width: LOGO_SIZE.width,
      height: LOGO_SIZE.height,
    },
    areaServed: { "@type": "Country", name: "Chile" },
  };
}

export function sitioWebLd(): Datos {
  return {
    "@context": "https://schema.org",
    "@type": "WebSite",
    "@id": WEBSITE_ID(),
    name: SITE_LEGAL_NAME,
    alternateName: SITE_NAME,
    url: absoluteUrl("/"),
    inLanguage: SITE_LANG,
    publisher: { "@id": ORGANIZATION_ID() },
  };
}

export function migasDePanLd(items: { nombre: string; path: string }[]): Datos {
  return {
    "@context": "https://schema.org",
    "@type": "BreadcrumbList",
    itemListElement: items.map((item, index) => ({
      "@type": "ListItem",
      position: index + 1,
      name: item.nombre,
      item: absoluteUrl(item.path),
    })),
  };
}

export function listadoArticulosLd(
  titulo: string,
  path: string,
  articulos: Articulo[],
): Datos {
  return {
    "@context": "https://schema.org",
    "@type": "CollectionPage",
    name: titulo,
    url: absoluteUrl(path),
    inLanguage: SITE_LANG,
    isPartOf: { "@id": WEBSITE_ID() },
    mainEntity: {
      "@type": "ItemList",
      itemListElement: articulos.map((articulo, index) => ({
        "@type": "ListItem",
        position: index + 1,
        url: absoluteUrl(rutaArticulo(articulo.slug)),
        name: articulo.titulo,
      })),
    },
  };
}

export function articuloLd(articulo: Articulo): Datos {
  const url = absoluteUrl(rutaArticulo(articulo.slug));
  return {
    "@context": "https://schema.org",
    "@type":
      articulo.categoria === "Noticia institucional"
        ? "NewsArticle"
        : "Article",
    "@id": `${url}#article`,
    mainEntityOfPage: { "@type": "WebPage", "@id": url },
    url,
    headline: truncate(articulo.titulo, 110),
    description: truncate(articulo.resumen, 200),
    articleSection: articulo.categoria,
    inLanguage: SITE_LANG,
    image: [absoluteUrl(`/og/articulos/${encodeURIComponent(articulo.slug)}`)],
    datePublished: articulo.publicadoEnIso,
    dateModified: articulo.actualizadoEnIso ?? articulo.publicadoEnIso,
    author: { "@id": ORGANIZATION_ID() },
    publisher: { "@id": ORGANIZATION_ID() },
    isPartOf: { "@id": WEBSITE_ID() },
  };
}
