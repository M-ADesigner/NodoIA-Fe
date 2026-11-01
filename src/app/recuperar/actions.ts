"use server";

import { after } from "next/server";
import {
  generarTokenRestablecimiento,
  RESET_VIGENCIA_MS,
} from "@/lib/auth/reset";
import {
  crearRestablecimiento,
  intentosFallidosDesde,
  registrarIntentoFallido,
  usuarioActivoPorCorreo,
} from "@/lib/db/queries";
import { enviarCorreo } from "@/lib/email/client";
import { correoRestablecimiento } from "@/lib/email/plantillas";
import { siteUrl } from "@/lib/seo/site";

export type RecuperarState = { ok?: boolean; error?: string };

const CORREO_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

// Límite por correo (misma tabla que el de login, con clave propia): evita
// que alguien llene de correos la bandeja de otra persona.
const SOLICITUDES_MAX = 3;
const VENTANA_MS = 10 * 60 * 1000;

export async function solicitarRestablecimiento(
  _previo: RecuperarState,
  formData: FormData,
): Promise<RecuperarState> {
  const correo = String(formData.get("correo") ?? "")
    .trim()
    .toLowerCase();
  if (!CORREO_RE.test(correo)) return { error: "Ingresa un correo válido." };

  const clave = `recuperar:${correo}`;
  const desde = new Date(Date.now() - VENTANA_MS);

  if ((await intentosFallidosDesde(clave, desde)) < SOLICITUDES_MAX) {
    await registrarIntentoFallido(clave, desde);

    // La búsqueda y el envío corren tras la respuesta: así el tiempo de
    // respuesta no delata si el correo existe. El enlace se arma con SITE_URL,
    // nunca con el Host de la petición (evita enlaces falsos).
    after(async () => {
      try {
        const usuario = await usuarioActivoPorCorreo(correo);
        if (!usuario) return;
        const { token, hash } = generarTokenRestablecimiento();
        await crearRestablecimiento(
          usuario.id,
          hash,
          new Date(Date.now() + RESET_VIGENCIA_MS),
        );
        const enlace = `${siteUrl()}/restablecer?token=${token}`;
        await enviarCorreo(correoRestablecimiento(usuario, enlace));
      } catch (error) {
        console.error(
          "No se pudo procesar la recuperación de contraseña",
          error,
        );
      }
    });
  }

  // Misma respuesta exista o no la cuenta, o esté limitada: no revela nada.
  return { ok: true };
}
