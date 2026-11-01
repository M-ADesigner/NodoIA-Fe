import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { SESSION_COOKIE, verificarToken } from "@/lib/auth/jwt";

const esDev = process.env.NODE_ENV === "development";

// Por defecto la CSP solo se reporta (consola del navegador) para detectar
// bloqueos antes de aplicarla. Con CSP_ENFORCE=true se hace cumplir.
const CSP_HEADER =
  process.env.CSP_ENFORCE === "true"
    ? "Content-Security-Policy"
    : "Content-Security-Policy-Report-Only";

// Nonce por petición (requiere render dinámico; ver docs de Next sobre CSP).
// `style-src-attr` permite los atributos style="…" de React; los <style> y
// <script> solo corren con nonce. Next solo permite eval en desarrollo.
function construirCsp(nonce: string) {
  return [
    "default-src 'self'",
    `script-src 'self' 'nonce-${nonce}' 'strict-dynamic'${esDev ? " 'unsafe-eval'" : ""}`,
    `style-src 'self' 'nonce-${nonce}'`,
    "style-src-attr 'unsafe-inline'",
    "img-src 'self' blob: data:",
    "font-src 'self'",
    "connect-src 'self'",
    "object-src 'none'",
    "base-uri 'self'",
    "form-action 'self'",
    "frame-ancestors 'self'",
    ...(esDev ? [] : ["upgrade-insecure-requests"]),
  ].join("; ");
}

// Primera barrera: sin JWT válido no se entra al panel. La autorización por
// rol y la vigencia del usuario se vuelven a comprobar en cada página y
// Server Action (ver lib/auth/session.ts).
export async function proxy(request: NextRequest) {
  if (request.nextUrl.pathname.startsWith("/dashboard")) {
    const claims = await verificarToken(
      request.cookies.get(SESSION_COOKIE)?.value,
    );
    if (!claims) {
      const login = new URL("/login", request.url);
      login.searchParams.set(
        "next",
        request.nextUrl.pathname + request.nextUrl.search,
      );
      return NextResponse.redirect(login);
    }
  }

  const nonce = Buffer.from(crypto.randomUUID()).toString("base64");
  const csp = construirCsp(nonce);

  const requestHeaders = new Headers(request.headers);
  requestHeaders.set("x-nonce", nonce);
  requestHeaders.set(CSP_HEADER, csp);

  const response = NextResponse.next({ request: { headers: requestHeaders } });
  response.headers.set(CSP_HEADER, csp);
  return response;
}

// Todo salvo estáticos e imágenes optimizadas; /api no necesita CSP.
export const config = {
  matcher: [
    "/((?!api|_next/static|_next/image|icons|favicon.ico|sitemap.xml|robots.txt).*)",
  ],
};
