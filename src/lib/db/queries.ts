import "server-only";
import { desc, eq } from "drizzle-orm";
import type { Articulo } from "@/types/content";
import type { Usuario } from "@/types/user";
import { db } from "./client";
import { articulos, usuarios } from "./schema";

const MESES = [
  "Enero",
  "Febrero",
  "Marzo",
  "Abril",
  "Mayo",
  "Junio",
  "Julio",
  "Agosto",
  "Septiembre",
  "Octubre",
  "Noviembre",
  "Diciembre",
];

function fechaRelativa(fecha: Date): string {
  const minutos = Math.max(
    0,
    Math.floor((Date.now() - fecha.getTime()) / 60000),
  );
  if (minutos < 60) {
    return minutos < 2 ? "Hace un momento" : `Hace ${minutos} minutos`;
  }
  const horas = Math.floor(minutos / 60);
  if (horas < 24) return horas === 1 ? "Hace 1 hora" : `Hace ${horas} horas`;
  const dias = Math.floor(horas / 24);
  if (dias === 1) return "Ayer";
  if (dias < 7) return `Hace ${dias} días`;
  if (dias < 30) {
    const semanas = Math.floor(dias / 7);
    return semanas === 1 ? "Hace 1 semana" : `Hace ${semanas} semanas`;
  }
  return fecha.toLocaleDateString("es-CL", {
    day: "numeric",
    month: "long",
    year: "numeric",
  });
}

function aArticulo(fila: typeof articulos.$inferSelect): Articulo {
  return {
    id: fila.id,
    slug: fila.slug,
    titulo: fila.titulo,
    categoria: fila.categoria,
    resumen: fila.resumen,
    contenido: fila.contenido,
    estado: fila.estado,
    autorId: fila.autorId,
    actualizadoEn: fechaRelativa(fila.actualizadoEn),
    publicadoEn: fila.publicadoEn ? fechaRelativa(fila.publicadoEn) : undefined,
  };
}

function aUsuario(fila: typeof usuarios.$inferSelect): Usuario {
  return {
    id: fila.id,
    nombre: fila.nombre,
    correo: fila.correo,
    rol: fila.rol,
    miembroDesde: `${MESES[fila.miembroDesde.getMonth()]} ${fila.miembroDesde.getFullYear()}`,
  };
}

export async function listarArticulos(): Promise<Articulo[]> {
  const filas = await db
    .select()
    .from(articulos)
    .orderBy(desc(articulos.actualizadoEn));
  return filas.map(aArticulo);
}

export async function articulosPublicados(): Promise<Articulo[]> {
  const filas = await db
    .select()
    .from(articulos)
    .where(eq(articulos.estado, "publicado"))
    .orderBy(desc(articulos.publicadoEn));
  return filas.map(aArticulo);
}

export async function articulosEnRevision(): Promise<Articulo[]> {
  const filas = await db
    .select()
    .from(articulos)
    .where(eq(articulos.estado, "en_revision"))
    .orderBy(desc(articulos.actualizadoEn));
  return filas.map(aArticulo);
}

export async function articuloPublicadoPorSlug(
  slug: string,
): Promise<Articulo | undefined> {
  const [fila] = await db
    .select()
    .from(articulos)
    .where(eq(articulos.slug, slug));
  return fila && fila.estado === "publicado" ? aArticulo(fila) : undefined;
}

export async function listarUsuarios(): Promise<Usuario[]> {
  const filas = await db.select().from(usuarios).orderBy(usuarios.nombre);
  return filas.map(aUsuario);
}
