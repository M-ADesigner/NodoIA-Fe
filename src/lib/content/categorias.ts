import type { CategoriaContenido } from "@/types/content";

export const CATEGORIAS: CategoriaContenido[] = [
  "Artículo",
  "Boletín",
  "Noticia institucional",
];

export function esCategoria(valor: unknown): valor is CategoriaContenido {
  return typeof valor === "string" && (CATEGORIAS as string[]).includes(valor);
}
