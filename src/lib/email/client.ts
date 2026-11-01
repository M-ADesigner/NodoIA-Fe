import "server-only";

export interface Correo {
  para: string;
  asunto: string;
  texto: string;
  html: string;
}

// Envío por la API HTTPS de Resend (Railway bloquea el SMTP saliente en los
// planes Free, Trial y Hobby). Sin RESEND_API_KEY: en desarrollo el correo se
// imprime en la consola del servidor; en producción falla, para no aparentar
// un envío que no ocurrió.
export async function enviarCorreo(correo: Correo): Promise<void> {
  const apiKey = process.env.RESEND_API_KEY;

  if (!apiKey) {
    if (process.env.NODE_ENV === "production") {
      throw new Error("Falta RESEND_API_KEY");
    }
    console.log(
      `\n[correo no enviado: falta RESEND_API_KEY]\nPara: ${correo.para}\nAsunto: ${correo.asunto}\n\n${correo.texto}\n`,
    );
    return;
  }

  const from = process.env.EMAIL_FROM;
  if (!from) {
    throw new Error("Falta EMAIL_FROM (remitente verificado en Resend)");
  }

  // https://resend.com/docs/api-reference/emails/send-email
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
