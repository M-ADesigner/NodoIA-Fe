import type { Metadata } from "next";
import Link from "next/link";
import { ArticleCard } from "@/components/content/ArticleCard";
import { PageHero } from "@/components/ui/PageHero";
import { articulosPublicados } from "@/lib/db/queries";
import { JsonLd, listadoArticulosLd, migasDePanLd } from "@/lib/seo/json-ld";
import { construirMetadata } from "@/lib/seo/metadata";
import type { CategoriaContenido } from "@/types/content";

export const dynamic = "force-dynamic";

const DESCRIPCION =
  "Noticias, boletines y análisis del ecosistema de inteligencia artificial en Chile, publicados por la Cámara Chilena de Inteligencia Artificial.";

// El filtro es una variante de la misma página: el canonical siempre es
// /articulos para no duplicar contenido en buscadores.
export const metadata: Metadata = construirMetadata({
  title: "Artículos",
  description: DESCRIPCION,
  path: "/articulos",
});

const CATEGORIAS: CategoriaContenido[] = [
  "Artículo",
  "Boletín",
  "Noticia institucional",
];

function esCategoria(valor: unknown): valor is CategoriaContenido {
  return typeof valor === "string" && (CATEGORIAS as string[]).includes(valor);
}

export default async function ArticulosPage(props: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const { categoria } = await props.searchParams;
  const filtro = esCategoria(categoria) ? categoria : null;

  const todos = await articulosPublicados();
  const articulos = filtro
    ? todos.filter((articulo) => articulo.categoria === filtro)
    : todos;

  const chips: { label: string; href: string; activo: boolean }[] = [
    { label: "Todos", href: "/articulos", activo: filtro === null },
    ...CATEGORIAS.map((cat) => ({
      label: cat,
      href: `/articulos?categoria=${encodeURIComponent(cat)}`,
      activo: filtro === cat,
    })),
  ];

  return (
    <main id="contenido" className="flex flex-1 flex-col">
      <JsonLd
        data={[
          migasDePanLd([
            { nombre: "Inicio", path: "/" },
            { nombre: "Artículos", path: "/articulos" },
          ]),
          listadoArticulosLd("Artículos", "/articulos", todos),
        ]}
      />
      <PageHero
        kicker="Cámara Chilena de Inteligencia Artificial"
        title="Artículos"
        description="Noticias, boletines y análisis del ecosistema de inteligencia artificial en Chile."
        align="left"
      />

      <section className="flex flex-1 flex-col bg-white">
        <div className="mx-auto w-full max-w-[1200px] px-6 py-12 sm:py-16">
          <nav aria-label="Filtrar por categoría">
            <ul className="flex flex-wrap gap-2.5">
              {chips.map((chip) => (
                <li key={chip.label}>
                  <Link
                    href={chip.href}
                    aria-current={chip.activo ? "true" : undefined}
                    className={
                      chip.activo
                        ? "bg-navy-900 border-navy-900 inline-flex min-h-11 items-center rounded-full border-[1.5px] px-5 text-[15px] font-semibold text-white"
                        : "text-navy-900 hover:border-navy-700 border-border-btn inline-flex min-h-11 items-center rounded-full border-[1.5px] bg-white px-5 text-[15px] font-semibold"
                    }
                  >
                    {chip.label}
                  </Link>
                </li>
              ))}
            </ul>
          </nav>

          <p className="mt-7 text-sm text-slate-500" aria-live="polite">
            {articulos.length}{" "}
            {articulos.length === 1 ? "artículo" : "artículos"}
            {filtro ? ` en ${filtro}` : ""}
          </p>

          {articulos.length === 0 ? (
            <p className="text-ink/60 mt-10">
              Aún no hay artículos publicados en esta categoría.
            </p>
          ) : (
            <div className="mt-5 grid grid-cols-[repeat(auto-fill,minmax(320px,1fr))] gap-6">
              {articulos.map((articulo) => (
                <ArticleCard
                  key={articulo.id}
                  articulo={articulo}
                  headingLevel={2}
                />
              ))}
            </div>
          )}
        </div>
      </section>
    </main>
  );
}
