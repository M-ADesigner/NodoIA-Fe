import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { redirect } from "next/navigation";
import { NetworkMotif } from "@/components/ui/NetworkMotif";
import { vistasPermitidas } from "@/lib/auth/roles";
import { usuarioActual } from "@/lib/auth/session";
import { NO_INDEXAR } from "@/lib/seo/metadata";
import { LoginForm } from "./LoginForm";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Ingresar",
  robots: NO_INDEXAR,
};

export default async function LoginPage(props: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const searchParams = await props.searchParams;
  const next = typeof searchParams.next === "string" ? searchParams.next : "";
  const sinPermiso = searchParams.error === "sin-permiso";

  const usuario = await usuarioActual();
  if (usuario) {
    const primera = vistasPermitidas(usuario.rol)[0]?.href;
    if (primera) redirect(primera);
  }

  return (
    <main
      id="contenido"
      className="flex min-h-screen flex-1 flex-wrap bg-white"
    >
      {/* Panel de marca */}
      <section className="relative flex min-h-[320px] flex-[1_1_520px] flex-col justify-between overflow-hidden bg-[linear-gradient(160deg,#0e2240_0%,#1a365d_45%,#2b77ad_100%)] p-8 text-white sm:p-12">
        <NetworkMotif className="pointer-events-none absolute inset-0 h-full w-full opacity-50" />
        <Link
          href="/"
          aria-label="Volver al sitio"
          className="relative inline-flex self-start rounded-2xl bg-white px-4 py-2.5"
        >
          <Image
            src="/images/Logo_CCHIA.png"
            alt="CCHIA"
            width={106}
            height={44}
            className="h-11 w-auto object-contain"
            priority
          />
        </Link>
        <div className="relative mt-16 max-w-[460px]">
          <p className="text-lime-soft text-[13px] font-bold tracking-[0.12em] uppercase">
            Panel interno
          </p>
          <h1 className="mt-4 text-[30px] leading-[1.12] font-extrabold text-white! sm:text-[40px]">
            Gestiona el contenido de la cámara en un solo lugar.
          </h1>
          <p className="mt-4 text-[17px] leading-relaxed text-white/85">
            Redacta con apoyo de IA, revisa y publica artículos y boletines.
          </p>
        </div>
      </section>

      {/* Formulario */}
      <section className="bg-surface flex flex-[1_1_480px] items-center justify-center px-6 py-12">
        <div className="border-border shadow-box w-full max-w-[420px] rounded-[28px] border bg-white px-9 py-10">
          <h2 className="text-navy-800 text-[28px] leading-tight font-extrabold">
            Ingresar
          </h2>
          <p className="mt-2 text-[15px] leading-normal text-slate-600">
            Usa la cuenta que te entregó el administrador.
          </p>

          {sinPermiso && (
            <p
              role="alert"
              className="mt-5 rounded-xl bg-amber-50 px-3.5 py-3 text-sm leading-snug text-amber-800"
            >
              Tu cuenta no tiene acceso a ninguna vista del panel. Contacta a un
              administrador.
            </p>
          )}

          <div className="mt-7">
            <LoginForm next={next} />
          </div>

          <Link
            href="/"
            className="text-navy-700 mt-6 inline-flex items-center gap-2 text-[15px] font-semibold"
          >
            <svg
              aria-hidden="true"
              width="18"
              height="18"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <path d="M19 12H5M11 6l-6 6 6 6" />
            </svg>
            Volver al sitio
          </Link>
        </div>
      </section>
    </main>
  );
}
