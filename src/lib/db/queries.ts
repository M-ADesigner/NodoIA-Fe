import "server-only";
import { cache } from "react";
import {
  and,
  count,
  desc,
  eq,
  gt,
  gte,
  inArray,
  isNull,
  lt,
  ne,
  sql,
} from "drizzle-orm";
import { esRol } from "@/lib/auth/roles";
import { esTipoFuente, type FuenteEntrada } from "@/lib/content/fuentes";
import type {
  Articulo,
  CategoriaContenido,
  EstadoContenido,
  Fuente,
  Revision,
} from "@/types/content";
import type { Rol, Usuario } from "@/types/user";
import { db } from "./client";
import {
  articuloFuentes,
  articulos,
  fuentes,
  intentosLogin,
  restablecimientos,
  revisiones,
  roles,
  usuarios,
} from "./schema";

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

export async function listarArticulos(autorId?: string): Promise<Articulo[]> {
  const filas = await db
    .select()
    .from(articulos)
    .where(autorId ? eq(articulos.autorId, autorId) : undefined)
    .orderBy(desc(articulos.actualizadoEn));
  return filas.map(aArticulo);
}

export async function articuloPorId(id: string): Promise<Articulo | undefined> {
  const [fila] = await db.select().from(articulos).where(eq(articulos.id, id));
  return fila ? aArticulo(fila) : undefined;
}

export async function slugEnUso(slug: string, exceptoId?: string) {
  const filtros = [eq(articulos.slug, slug)];
  if (exceptoId) filtros.push(ne(articulos.id, exceptoId));
  const [fila] = await db
    .select({ id: articulos.id })
    .from(articulos)
    .where(and(...filtros));
  return Boolean(fila);
}

type DatosArticulo = {
  titulo: string;
  categoria: CategoriaContenido;
  resumen: string;
  contenido: string;
};

export async function insertarArticulo(
  datos: DatosArticulo & { slug: string; autorId: string },
) {
  const [fila] = await db
    .insert(articulos)
    .values(datos)
    .returning({ id: articulos.id });
  return fila.id;
}

export async function modificarArticulo(id: string, datos: DatosArticulo) {
  await db
    .update(articulos)
    .set({ ...datos, actualizadoEn: new Date() })
    .where(eq(articulos.id, id));
}

// El estado de origen va en el WHERE: si otra petición ya movió el artículo,
// no se pisa y devuelve false. El cambio y su registro en `revisiones` van en
// una transacción: no puede quedar uno sin el otro.
export async function cambiarEstadoArticulo(
  id: string,
  desde: EstadoContenido,
  hacia: EstadoContenido,
  usuarioId: string,
  comentario?: string,
) {
  const ahora = new Date();
  return db.transaction(async (tx) => {
    const filas = await tx
      .update(articulos)
      .set({
        estado: hacia,
        actualizadoEn: ahora,
        ...(hacia === "publicado" ? { publicadoEn: ahora } : {}),
      })
      .where(and(eq(articulos.id, id), eq(articulos.estado, desde)))
      .returning({ id: articulos.id });
    if (filas.length === 0) return false;

    await tx.insert(revisiones).values({
      articuloId: id,
      usuarioId,
      estadoAnterior: desde,
      estadoNuevo: hacia,
      comentario: comentario || null,
      creadoEn: ahora,
    });
    return true;
  });
}

// Historial por artículo, del cambio más reciente al más antiguo.
export async function historialDeArticulos(
  ids: string[],
): Promise<Record<string, Revision[]>> {
  if (ids.length === 0) return {};
  const filas = await db
    .select({
      id: revisiones.id,
      articuloId: revisiones.articuloId,
      usuarioNombre: usuarios.nombre,
      estadoAnterior: revisiones.estadoAnterior,
      estadoNuevo: revisiones.estadoNuevo,
      comentario: revisiones.comentario,
      creadoEn: revisiones.creadoEn,
    })
    .from(revisiones)
    .innerJoin(usuarios, eq(revisiones.usuarioId, usuarios.id))
    .where(inArray(revisiones.articuloId, ids))
    .orderBy(desc(revisiones.creadoEn));

  const porArticulo: Record<string, Revision[]> = {};
  for (const fila of filas) {
    (porArticulo[fila.articuloId] ??= []).push({
      ...fila,
      comentario: fila.comentario ?? undefined,
      creadoEn: fechaRelativa(fila.creadoEn),
    });
  }
  return porArticulo;
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
    .where(inArray(articulos.estado, ["en_revision", "aprobado"]))
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

// Para validar la sesión: el token solo vale si su versión es la vigente.
export async function usuarioActivoConVersion(id: string) {
  const [fila] = await db
    .select({ ...COLUMNAS_USUARIO, sesionVersion: usuarios.sesionVersion })
    .from(usuarios)
    .innerJoin(roles, eq(usuarios.rolId, roles.id))
    .where(eq(usuarios.id, id));
  return fila
    ? { usuario: aUsuario(fila), sesionVersion: fila.sesionVersion }
    : undefined;
}

// Invalida todos los tokens emitidos para el usuario (en todos sus dispositivos).
export async function invalidarSesiones(id: string) {
  await db
    .update(usuarios)
    .set({ sesionVersion: sql`${usuarios.sesionVersion} + 1` })
    .where(eq(usuarios.id, id));
}

export async function credencialesPorCorreo(correo: string) {
  const [fila] = await db
    .select({
      ...COLUMNAS_USUARIO,
      passwordHash: usuarios.passwordHash,
      sesionVersion: usuarios.sesionVersion,
    })
    .from(usuarios)
    .innerJoin(roles, eq(usuarios.rolId, roles.id))
    .where(eq(usuarios.correo, correo));
  return fila
    ? {
        usuario: aUsuario(fila),
        passwordHash: fila.passwordHash,
        sesionVersion: fila.sesionVersion,
      }
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
      // Cambiar la contraseña cierra las sesiones abiertas.
      ...(datos.passwordHash
        ? {
            passwordHash: datos.passwordHash,
            sesionVersion: sql`${usuarios.sesionVersion} + 1`,
          }
        : {}),
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

export async function borrarArticulo(id: string) {
  await db.delete(articulos).where(eq(articulos.id, id));
}

export async function intentosFallidosDesde(clave: string, desde: Date) {
  const [fila] = await db
    .select({ n: count() })
    .from(intentosLogin)
    .where(
      and(eq(intentosLogin.clave, clave), gte(intentosLogin.creadoEn, desde)),
    );
  return fila?.n ?? 0;
}

// Aprovecha cada fallo para borrar lo que ya salió de la ventana: la tabla no crece sin límite.
export async function registrarIntentoFallido(clave: string, antesDe: Date) {
  await db.insert(intentosLogin).values({ clave });
  await db.delete(intentosLogin).where(lt(intentosLogin.creadoEn, antesDe));
}

export async function limpiarIntentosFallidos(clave: string) {
  await db.delete(intentosLogin).where(eq(intentosLogin.clave, clave));
}

export async function usuarioActivoPorCorreo(correo: string) {
  const [fila] = await db
    .select({
      id: usuarios.id,
      nombre: usuarios.nombre,
      correo: usuarios.correo,
    })
    .from(usuarios)
    .where(and(eq(usuarios.correo, correo), eq(usuarios.activo, true)));
  return fila;
}

// Un solo enlace vigente por usuario: pedir otro anula los anteriores sin usar.
export async function crearRestablecimiento(
  usuarioId: string,
  tokenHash: string,
  expiraEn: Date,
) {
  await db.transaction(async (tx) => {
    await tx
      .delete(restablecimientos)
      .where(
        and(
          eq(restablecimientos.usuarioId, usuarioId),
          isNull(restablecimientos.usadoEn),
        ),
      );
    await tx
      .insert(restablecimientos)
      .values({ usuarioId, tokenHash, expiraEn });
  });
}

export async function restablecimientoVigente(tokenHash: string) {
  const [fila] = await db
    .select({ id: restablecimientos.id })
    .from(restablecimientos)
    .where(
      and(
        eq(restablecimientos.tokenHash, tokenHash),
        isNull(restablecimientos.usadoEn),
        gt(restablecimientos.expiraEn, new Date()),
      ),
    );
  return Boolean(fila);
}

// Consume el enlace y cambia la contraseña en una transacción. Devuelve false
// si el enlace no existe, venció o ya se usó (el WHERE lo marca de forma
// atómica: dos peticiones a la vez no pueden usarlo ambas). Subir la versión de
// sesión cierra las sesiones abiertas; borrar los intentos fallidos destraba el login.
export async function aplicarRestablecimiento(
  tokenHash: string,
  passwordHash: string,
) {
  return db.transaction(async (tx) => {
    const [enlace] = await tx
      .update(restablecimientos)
      .set({ usadoEn: new Date() })
      .where(
        and(
          eq(restablecimientos.tokenHash, tokenHash),
          isNull(restablecimientos.usadoEn),
          gt(restablecimientos.expiraEn, new Date()),
        ),
      )
      .returning({ usuarioId: restablecimientos.usuarioId });
    if (!enlace) return false;

    const [usuario] = await tx
      .update(usuarios)
      .set({
        passwordHash,
        sesionVersion: sql`${usuarios.sesionVersion} + 1`,
      })
      .where(eq(usuarios.id, enlace.usuarioId))
      .returning({ correo: usuarios.correo });

    if (usuario) {
      await tx
        .delete(intentosLogin)
        .where(eq(intentosLogin.clave, usuario.correo));
    }
    return true;
  });
}

function aFuente(fila: typeof fuentes.$inferSelect): Fuente {
  return {
    id: fila.id,
    titulo: fila.titulo,
    url: fila.url,
    // El tipo solo se escribe validado; un valor ajeno cae en "Otro".
    tipo: esTipoFuente(fila.tipo) ? fila.tipo : "Otro",
    organismo: fila.organismo ?? undefined,
  };
}

// Catálogo completo, para reutilizar fuentes al redactar.
export async function listarFuentes(): Promise<Fuente[]> {
  const filas = await db.select().from(fuentes).orderBy(fuentes.titulo);
  return filas.map(aFuente);
}

// Fuentes por artículo, en el orden en que el autor las dejó.
export async function fuentesDeArticulos(
  ids: string[],
): Promise<Record<string, Fuente[]>> {
  if (ids.length === 0) return {};
  const filas = await db
    .select({ articuloId: articuloFuentes.articuloId, fuente: fuentes })
    .from(articuloFuentes)
    .innerJoin(fuentes, eq(articuloFuentes.fuenteId, fuentes.id))
    .where(inArray(articuloFuentes.articuloId, ids))
    .orderBy(articuloFuentes.orden);

  const porArticulo: Record<string, Fuente[]> = {};
  for (const fila of filas) {
    (porArticulo[fila.articuloId] ??= []).push(aFuente(fila.fuente));
  }
  return porArticulo;
}

export const fuentesDeArticulo = cache(
  async (id: string): Promise<Fuente[]> =>
    (await fuentesDeArticulos([id]))[id] ?? [],
);

// Reemplaza las fuentes del artículo. Las nuevas se crean en el catálogo (o se
// reutiliza la que ya tenga esa URL); las del catálogo deben existir. Todo en
// una transacción: devuelve false, sin tocar nada, si alguna ya no existe.
export async function reemplazarFuentesDeArticulo(
  articuloId: string,
  entradas: FuenteEntrada[],
  usuarioId: string,
) {
  return db.transaction(async (tx) => {
    const ids: string[] = [];
    for (const entrada of entradas) {
      let id: string | undefined;
      if ("id" in entrada) {
        const [fila] = await tx
          .select({ id: fuentes.id })
          .from(fuentes)
          .where(eq(fuentes.id, entrada.id));
        id = fila?.id;
      } else {
        const [creada] = await tx
          .insert(fuentes)
          .values({
            titulo: entrada.titulo,
            url: entrada.url,
            tipo: entrada.tipo,
            organismo: entrada.organismo ?? null,
            creadoPor: usuarioId,
          })
          .onConflictDoNothing({ target: fuentes.url })
          .returning({ id: fuentes.id });
        id =
          creada?.id ??
          (
            await tx
              .select({ id: fuentes.id })
              .from(fuentes)
              .where(eq(fuentes.url, entrada.url))
          )[0]?.id;
      }
      if (!id) return false;
      if (!ids.includes(id)) ids.push(id);
    }

    await tx
      .delete(articuloFuentes)
      .where(eq(articuloFuentes.articuloId, articuloId));
    if (ids.length > 0) {
      await tx
        .insert(articuloFuentes)
        .values(
          ids.map((fuenteId, orden) => ({ articuloId, fuenteId, orden })),
        );
    }
    return true;
  });
}
