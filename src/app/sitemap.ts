import type { MetadataRoute } from "next";
import { articulosPublicados } from "@/lib/db/queries";
import { absoluteUrl, rutaArticulo } from "@/lib/seo/site";

// Se genera por petición: los artículos publicados cambian sin redeploy.
export const dynamic = "force-dynamic";

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const ahora = new Date();

  let articulos: Awaited<ReturnType<typeof articulosPublicados>> = [];
  try {
    articulos = await articulosPublicados();
  } catch {
    // Si la BD no responde, el sitemap igual lista las rutas estáticas.
  }

  const fechas = articulos
    .map((a) => a.actualizadoEnIso ?? a.publicadoEnIso)
    .filter((fecha): fecha is string => Boolean(fecha))
    .map((fecha) => new Date(fecha));
  const ultimaActualizacion = fechas.length
    ? new Date(Math.max(...fechas.map((fecha) => fecha.getTime())))
    : ahora;

  return [
    {
      url: absoluteUrl("/"),
      lastModified: ultimaActualizacion,
      changeFrequency: "weekly",
      priority: 1,
    },
    {
      url: absoluteUrl("/articulos"),
      lastModified: ultimaActualizacion,
      changeFrequency: "daily",
      priority: 0.8,
    },
    ...articulos.map((articulo) => ({
      url: absoluteUrl(rutaArticulo(articulo.slug)),
      lastModified: new Date(
        articulo.actualizadoEnIso ?? articulo.publicadoEnIso ?? ahora,
      ),
      changeFrequency: "monthly" as const,
      priority: 0.6,
    })),
  ];
}
