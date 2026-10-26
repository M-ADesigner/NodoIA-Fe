import { randomBytes } from "node:crypto";
import { eq } from "drizzle-orm";
import { drizzle } from "drizzle-orm/postgres-js";
import postgres from "postgres";
import { articulosMock } from "../src/lib/content/mock-articulos";
import { usuariosSeed } from "../src/lib/auth/mock-usuarios";
import { hashPassword } from "../src/lib/auth/password";
import { articulos, roles, usuarios } from "../src/lib/db/schema";

const ROLES = [
  {
    clave: "administrador",
    nombre: "Administrador",
    descripcion: "Gestiona usuarios, contenido y publicación.",
  },
  {
    clave: "revisor",
    nombre: "Revisor",
    descripcion: "Revisa y aprueba el contenido en cola.",
  },
  {
    clave: "autor",
    nombre: "Autor",
    descripcion: "Genera y edita contenido con asistencia de IA.",
  },
];

const slugify = (texto: string) =>
  texto
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/(^-|-$)/g, "");

const passwordAleatoria = () => randomBytes(9).toString("base64url");

async function main() {
  const url = process.env.DATABASE_URL;
  if (!url) throw new Error("Falta DATABASE_URL");
  const sql = postgres(url, { max: 1 });
  const db = drizzle(sql);

  // Roles
  const rolId = new Map<string, string>();
  for (const rol of ROLES) {
    const [fila] = await db
      .insert(roles)
      .values(rol)
      .onConflictDoUpdate({
        target: roles.clave,
        set: { nombre: rol.nombre, descripcion: rol.descripcion },
      })
      .returning({ id: roles.id });
    rolId.set(rol.clave, fila.id);
  }

  // Usuarios de ejemplo (no pisa a los que ya existen)
  const idPorRef = new Map<string, string>();
  const credenciales: { correo: string; password: string }[] = [];
  for (const u of usuariosSeed) {
    const password = passwordAleatoria();
    const [fila] = await db
      .insert(usuarios)
      .values({
        nombre: u.nombre,
        correo: u.correo,
        rolId: rolId.get(u.rol)!,
        passwordHash: await hashPassword(password),
      })
      .onConflictDoNothing({ target: usuarios.correo })
      .returning({ id: usuarios.id });
    if (fila) {
      credenciales.push({ correo: u.correo, password });
      idPorRef.set(u.ref, fila.id);
    } else {
      const [existente] = await db
        .select({ id: usuarios.id })
        .from(usuarios)
        .where(eq(usuarios.correo, u.correo));
      idPorRef.set(u.ref, existente.id);
    }
  }

  // Usuarios sin contraseña válida (migración desde la versión sin login)
  const sinPassword = await db
    .select({ id: usuarios.id, correo: usuarios.correo })
    .from(usuarios)
    .where(eq(usuarios.passwordHash, "!"));
  for (const u of sinPassword) {
    const password = passwordAleatoria();
    await db
      .update(usuarios)
      .set({ passwordHash: await hashPassword(password) })
      .where(eq(usuarios.id, u.id));
    credenciales.push({ correo: u.correo, password });
  }

  // Garantiza al menos un administrador
  const admins = await db
    .select({ id: usuarios.id })
    .from(usuarios)
    .where(eq(usuarios.rolId, rolId.get("administrador")!));
  if (admins.length === 0) {
    const correo = process.env.SEED_ADMIN_EMAIL ?? "admin@cchia.cl";
    const password = process.env.SEED_ADMIN_PASSWORD ?? passwordAleatoria();
    await db.insert(usuarios).values({
      nombre: "Administrador",
      correo,
      rolId: rolId.get("administrador")!,
      passwordHash: await hashPassword(password),
    });
    credenciales.push({ correo, password });
  }

  // Artículos de ejemplo
  for (const a of articulosMock) {
    const autorId = idPorRef.get(a.autorId);
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

  console.log("Seed listo.");
  if (credenciales.length > 0) {
    console.log(
      "\nContraseñas iniciales (se muestran una sola vez, guárdalas):",
    );
    for (const c of credenciales) console.log(`  ${c.correo}  ${c.password}`);
  }
  await sql.end();
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
