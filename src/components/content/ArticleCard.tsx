import Link from "next/link";
import { rutaArticulo } from "@/lib/seo/site";
import type { Articulo, CategoriaContenido } from "@/types/content";

// Colores de la etiqueta por categoría (alineados con el sistema de marca).
const CATEGORIA_CLASS: Record<CategoriaContenido, string> = {
  Artículo: "bg-[#f1ece3] text-[#5a4630]",
  Boletín: "bg-navy-100 text-navy-900",
  "Noticia institucional": "bg-lime-soft text-lime-ink",
};

export function ArticleCard({
  articulo,
  headingLevel = 3,
}: {
  articulo: Articulo;
  /** Nivel del título, para no saltarse niveles según dónde se use. */
  headingLevel?: 2 | 3;
}) {
  const Heading = `h${headingLevel}` as const;
  const href = rutaArticulo(articulo.slug);

  return (
    <article className="border-border shadow-nav rounded-medium hover:shadow-box flex flex-col gap-3 border bg-white p-7 transition hover:-translate-y-0.5">
      <span
        className={`self-start rounded-full px-3 py-1 text-xs font-bold tracking-[0.06em] uppercase ${CATEGORIA_CLASS[articulo.categoria]}`}
      >
        {articulo.categoria}
      </span>
      <Heading className="font-heading text-navy-800 mt-1 text-[21px] leading-[1.3] font-bold">
        <Link href={href} className="hover:underline">
          {articulo.titulo}
        </Link>
      </Heading>
      <p className="text-[15px] leading-relaxed text-slate-600">
        {articulo.resumen}
      </p>
      <div className="mt-auto flex items-center justify-between pt-3 text-[13px] text-slate-500">
        {articulo.publicadoEn ? (
          <time dateTime={articulo.publicadoEnIso}>{articulo.publicadoEn}</time>
        ) : (
          <span />
        )}
        <Link
          href={href}
          className="text-navy-700 text-[15px] font-semibold"
          aria-label={`Leer más: ${articulo.titulo}`}
        >
          Leer más →
        </Link>
      </div>
    </article>
  );
}
