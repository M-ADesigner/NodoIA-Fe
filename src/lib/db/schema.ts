import {
  boolean,
  index,
  integer,
  pgEnum,
  pgTable,
  primaryKey,
  text,
  timestamp,
  uuid,
} from "drizzle-orm/pg-core";

export const estadoEnum = pgEnum("estado_contenido", [
  "borrador",
  "asistido_ia",
  "en_revision",
  "aprobado",
  "publicado",
]);

export const categoriaEnum = pgEnum("categoria_contenido", [
  "Artículo",
  "Boletín",
  "Noticia institucional",
]);

export const roles = pgTable("roles", {
  id: uuid("id").primaryKey().defaultRandom(),
  clave: text("clave").notNull().unique(),
  nombre: text("nombre").notNull(),
  descripcion: text("descripcion").notNull().default(""),
});

export const usuarios = pgTable("usuarios", {
  id: uuid("id").primaryKey().defaultRandom(),
  nombre: text("nombre").notNull(),
  correo: text("correo").notNull().unique(),
  rolId: uuid("rol_id")
    .notNull()
    .references(() => roles.id),
  passwordHash: text("password_hash").notNull(),
  activo: boolean("activo").notNull().default(true),
  // Sube al cerrar sesión o cambiar la contraseña: invalida los tokens ya emitidos.
  sesionVersion: integer("sesion_version").notNull().default(0),
  ultimoAcceso: timestamp("ultimo_acceso", { withTimezone: true }),
  miembroDesde: timestamp("miembro_desde", { withTimezone: true })
    .notNull()
    .defaultNow(),
});

export const articulos = pgTable("articulos", {
  id: uuid("id").primaryKey().defaultRandom(),
  slug: text("slug").notNull().unique(),
  titulo: text("titulo").notNull(),
  categoria: categoriaEnum("categoria").notNull(),
  resumen: text("resumen").notNull(),
  contenido: text("contenido").notNull().default(""),
  estado: estadoEnum("estado").notNull().default("borrador"),
  autorId: uuid("autor_id")
    .notNull()
    .references(() => usuarios.id),
  creadoEn: timestamp("creado_en", { withTimezone: true })
    .notNull()
    .defaultNow(),
  actualizadoEn: timestamp("actualizado_en", { withTimezone: true })
    .notNull()
    .defaultNow(),
  publicadoEn: timestamp("publicado_en", { withTimezone: true }),
});

// Historial de cambios de estado de un artículo: quién lo movió, cuándo y por qué.
// Se borra junto con el artículo (el administrador puede eliminarlos).
export const revisiones = pgTable("revisiones", {
  id: uuid("id").primaryKey().defaultRandom(),
  articuloId: uuid("articulo_id")
    .notNull()
    .references(() => articulos.id, { onDelete: "cascade" }),
  usuarioId: uuid("usuario_id")
    .notNull()
    .references(() => usuarios.id),
  estadoAnterior: estadoEnum("estado_anterior").notNull(),
  estadoNuevo: estadoEnum("estado_nuevo").notNull(),
  comentario: text("comentario"),
  creadoEn: timestamp("creado_en", { withTimezone: true })
    .notNull()
    .defaultNow(),
});

// Un registro por intento de login fallido. El límite se calcula contando los
// de una clave (correo) dentro de la ventana; sobrevive a reinicios y despliegues.
export const intentosLogin = pgTable(
  "intentos_login",
  {
    id: uuid("id").primaryKey().defaultRandom(),
    clave: text("clave").notNull(),
    creadoEn: timestamp("creado_en", { withTimezone: true })
      .notNull()
      .defaultNow(),
  },
  (tabla) => [
    index("intentos_login_clave_creado_en_idx").on(tabla.clave, tabla.creadoEn),
  ],
);

// Enlaces de recuperación de contraseña. Solo se guarda el hash del token: si
// se filtra la tabla, los enlaces no sirven. Un solo uso y vencen pronto.
export const restablecimientos = pgTable("restablecimientos", {
  id: uuid("id").primaryKey().defaultRandom(),
  usuarioId: uuid("usuario_id")
    .notNull()
    .references(() => usuarios.id, { onDelete: "cascade" }),
  tokenHash: text("token_hash").notNull().unique(),
  expiraEn: timestamp("expira_en", { withTimezone: true }).notNull(),
  usadoEn: timestamp("usado_en", { withTimezone: true }),
  creadoEn: timestamp("creado_en", { withTimezone: true })
    .notNull()
    .defaultNow(),
});

// Catálogo de fuentes reutilizables entre artículos. La URL es única: si dos
// autores citan la misma, comparten el registro.
export const fuentes = pgTable("fuentes", {
  id: uuid("id").primaryKey().defaultRandom(),
  titulo: text("titulo").notNull(),
  url: text("url").notNull().unique(),
  tipo: text("tipo").notNull(),
  organismo: text("organismo"),
  creadoPor: uuid("creado_por").references(() => usuarios.id, {
    onDelete: "set null",
  }),
  creadoEn: timestamp("creado_en", { withTimezone: true })
    .notNull()
    .defaultNow(),
});

// Fuentes citadas por cada artículo, en el orden en que se muestran.
export const articuloFuentes = pgTable(
  "articulo_fuentes",
  {
    articuloId: uuid("articulo_id")
      .notNull()
      .references(() => articulos.id, { onDelete: "cascade" }),
    fuenteId: uuid("fuente_id")
      .notNull()
      .references(() => fuentes.id),
    orden: integer("orden").notNull().default(0),
  },
  (tabla) => [primaryKey({ columns: [tabla.articuloId, tabla.fuenteId] })],
);
