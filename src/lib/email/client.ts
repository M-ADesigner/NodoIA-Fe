import "server-only";
import nodemailer from "nodemailer";

export interface Correo {
  para: string;
  asunto: string;
  texto: string;
  html: string;
}

// Envío por SMTP con nodemailer (p. ej. Gmail con contraseña de aplicación).
// Sin SMTP_USER, SMTP_PASSWORD o EMAIL_FROM: en desarrollo el correo se
// imprime en la consola del servidor; en producción falla, para no aparentar
// un envío que no ocurrió.
export async function enviarCorreo(correo: Correo): Promise<void> {
  const user = process.env.SMTP_USER;
  const pass = process.env.SMTP_PASSWORD;
  const from = process.env.EMAIL_FROM || user;

  if (!user || !pass || !from) {
    if (process.env.NODE_ENV === "production") {
      throw new Error("Falta SMTP_USER, SMTP_PASSWORD o EMAIL_FROM");
    }
    console.log(
      `\n[correo no enviado: falta SMTP_USER o SMTP_PASSWORD]\nPara: ${correo.para}\nAsunto: ${correo.asunto}\n\n${correo.texto}\n`,
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
