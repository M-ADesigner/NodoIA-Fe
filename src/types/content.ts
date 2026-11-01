export type EstadoContenido =
  "borrador" | "asistido_ia" | "en_revision" | "aprobado" | "publicado";

export type CategoriaContenido =
  "Artículo" | "Boletín" | "Noticia institucional";

export interface Articulo {
  id: string;
  slug: string;
  titulo: string;
  categoria: CategoriaContenido;
  resumen: string;
  contenido: string;
  estado: EstadoContenido;
  autorId: string;
  actualizadoEn: string;
  publicadoEn?: string;
  /** Fechas ISO 8601 para <time>, JSON-LD y sitemap. */
  actualizadoEnIso?: string;
  publicadoEnIso?: string;
}

export interface Revision {
  id: string;
  articuloId: string;
  usuarioNombre: string;
  estadoAnterior: EstadoContenido;
  estadoNuevo: EstadoContenido;
  comentario?: string;
  creadoEn: string;
}

export type TipoFuente =
  "Informe" | "Noticia" | "Estudio" | "Sitio oficial" | "Otro";

export interface Fuente {
  id: string;
  titulo: string;
  url: string;
  tipo: TipoFuente;
  organismo?: string;
}
//sssss