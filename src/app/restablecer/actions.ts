"use server";

import { redirect } from "next/navigation";
import { hashPassword } from "@/lib/auth/password";
import { hashToken } from "@/lib/auth/reset";
import { aplicarRestablecimiento } from "@/lib/db/queries";

export type RestablecerState = { error?: string };

const PASSWORD_MIN = 8;
// Tope para que una contraseña enorme no sea un vector de consumo de CPU en scrypt.
const PASSWORD_MAX = 128;

export async function restablecerPassword(
  _previo: RestablecerState,
  formData: FormData,
): Promise<RestablecerState> {
  const token = String(formData.get("token") ?? "");
  const password = String(formData.get("password") ?? "");
  const confirmar = String(formData.get("confirmar") ?? "");

  if (!token || token.length > 200) {
    return { error: "El enlace no es válido. Solicita uno nuevo." };
  }
  if (password.length < PASSWORD_MIN || password.length > PASSWORD_MAX) {
    return {
      error: `La contraseña debe tener entre ${PASSWORD_MIN} y ${PASSWORD_MAX} caracteres.`,
    };
  }
  if (password !== confirmar) return { error: "Las contraseñas no coinciden." };

  let aplicado = false;
  try {
    aplicado = await aplicarRestablecimiento(
      hashToken(token),
      await hashPassword(password),
    );
  } catch {
    return { error: "No se pudo cambiar la contraseña. Inténtalo de nuevo." };
  }
  if (!aplicado) {
    return { error: "El enlace venció o ya se usó. Solicita uno nuevo." };
  }

  redirect("/login?restablecida=1");
}
