import "server-only";
import { drizzle } from "drizzle-orm/postgres-js";
import postgres from "postgres";
import * as schema from "./schema";

type Db = ReturnType<typeof drizzle<typeof schema>>;

// En dev, Next recarga módulos: reutilizar la conexión evita agotar el pool.
const globalForDb = globalThis as unknown as { db?: Db };

// La conexión se crea en la primera consulta, no al importar el módulo, para
// que `next build` no exija DATABASE_URL (las páginas son dinámicas).
function getDb(): Db {
  if (globalForDb.db) return globalForDb.db;

  const connectionString = process.env.DATABASE_URL;
  if (!connectionString) {
    throw new Error("Falta la variable de entorno DATABASE_URL");
  }

  const db = drizzle(postgres(connectionString, { max: 10 }), { schema });
  globalForDb.db = db;
  return db;
}

export const db = new Proxy({} as Db, {
  get(_target, prop) {
    const real = getDb();
    const value = Reflect.get(real, prop, real);
    return typeof value === "function" ? value.bind(real) : value;
  },
});
