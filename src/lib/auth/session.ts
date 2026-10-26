import "server-only";
import { cache } from "react";
import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import {
  firmarToken,
  SESSION_COOKIE,
  SESSION_MAX_AGE,
  verificarToken,
} from "@/lib/auth/jwt";
import { tienePermiso, vistasPermitidas, type Permiso } from "@/lib/auth/roles";
import { usuarioActivoPorId } from "@/lib/db/queries";
import type { Rol, Usuario } from "@/types/user";

export async function crearSesion(usuario: {
  id: string;
  rol: Rol;
  nombre: string;
}) {
  const token = await firmarToken({
    sub: usuario.id,
    rol: usuario.rol,
    nombre: usuario.nombre,
  });
  const store = await cookies();
  store.set(SESSION_COOKIE, token, {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax",
    path: "/",
    maxAge: SESSION_MAX_AGE,
  });
}

export async function cerrarSesionCookie() {
  const store = await cookies();
  store.delete(SESSION_COOKIE);
}

// Usuario autenticado y vigente. El JWT es stateless, así que se vuelve a
// consultar la BD para detectar usuarios desactivados o con rol cambiado.
export const usuarioActual = cache(async (): Promise<Usuario | null> => {
  const store = await cookies();
  const claims = await verificarToken(store.get(SESSION_COOKIE)?.value);
  if (!claims) return null;
  const usuario = await usuarioActivoPorId(claims.sub);
  return usuario && usuario.activo ? usuario : null;
});

// Guard para páginas y Server Actions del panel. Redirige si no hay sesión o
// si el rol no tiene el permiso pedido.
export async function requerirPermiso(permiso: Permiso): Promise<Usuario> {
  const usuario = await usuarioActual();
  if (!usuario) redirect("/login");
  if (!tienePermiso(usuario.rol, permiso)) {
    const destino = vistasPermitidas(usuario.rol)[0]?.href;
    if (destino) redirect(destino);
    redirect("/login?error=sin-permiso");
  }
  return usuario;
}
