import { jwtVerify, SignJWT } from "jose";
import { esRol } from "@/lib/auth/roles";
import type { Rol } from "@/types/user";

export const SESSION_COOKIE = "nodoia_session";
export const SESSION_MAX_AGE = 60 * 60 * 8; // 8 horas

export interface SesionClaims {
  sub: string;
  rol: Rol;
  nombre: string;
}

function getSecret(): Uint8Array {
  const secret = process.env.AUTH_SECRET;
  if (!secret || secret.length < 32) {
    throw new Error("Falta AUTH_SECRET (mínimo 32 caracteres)");
  }
  return new TextEncoder().encode(secret);
}

export async function firmarToken(claims: SesionClaims): Promise<string> {
  return new SignJWT({ rol: claims.rol, nombre: claims.nombre })
    .setProtectedHeader({ alg: "HS256" })
    .setSubject(claims.sub)
    .setIssuedAt()
    .setExpirationTime(`${SESSION_MAX_AGE}s`)
    .sign(getSecret());
}

export async function verificarToken(
  token: string | undefined,
): Promise<SesionClaims | null> {
  if (!token) return null;
  try {
    const { payload } = await jwtVerify(token, getSecret(), {
      algorithms: ["HS256"],
    });
    if (!payload.sub || !esRol(payload.rol)) return null;
    return {
      sub: payload.sub,
      rol: payload.rol,
      nombre: typeof payload.nombre === "string" ? payload.nombre : "",
    };
  } catch {
    return null;
  }
}
