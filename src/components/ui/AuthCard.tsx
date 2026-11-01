import type { ReactNode } from "react";
import Image from "next/image";
import Link from "next/link";

// Tarjeta centrada para las pantallas de acceso que no son el login completo
// (recuperar y restablecer contraseña).
export function AuthCard({
  titulo,
  descripcion,
  children,
}: {
  titulo: string;
  descripcion: string;
  children: ReactNode;
}) {
  return (
    <main
      id="contenido"
      className="bg-surface flex min-h-screen flex-1 items-center justify-center px-6 py-12"
    >
      <div className="border-border shadow-box w-full max-w-[420px] rounded-[28px] border bg-white px-9 py-10">
        <Link href="/" aria-label="Volver al sitio" className="inline-flex">
          <Image
            src="/images/Logo_CCHIA.png"
            alt="CCHIA"
            width={106}
            height={44}
            className="h-11 w-auto object-contain"
          />
        </Link>
        <h1 className="text-navy-800 mt-6 text-[28px] leading-tight font-extrabold">
          {titulo}
        </h1>
        <p className="mt-2 text-[15px] leading-normal text-slate-600">
          {descripcion}
        </p>
        <div className="mt-7">{children}</div>
      </div>
    </main>
  );
}
