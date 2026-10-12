import "server-only";
import { drizzle } from "drizzle-orm/postgres-js";
import postgres from "postgres";
import * as schema from "./schema";

const connectionString = process.env.DATABASE_URL;
if (!connectionString) {
  throw new Error("Falta la variable de entorno DATABASE_URL");
}

// En dev, Next recarga módulos: reutilizar la conexión evita agotar el pool.
const globalForDb = globalThis as unknown as {
  sql?: ReturnType<typeof postgres>;
};
const sql = globalForDb.sql ?? postgres(connectionString, { max: 10 });
if (process.env.NODE_ENV !== "production") globalForDb.sql = sql;

export const db = drizzle(sql, { schema });
