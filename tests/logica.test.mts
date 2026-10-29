// Pruebas de lógica del informe (PT-08 a PT-10). Ejecutar con: npx tsx tests/logica.test.mts
import { hashPassword, verifyPassword } from "../src/lib/auth/password.ts";
import { puedeTransicionar } from "../src/lib/content/workflow.ts";
import { tienePermiso, vistasPermitidas } from "../src/lib/auth/roles.ts";

const resultados: [string, boolean][] = [];
const registrar = (nombre: string, ok: boolean) =>
  resultados.push([nombre, ok]);

const hash1 = await hashPassword("Clave-Segura-1");
const hash2 = await hashPassword("Clave-Segura-1");
registrar(
  "PT-08 hash scrypt con prefijo y sal distinta por usuario",
  hash1.startsWith("scrypt$") && hash1 !== hash2,
);
registrar(
  "PT-08 acepta la clave correcta",
  await verifyPassword("Clave-Segura-1", hash1),
);
registrar(
  "PT-08 rechaza la clave incorrecta",
  !(await verifyPassword("otra", hash1)),
);

registrar(
  "PT-09 borrador -> en_revision permitido",
  puedeTransicionar("borrador", "en_revision"),
);
registrar(
  "PT-09 borrador -> publicado bloqueado",
  !puedeTransicionar("borrador", "publicado"),
);
registrar(
  "PT-09 publicado no admite transiciones",
  !puedeTransicionar("publicado", "borrador"),
);

registrar(
  "PT-10 revisor no gestiona usuarios",
  !tienePermiso("revisor", "gestionar_usuarios"),
);
registrar(
  "PT-10 autor ve solo Contenido",
  vistasPermitidas("autor")
    .map((v) => v.label)
    .join() === "Contenido",
);
registrar(
  "PT-10 administrador ve 3 vistas",
  vistasPermitidas("administrador").length === 3,
);

for (const [nombre, ok] of resultados) {
  console.log(ok ? "OK  " : "FAIL", nombre);
}
if (resultados.some(([, ok]) => !ok)) process.exit(1);
