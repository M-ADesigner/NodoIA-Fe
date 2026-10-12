import Link from "next/link";
import type { Articulo } from "@/types/content";

export function ArticleCard({ articulo }: { articulo: Articulo }) {
  return (
    <article className="border-border shadow-nav flex flex-col gap-2.5 rounded-medium border bg-white p-7 transition-shadow hover:shadow-box">
      <span className="text-[#5C7A12] text-xs font-bold tracking-widest uppercase">
        {articulo.categoria}
      </span>
      <h3 className="font-heading text-ink text-xl font-semibold">
        {articulo.titulo}
      </h3>
      <p className="text-ink/65 text-sm leading-relaxed">{articulo.resumen}</p>
      <div className="text-ink/45 mt-2 flex justify-between text-xs">
        <span>{articulo.publicadoEn}</span>
        <Link
          href={`/articulos/${articulo.slug}`}
          className="text-navy-700 font-semibold"
        >
          Leer más →
        </Link>
      </div>
    </article>
  );
}
