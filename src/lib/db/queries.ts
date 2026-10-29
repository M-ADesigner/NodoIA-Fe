import "server-only";
import { cache } from "react";
import { and, count, desc, eq, ne } from "drizzle-orm";
import { esRol } from "@/lib/auth/roles";
import type { Articulo } from "@/types/content";
import type { Rol, Usuario } from "@/types/user";
import { db } from "./client";
import { articulos, roles, usuarios } from "./schema";

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
    actualizadoEnIso: fila.actualizadoEn.toISOString(),
    publicadoEnIso: fila.publicadoEn?.toISOString(),
  };
}

type FilaUsuario = {
  id: string;
  nombre: string;
  correo: string;
  rol: string;
  activo: boolean;
  miembroDesde: Date;
};

const COLUMNAS_USUARIO = {
  id: usuarios.id,
  nombre: usuarios.nombre,
  correo: usuarios.correo,
  rol: roles.clave,
  activo: usuarios.activo,
  miembroDesde: usuarios.miembroDesde,
};

function aUsuario(fila: FilaUsuario): Usuario {
  return {
    id: fila.id,
    nombre: fila.nombre,
    correo: fila.correo,
    // Un rol desconocido nunca debe heredar permisos: cae al de menor privilegio.
    rol: esRol(fila.rol) ? fila.rol : "autor",
    activo: fila.activo,
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

export async function articulosRelacionados(
  slugActual: string,
  cantidad = 2,
): Promise<Articulo[]> {
  const filas = await db
    .select()
    .from(articulos)
    .where(
      and(eq(articulos.estado, "publicado"), ne(articulos.slug, slugActual)),
    )
    .orderBy(desc(articulos.publicadoEn))
    .limit(cantidad);
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

// `cache` evita consultar dos veces por petición (generateMetadata + página).
export const articuloPublicadoPorSlug = cache(
  async (slug: string): Promise<Articulo | undefined> => {
    const [fila] = await db
      .select()
      .from(articulos)
      .where(eq(articulos.slug, slug));
    return fila && fila.estado === "publicado" ? aArticulo(fila) : undefined;
  },
);

export async function listarUsuarios(): Promise<Usuario[]> {
  const filas = await db
    .select(COLUMNAS_USUARIO)
    .from(usuarios)
    .innerJoin(roles, eq(usuarios.rolId, roles.id))
    .orderBy(usuarios.nombre);
  return filas.map(aUsuario);
}

export async function usuarioActivoPorId(
  id: string,
): Promise<Usuario | undefined> {
  const [fila] = await db
    .select(COLUMNAS_USUARIO)
    .from(usuarios)
    .innerJoin(roles, eq(usuarios.rolId, roles.id))
    .where(eq(usuarios.id, id));
  return fila ? aUsuario(fila) : undefined;
}

export async function credencialesPorCorreo(correo: string) {
  const [fila] = await db
    .select({ ...COLUMNAS_USUARIO, passwordHash: usuarios.passwordHash })
    .from(usuarios)
    .innerJoin(roles, eq(usuarios.rolId, roles.id))
    .where(eq(usuarios.correo, correo));
  return fila
    ? { usuario: aUsuario(fila), passwordHash: fila.passwordHash }
    : undefined;
}

export async function registrarAcceso(id: string) {
  await db
    .update(usuarios)
    .set({ ultimoAcceso: new Date() })
    .where(eq(usuarios.id, id));
}

export async function listarRoles() {
  const filas = await db.select().from(roles).orderBy(roles.nombre);
  return filas
    .filter((fila) => esRol(fila.clave))
    .map((fila) => ({
      clave: fila.clave as Rol,
      nombre: fila.nombre,
      descripcion: fila.descripcion,
    }));
}

async function rolIdPorClave(clave: Rol): Promise<string> {
  const [fila] = await db
    .select({ id: roles.id })
    .from(roles)
    .where(eq(roles.clave, clave));
  if (!fila) throw new Error(`El rol "${clave}" no existe en la BD`);
  return fila.id;
}

export async function insertarUsuario(datos: {
  nombre: string;
  correo: string;
  rol: Rol;
  passwordHash: string;
}) {
  await db.insert(usuarios).values({
    nombre: datos.nombre,
    correo: datos.correo,
    rolId: await rolIdPorClave(datos.rol),
    passwordHash: datos.passwordHash,
  });
}

export async function modificarUsuario(
  id: string,
  datos: {
    nombre: string;
    correo: string;
    rol: Rol;
    activo: boolean;
    passwordHash?: string;
  },
) {
  await db
    .update(usuarios)
    .set({
      nombre: datos.nombre,
      correo: datos.correo,
      rolId: await rolIdPorClave(datos.rol),
      activo: datos.activo,
      ...(datos.passwordHash ? { passwordHash: datos.passwordHash } : {}),
    })
    .where(eq(usuarios.id, id));
}

export async function borrarUsuario(id: string) {
  await db.delete(usuarios).where(eq(usuarios.id, id));
}

export async function correoEnUso(correo: string, exceptoId?: string) {
  const filtros = [eq(usuarios.correo, correo)];
  if (exceptoId) filtros.push(ne(usuarios.id, exceptoId));
  const [fila] = await db
    .select({ id: usuarios.id })
    .from(usuarios)
    .where(and(...filtros));
  return Boolean(fila);
}

export async function otrosAdministradoresActivos(exceptoId: string) {
  const [fila] = await db
    .select({ n: count() })
    .from(usuarios)
    .innerJoin(roles, eq(usuarios.rolId, roles.id))
    .where(
      and(
        eq(roles.clave, "administrador"),
        eq(usuarios.activo, true),
        ne(usuarios.id, exceptoId),
      ),
    );
  return fila?.n ?? 0;
}

export async function cantidadArticulosDeUsuario(id: string) {
  const [fila] = await db
    .select({ n: count() })
    .from(articulos)
    .where(eq(articulos.autorId, id));
  return fila?.n ?? 0;
}
