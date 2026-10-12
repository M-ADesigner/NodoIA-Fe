import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { buttonClasses } from "@/components/ui/Button";

// Next agrega por su cuenta `noindex` y el estado 404 a esta página.
export const metadata: Metadata = {
  title: "Página no encontrada",
};

export default function NotFound() {
  return (
    <main
      id="contenido"
      className="bg-cchia-hero flex min-h-screen flex-col items-center justify-center gap-6 px-5 py-16 text-center"
    >
      <Image
        src="/images/Logo_CCHIA.png"
        alt="CCHIA, Cámara Chilena de Inteligencia Artificial"
        width={155}
        height={64}
        className="h-16 w-auto rounded-lg bg-white px-3 py-2"
      />
      <p className="text-sm font-medium tracking-widest text-white/80 uppercase">
        Error 404
      </p>
      <h1 className="text-[32px] leading-[1.2] font-bold !text-white xl:text-[44px]">
        No encontramos esta página
      </h1>
      <p className="max-w-[480px] text-white/85">
        El enlace puede estar roto o la página fue movida. Vuelve al inicio o
        revisa nuestros artículos.
      </p>
      <div className="flex flex-col gap-3 sm:flex-row">
        <Link href="/" className={buttonClasses("primary")}>
          Ir al inicio
        </Link>
        <Link href="/articulos" className={buttonClasses("outline")}>
          Ver artículos
        </Link>
      </div>
    </main>
  );
}
