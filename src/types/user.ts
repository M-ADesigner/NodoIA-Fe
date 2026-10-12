export type Rol = "administrador" | "revisor" | "autor";

export interface Usuario {
  id: string;
  nombre: string;
  correo: string;
  rol: Rol;
  activo: boolean;
  miembroDesde: string;
}
