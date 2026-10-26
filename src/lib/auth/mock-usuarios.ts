import type { Rol } from "@/types/user";

// Datos de ejemplo que usa scripts/seed.ts para poblar la BD inicial.
export const usuariosSeed: {
  nombre: string;
  correo: string;
  rol: Rol;
  /** Id con el que los artículos de ejemplo referencian al autor. */
  ref: string;
}[] = [
  {
    ref: "u1",
    nombre: "María Fernández",
    correo: "maria.fernandez@cchia.cl",
    rol: "autor",
  },
  {
    ref: "u2",
    nombre: "Diego Rojas",
    correo: "diego.rojas@cchia.cl",
    rol: "autor",
  },
  {
    ref: "u3",
    nombre: "Javiera Soto",
    correo: "javiera.soto@cchia.cl",
    rol: "revisor",
  },
];
