import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { redirect } from "next/navigation";
import { cerrarSesion } from "@/app/login/actions";
import { DashboardNav } from "@/components/dashboard/DashboardNav";
import { DashboardShell } from "@/components/dashboard/DashboardShell";
import { ROL_LABEL, vistasPermitidas } from "@/lib/auth/roles";
import { usuarioActual } from "@/lib/auth/session";
import { NO_INDEXAR } from "@/lib/seo/metadata";

export const dynamic = "force-dynamic";

// El panel interno nunca debe indexarse.
export const metadata: Metadata = {
  title: { default: "Panel interno", template: "%s | Panel CCHIA" },
  robots: NO_INDEXAR,
};

export default async function DashboardLayout({
  children,
}: LayoutProps<"/dashboard">) {
  // El proxy ya exige un JWT válido; aquí se confirma que el usuario sigue
  // existiendo y activo. Cada página vuelve a exigir su permiso.
  const usuario = await usuarioActual();
  if (!usuario) redirect("/login");

  const items = vistasPermitidas(usuario.rol).map(({ href, label }) => ({
    href,
    label,
  }));

  const sidebar = (
    <>
      <Link
        href="/"
        aria-label="Ir al sitio público"
        className="inline-flex self-start rounded-[14px] bg-white px-3.5 py-2"
      >
        <Image
          src="/images/Logo_CCHIA.png"
          alt="CCHIA"
          width={92}
          height={38}
          className="h-[38px] w-auto object-contain"
        />
      </Link>

      <div>
        <p className="mb-2.5 px-3 text-xs font-bold tracking-[0.12em] text-white/55 uppercase">
          Menú
        </p>
        <DashboardNav items={items} />
      </div>

      <div className="mt-auto flex flex-col gap-1 rounded-2xl bg-white/6 p-4">
        <span className="text-[15px] font-semibold text-white">
          {usuario.nombre}
        </span>
        <span className="text-[13px] text-white/65">
          {ROL_LABEL[usuario.rol]}
        </span>
        <form action={cerrarSesion} className="mt-3">
          <button
            type="submit"
            className="inline-flex min-h-10 w-full items-center justify-center rounded-full border-[1.5px] border-white/35 text-sm font-semibold text-white transition-colors hover:bg-white/10"
          >
            Cerrar sesión
          </button>
        </form>
      </div>
    </>
  );

  return <DashboardShell sidebar={sidebar}>{children}</DashboardShell>;
}
