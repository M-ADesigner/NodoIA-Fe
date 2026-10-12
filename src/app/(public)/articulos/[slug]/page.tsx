import Link from "next/link";
import { notFound } from "next/navigation";
import { PageHero } from "@/components/ui/PageHero";
import { articuloPublicadoPorSlug } from "@/lib/db/queries";

export const dynamic = "force-dynamic";

export default async function ArticuloPage(
  props: PageProps<"/articulos/[slug]">,
) {
  const { slug } = await props.params;
  const articulo = await articuloPublicadoPorSlug(slug);

  if (!articulo) {
    notFound();
  }

  return (
    <main className="flex flex-1 flex-col">
      <PageHero kicker={articulo.categoria} title={articulo.titulo} />

      <section className="flex flex-1 flex-col bg-white">
        <div className="mx-auto flex w-full max-w-2xl flex-col gap-6 px-5 py-20 sm:py-24">
          <p className="text-ink/45 text-xs">{articulo.publicadoEn}</p>
          <p className="text-ink/70 text-base leading-relaxed">
            {articulo.resumen}
          </p>
          <Link href="/articulos" className="text-navy-700 font-semibold">
            ← Volver a artículos
          </Link>
        </div>
      </section>
    </main>
  );
}
