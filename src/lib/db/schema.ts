import {
  boolean,
  pgEnum,
  pgTable,
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
