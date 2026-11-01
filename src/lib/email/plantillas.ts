import type { Correo } from "./client";

const escapar = (texto: string) =>
  texto
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");

export function correoRestablecimiento(
  destino: { nombre: string; correo: string },
  enlace: string,
): Correo {
  const texto = [
    `Hola ${destino.nombre},`,
    "",
    "Recibimos una solicitud para restablecer tu contraseña del panel de la CCHIA.",
    "Usa este enlace (vale 1 hora y solo funciona una vez):",
    "",
    enlace,
    "",
    "Si no fuiste tú, ignora este correo: tu contraseña no cambiará.",
  ].join("\n");

  const html = `<div style="font-family:system-ui,sans-serif;font-size:15px;line-height:1.5;color:#14213d">
<p>Hola ${escapar(destino.nombre)},</p>
<p>Recibimos una solicitud para restablecer tu contraseña del panel de la CCHIA.</p>
<p><a href="${escapar(enlace)}" style="display:inline-block;padding:12px 24px;border-radius:999px;background:#0e2a4d;color:#ffffff;text-decoration:none;font-weight:600">Restablecer contraseña</a></p>
<p style="color:#475569">El enlace vale 1 hora y solo funciona una vez. Si no fuiste tú, ignora este correo: tu contraseña no cambiará.</p>
</div>`;

  return {
    para: destino.correo,
    asunto: "Restablece tu contraseña · CCHIA",
    texto,
    html,
  };
}
