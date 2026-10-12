import { imagenSocial } from "@/lib/seo/og";

// Imagen social por defecto del sitio (Open Graph / Twitter).
export async function GET() {
  return imagenSocial({
    titulo: "Impulsando la Inteligencia Artificial en Chile",
    etiqueta: "CCHIA",
  });
}
