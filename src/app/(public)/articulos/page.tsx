import { ArticleCard } from "@/components/content/ArticleCard";
import { PageHero } from "@/components/ui/PageHero";
import { articulosPublicados } from "@/lib/db/queries";

export const dynamic = "force-dynamic";

export default async function ArticulosPage() {
  const articulos = await articulosPublicados();

  return (
    <main className="flex flex-1 flex-col">
      <PageHero
        kicker="Cámara Chilena de Inteligencia Artificial"
        title="Artículos"
        description="Noticias, boletines y análisis del ecosistema de inteligencia artificial en Chile."
      />

      <section className="flex flex-1 flex-col bg-white">
        <div className="mx-auto flex w-full max-w-[1290px] flex-col items-center gap-8 px-5 py-20 sm:gap-10 sm:py-24">
          <div className="grid w-full grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
            {articulos.map((articulo) => (
              <ArticleCard key={articulo.id} articulo={articulo} />
            ))}
          </div>
        </div>
      </section>
    </main>
  );
}
