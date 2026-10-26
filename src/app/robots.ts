import type { MetadataRoute } from "next";
import { absoluteUrl } from "@/lib/seo/site";

// Dinámico: la URL pública se toma de las variables de entorno en runtime.
export const dynamic = "force-dynamic";

export default function robots(): MetadataRoute.Robots {
  return {
    rules: [
      {
        userAgent: "*",
        allow: "/",
        // Panel interno, login y endpoints: no tienen valor para buscadores.
        disallow: ["/dashboard", "/login", "/api/"],
      },
    ],
    sitemap: absoluteUrl("/sitemap.xml"),
  };
}
