import { notFound } from "next/navigation";
import { articuloPorSlug } from "@/lib/content/mock-articulos";

export default async function ArticuloPage(
  props: PageProps<"/articulos/[slug]">,
) {
  const { slug } = await props.params;
  const articulo = articuloPorSlug(slug);

  if (!articulo) {
    notFound();
  }

  return (
    <main className="mx-auto flex w-full max-w-2xl flex-col gap-4 px-5 pt-[160px] pb-20 max-sm:pt-[140px]">
      <span className="text-[#5C7A12] text-xs font-bold tracking-widest uppercase">
        {articulo.categoria}
      </span>
      <h1 className="text-ink text-[32px] leading-[1.33] font-bold xl:text-[36px]">
        {articulo.titulo}
      </h1>
      <p className="text-ink/45 text-xs">{articulo.publicadoEn}</p>
      <p className="text-ink/70 text-base leading-relaxed">
        {articulo.resumen}
      </p>
    </main>
  );
}
