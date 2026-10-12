import { ArticleCard } from "@/components/content/ArticleCard";
import { articulosPublicados } from "@/lib/db/queries";

export const dynamic = "force-dynamic";

export default async function ArticulosPage() {
  const articulos = await articulosPublicados();

  return (
    <main className="flex flex-col gap-8 px-5 pt-[160px] pb-20 max-sm:pt-[140px]">
      <h1 className="text-ink text-[32px] leading-[1.33] font-bold xl:text-[36px]">
        Artículos
      </h1>
      <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
        {articulos.map((articulo) => (
          <ArticleCard key={articulo.id} articulo={articulo} />
        ))}
      </div>
    </main>
  );
}
