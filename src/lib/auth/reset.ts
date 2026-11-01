import { createHash, randomBytes } from "node:crypto";

export const RESET_VIGENCIA_MS = 60 * 60 * 1000; // 1 hora

// El token (256 bits) viaja solo en el enlace; en la BD queda su SHA-256.
export function generarTokenRestablecimiento() {
  const token = randomBytes(32).toString("base64url");
  return { token, hash: hashToken(token) };
}

export function hashToken(token: string) {
  return createHash("sha256").update(token).digest("hex");
}
