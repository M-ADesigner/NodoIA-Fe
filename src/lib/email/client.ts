import "server-only";
import nodemailer from "nodemailer";

export interface Correo {
  para: string;
  asunto: string;
  texto: string;
  html: string;
}

// Dos formas de envío:
// 1. Resend (API HTTPS): se usa si existe RESEND_API_KEY. Es la opción para
//    Railway, que bloquea el SMTP saliente en los planes Free, Trial y Hobby.
// 2. SMTP con nodemailer (p. ej. Gmail con contraseña de aplicación): se usa
//    si no hay RESEND_API_KEY pero sí SMTP_USER y SMTP_PASSWORD (útil en local).
// Sin ninguna de las dos: en desarrollo el correo se imprime en la consola del
// servidor; en producción falla, para no aparentar un envío que no ocurrió.
export async function enviarCorreo(correo: Correo): Promise<void> {
  const resendKey = process.env.RESEND_API_KEY;
  if (resendKey) {
    await enviarConResend(correo, resendKey);
    return;
  }

  const user = process.env.SMTP_USER;
  const pass = process.env.SMTP_PASSWORD;
  const from = process.env.EMAIL_FROM || user;

  if (!user || !pass || !from) {
    if (process.env.NODE_ENV === "production") {
      throw new Error(
        "Falta RESEND_API_KEY, o bien SMTP_USER, SMTP_PASSWORD o EMAIL_FROM",
      );
    }
    console.log(
      `\n[correo no enviado: falta RESEND_API_KEY o SMTP_USER/SMTP_PASSWORD]\nPara: ${correo.para}\nAsunto: ${correo.asunto}\n\n${correo.texto}\n`,
    );
    return;
  }

  const puerto = Number(process.env.SMTP_PORT) || 465;
  const transporte = nodemailer.createTransport({
    host: process.env.SMTP_HOST || "smtp.gmail.com",
    port: puerto,
    secure: puerto === 465,
    auth: { user, pass },
  });

  await transporte.sendMail({
    from,
    to: correo.para,
    subject: correo.asunto,
    text: correo.texto,
    html: correo.html,
  });
}

// https://resend.com/docs/api-reference/emails/send-email
async function enviarConResend(correo: Correo, apiKey: string): Promise<void> {
  const from = process.env.EMAIL_FROM;
  if (!from) {
    throw new Error("Falta EMAIL_FROM (remitente verificado en Resend)");
  }

  const respuesta = await fetch("https://api.resend.com/emails", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${apiKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      from,
      to: [correo.para],
      subject: correo.asunto,
      text: correo.texto,
      html: correo.html,
    }),
    signal: AbortSignal.timeout(15_000),
  });

  if (!respuesta.ok) {
    // El cuerpo trae el motivo (dominio no verificado, clave inválida...),
    // nunca la clave.
    const detalle = await respuesta.text().catch(() => "");
    throw new Error(`Resend respondió ${respuesta.status}: ${detalle}`);
  }
}
