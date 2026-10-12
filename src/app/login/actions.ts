"use server";

import { redirect } from "next/navigation";
import { verifyPassword } from "@/lib/auth/password";
import { vistasPermitidas } from "@/lib/auth/roles";
import { cerrarSesionCookie, crearSesion } from "@/lib/auth/session";
import { credencialesPorCorreo, registrarAcceso } from "@/lib/db/queries";

export type LoginState = { error?: string };

const ERROR_GENERICO = "Correo o contraseña incorrectos.";

// Hash de relleno para que el tiempo de respuesta no delate si el correo existe.
const HASH_RELLENO =
  "scrypt$00000000000000000000000000000000$" + "0".repeat(128);

// Límite básico por correo (en memoria: se reinicia con cada despliegue y no
// se comparte entre instancias; es una defensa mínima, no un WAF).
const INTENTOS_MAX = 5;
const VENTANA_MS = 10 * 60 * 1000;
const intentos = new Map<string, { n: number; hasta: number }>();

function bloqueado(clave: string) {
  const registro = intentos.get(clave);
  if (!registro) return false;
  if (registro.hasta < Date.now()) {
    intentos.delete(clave);
    return false;
  }
  return registro.n >= INTENTOS_MAX;
}

function registrarFallo(clave: string) {
  const ahora = Date.now();
  const registro = intentos.get(clave);
  if (!registro || registro.hasta < ahora) {
    intentos.set(clave, { n: 1, hasta: ahora + VENTANA_MS });
  } else {
    registro.n += 1;
  }
}

function destinoSeguro(next: FormDataEntryValue | null): string | null {
  if (typeof next !== "string") return null;
  // Solo rutas internas del panel: evita redirecciones abiertas.
  if (!next.startsWith("/dashboard") || next.startsWith("//")) return null;
  return next;
}

export async function iniciarSesion(
  _previo: LoginState,
  formData: FormData,
): Promise<LoginState> {
  const correo = String(formData.get("correo") ?? "")
    .trim()
    .toLowerCase();
  const password = String(formData.get("password") ?? "");

  if (!correo || !password) {
    return { error: "Ingresa tu correo y contraseña." };
  }
  if (bloqueado(correo)) {
    return { error: "Demasiados intentos. Espera unos minutos." };
  }

  const credenciales = await credencialesPorCorreo(correo);
  const valido = await verifyPassword(
    password,
    credenciales?.passwordHash ?? HASH_RELLENO,
  );

  if (!credenciales || !valido) {
    registrarFallo(correo);
    return { error: ERROR_GENERICO };
  }

  // Solo se revela que la cuenta está deshabilitada si la contraseña es
  // correcta: así nadie puede averiguar qué correos existen o están inactivos.
  if (!credenciales.usuario.activo) {
    return {
      error:
        "Tu cuenta ha sido deshabilitada. Contacta a administración para volver a acceder.",
    };
  }

  intentos.delete(correo);
  await crearSesion(credenciales.usuario);
  await registrarAcceso(credenciales.usuario.id);

  const permitidas = vistasPermitidas(credenciales.usuario.rol);
  const destino = destinoSeguro(formData.get("next"));
  const puedeVerDestino =
    destino && permitidas.some((vista) => destino.startsWith(vista.href));
  redirect(puedeVerDestino ? destino : (permitidas[0]?.href ?? "/"));
}

export async function cerrarSesion() {
  await cerrarSesionCookie();
  redirect("/login");
}
