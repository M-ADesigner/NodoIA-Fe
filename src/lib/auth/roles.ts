import type { Rol } from "@/types/user";

export type Permiso = "gestionar_usuarios" | "gestionar_contenido" | "revisar";

export const ROLES: Rol[] = ["administrador", "revisor", "autor"];

export const ROL_LABEL: Record<Rol, string> = {
  administrador: "Administrador",
  revisor: "Revisor",
  autor: "Autor",
};

const PERMISOS_POR_ROL: Record<Rol, Permiso[]> = {
  administrador: ["gestionar_usuarios", "gestionar_contenido", "revisar"],
  revisor: ["revisar"],
  autor: ["gestionar_contenido"],
};

export function esRol(valor: unknown): valor is Rol {
  return typeof valor === "string" && (ROLES as string[]).includes(valor);
}

export function tienePermiso(rol: Rol, permiso: Permiso): boolean {
  return PERMISOS_POR_ROL[rol].includes(permiso);
}

// Vistas del panel interno y el permiso que exige cada una.
export const VISTAS_DASHBOARD: {
  href: string;
  label: string;
  permiso: Permiso;
}[] = [
  {
    href: "/dashboard/contenido",
    label: "Contenido",
    permiso: "gestionar_contenido",
  },
  { href: "/dashboard/revision", label: "Revisión", permiso: "revisar" },
  {
    href: "/dashboard/admin",
    label: "Administración",
    permiso: "gestionar_usuarios",
  },
];

export function vistasPermitidas(rol: Rol) {
  return VISTAS_DASHBOARD.filter((vista) => tienePermiso(rol, vista.permiso));
}
