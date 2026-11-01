import type { Metadata } from "next";
import Link from "next/link";
import { AuthCard } from "@/components/ui/AuthCard";
import { NO_INDEXAR } from "@/lib/seo/metadata";
import { RecuperarForm } from "./RecuperarForm";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Recuperar contraseña",
  robots: NO_INDEXAR,
};

export default function RecuperarPage() {
  return (
    <AuthCard
      titulo="Recuperar contraseña"
      descripcion="Ingresa tu correo y te enviaremos un enlace para elegir una nueva contraseña."
    >
      <RecuperarForm />
      <Link
        href="/login"
        className="text-navy-700 mt-6 inline-flex text-[15px] font-semibold hover:underline"
      >
        Volver a ingresar
      </Link>
    </AuthCard>
  );
}
