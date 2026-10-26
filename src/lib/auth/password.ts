import { randomBytes, scrypt, timingSafeEqual } from "node:crypto";

// Formato: scrypt$<salt hex>$<hash hex>. Sin dependencias nativas.
const KEY_LENGTH = 64;

function derivar(password: string, salt: Buffer): Promise<Buffer> {
  return new Promise((resolve, reject) => {
    scrypt(password, salt, KEY_LENGTH, (error, key) =>
      error ? reject(error) : resolve(key),
    );
  });
}

export async function hashPassword(password: string): Promise<string> {
  const salt = randomBytes(16);
  const key = await derivar(password, salt);
  return `scrypt$${salt.toString("hex")}$${key.toString("hex")}`;
}

export async function verifyPassword(
  password: string,
  almacenado: string,
): Promise<boolean> {
  const [esquema, saltHex, keyHex] = almacenado.split("$");
  if (esquema !== "scrypt" || !saltHex || !keyHex) return false;
  const esperado = Buffer.from(keyHex, "hex");
  const obtenido = await derivar(password, Buffer.from(saltHex, "hex"));
  return (
    esperado.length === obtenido.length && timingSafeEqual(esperado, obtenido)
  );
}
