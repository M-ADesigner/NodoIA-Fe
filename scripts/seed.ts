import { drizzle } from "drizzle-orm/postgres-js";
import postgres from "postgres";
import { articulosMock } from "../src/lib/content/mock-articulos";
import { usuariosMock } from "../src/lib/auth/mock-usuarios";
import { articulos, usuarios } from "../src/lib/db/schema";

const slugify = (texto: string) =>
  texto
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/(^-|-$)/g, "");

async function main() {
  const url = process.env.DATABASE_URL;
  if (!url) throw new Error("Falta DATABASE_URL");
  const sql = postgres(url, { max: 1 });
  const db = drizzle(sql);

  const idPorMock = new Map<string, string>();
  for (const u of usuariosMock) {
    const [fila] = await db
      .insert(usuarios)
      .values({ nombre: u.nombre, correo: u.correo, rol: u.rol })
      .onConflictDoUpdate({
        target: usuarios.correo,
        set: { nombre: u.nombre, rol: u.rol },
      })
      .returning({ id: usuarios.id });
    idPorMock.set(u.id, fila.id);
  }

  for (const a of articulosMock) {
    const autorId = idPorMock.get(a.autorId);
    if (!autorId) continue;
    await db
      .insert(articulos)
      .values({
        slug: slugify(a.titulo),
        titulo: a.titulo,
        categoria: a.categoria,
        resumen: a.resumen,
        contenido: a.contenido,
        estado: a.estado,
        autorId,
        publicadoEn: a.estado === "publicado" ? new Date() : null,
      })
      .onConflictDoNothing({ target: articulos.slug });
  }

  console.log(
    `Seed listo: ${usuariosMock.length} usuarios, ${articulosMock.length} artículos`,
  );
  await sql.end();
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
