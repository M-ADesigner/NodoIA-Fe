import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { SESSION_COOKIE, verificarToken } from "@/lib/auth/jwt";

// Primera barrera: sin JWT válido no se entra al panel. La autorización por
// rol y la vigencia del usuario se vuelven a comprobar en cada página y
// Server Action (ver lib/auth/session.ts).
export async function proxy(request: NextRequest) {
  const claims = await verificarToken(
    request.cookies.get(SESSION_COOKIE)?.value,
  );
  if (claims) return NextResponse.next();

  const login = new URL("/login", request.url);
  login.searchParams.set(
    "next",
    request.nextUrl.pathname + request.nextUrl.search,
  );
  return NextResponse.redirect(login);
}

export const config = {
  matcher: ["/dashboard", "/dashboard/:path*"],
};
