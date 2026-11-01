import type { Metadata } from "next";
import Link from "next/link";
import { AuthCard } from "@/components/ui/AuthCard";
import { hashToken } from "@/lib/auth/reset";
import { restablecimientoVigente } from "@/lib/db/queries";
import { NO_INDEXAR } from "@/lib/seo/metadata";
import { RestablecerForm } from "./RestablecerForm";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Nueva contraseña",
  robots: NO_INDEXAR,
  // El token viaja en la URL: que no salga en el Referer hacia otros sitios.
  referrer: "no-referrer",
};

export default async function RestablecerPage(props: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const { token } = await props.searchParams;
  const valor = typeof token === "string" && token.length <= 200 ? token : "";
  const vigente = valor
    ? await restablecimientoVigente(hashToken(valor))
    : false;

  if (!vigente) {
    return (
      <AuthCard
        titulo="Enlace no válido"
        descripcion="El enlace venció, ya se usó o no es correcto."
      >
        <Link
          href="/recuperar"
          className="text-navy-700 inline-flex text-[15px] font-semibold hover:underline"
        >
          Solicitar un enlace nuevo
        </Link>
      </AuthCard>
    );
  }

  return (
    <AuthCard
      titulo="Nueva contraseña"
      descripcion="Elige una contraseña nueva. Al guardarla se cerrarán las sesiones abiertas."
    >
      <RestablecerForm token={valor} />
    </AuthCard>
  );
}
