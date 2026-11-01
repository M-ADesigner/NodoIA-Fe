// Diagnóstico del correo de recuperación (no se versiona ni se despliega).
// Ejecutar: npx tsx --conditions=react-server --env-file=.env.local scripts/diagnostico-correo.ts
// (--conditions=react-server: queries.ts importa "server-only").
// Solo imprime sí/no, códigos de estado y mensajes de error; nunca valores de
// variables de entorno.
import {
  credencialesPorCorreo,
  usuarioActivoPorCorreo,
} from "../src/lib/db/queries";

const CORREOS = ["andresavilamf@gmail.com", "andresavilamf+revisor@gmail.com"];
const REMITENTE = "NodoIA <onboarding@resend.dev>";

async function estadoUsuario(correo: string) {
  const cuenta = await credencialesPorCorreo(correo);
  const activo = await usuarioActivoPorCorreo(correo);
  return { existe: Boolean(cuenta), activo: Boolean(activo) };
}

async function enviarPrueba(apiKey: string, para: string) {
  const respuesta = await fetch("https://api.resend.com/emails", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${apiKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      from: REMITENTE,
      to: [para],
      subject: "Diagnóstico de correo NodoIA",
      text: "Si ves esto, Resend entregó el correo de prueba.",
    }),
    signal: AbortSignal.timeout(15_000),
  });
  const cuerpo = await respuesta.text();
  return { status: respuesta.status, cuerpo };
}

async function main() {
  const apiKey = process.env.RESEND_API_KEY;
  console.log(`RESEND_API_KEY definida: ${apiKey ? "sí" : "no"}`);
  console.log(`DATABASE_URL definida: ${process.env.DATABASE_URL ? "sí" : "no"}`);

  console.log("\n== Base de datos ==");
  for (const correo of CORREOS) {
    const { existe, activo } = await estadoUsuario(correo);
    console.log(
      `${correo}: existe: ${existe ? "sí" : "no"} / activo: ${activo ? "sí" : "no"}`,
    );
  }

  if (!apiKey) {
    console.log("\nSin RESEND_API_KEY no se puede probar el envío.");
    return;
  }

  console.log("\n== Resend ==");
  for (const correo of [...CORREOS].reverse()) {
    const { status, cuerpo } = await enviarPrueba(apiKey, correo);
    console.log(`${correo}: HTTP ${status} ${cuerpo}`);
  }
}

main()
  .catch((error) => {
    console.error("Error:", error instanceof Error ? error.message : error);
    process.exitCode = 1;
  })
  .finally(() => process.exit());
