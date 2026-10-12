import { articuloPublicadoPorSlug } from "@/lib/db/queries";
import { imagenSocial } from "@/lib/seo/og";

export const dynamic = "force-dynamic";

// Imagen social de un artículo: su título y categoría sobre la marca.
export async function GET(
  _request: Request,
  context: { params: Promise<{ slug: string }> },
) {
  const { slug } = await context.params;
  const articulo = await articuloPublicadoPorSlug(slug);

  if (!articulo) {
    // Un slug inexistente no debe generar imágenes: 404 real.
    return new Response("No encontrado", { status: 404 });
  }

  return imagenSocial({
    titulo: articulo.titulo,
    etiqueta: articulo.categoria,
  });
}
