import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { CompartirArticulo } from "@/components/content/CompartirArticulo";
import { ListaFuentes } from "@/components/content/ListaFuentes";
import { buttonClasses } from "@/components/ui/Button";
import { PageHero } from "@/components/ui/PageHero";
import {
  articuloPublicadoPorSlug,
  articulosRelacionados,
  fuentesDeArticulo,
} from "@/lib/db/queries";
import { fechaLarga, minutosDeLectura, mismoDia } from "@/lib/content/fecha";
import { articuloLd, JsonLd, migasDePanLd } from "@/lib/seo/json-ld";
import { construirMetadata } from "@/lib/seo/metadata";
import {
  absoluteUrl,
  HAZTE_SOCIO_URL,
  rutaArticulo,
  SITE_LEGAL_NAME,
} from "@/lib/seo/site";

export const dynamic = "force-dynamic";

export async function generateMetadata(
  props: PageProps<"/articulos/[slug]">,
): Promise<Metadata> {
  const { slug } = await props.params;
  const articulo = await articuloPublicadoPorSlug(slug);

  if (!articulo) {
    return { title: "Artículo no encontrado" };
  }

  return construirMetadata({
    title: articulo.titulo,
    description: articulo.resumen,
    path: rutaArticulo(articulo.slug),
    image: `/og/articulos/${encodeURIComponent(articulo.slug)}`,
    imageAlt: articulo.titulo,
    type: "article",
    publishedTime: articulo.publicadoEnIso,
    modifiedTime: articulo.actualizadoEnIso,
    section: articulo.categoria,
  });
}

export default async function ArticuloPage(
  props: PageProps<"/articulos/[slug]">,
) {
  const { slug } = await props.params;
  const articulo = await articuloPublicadoPorSlug(slug);

  if (!articulo) {
    notFound();
  }

  const [relacionados, fuentes] = await Promise.all([
    articulosRelacionados(articulo.slug, 2),
    fuentesDeArticulo(articulo.id),
  ]);

  return (
    <main id="contenido" className="flex flex-1 flex-col">
      <JsonLd
        data={[
          migasDePanLd([
            { nombre: "Inicio", path: "/" },
            { nombre: "Artículos", path: "/articulos" },
            { nombre: articulo.titulo, path: rutaArticulo(articulo.slug) },
          ]),
          articuloLd(articulo),
        ]}
      />
      <PageHero
        kicker={articulo.categoria}
        title={articulo.titulo}
        align="left"
      />

      <div className="bg-white">
        <div className="mx-auto flex w-full max-w-[1100px] flex-wrap items-start gap-14 px-6 py-14 sm:py-16">
          <article className="max-w-[700px] min-w-0 flex-[1_1_560px]">
            <nav
              aria-label="Ruta de navegación"
              className="text-sm text-slate-500"
            >
              <ol className="flex flex-wrap items-center gap-1.5">
                <li>
                  <Link href="/" className="hover:underline">
                    Inicio
                  </Link>
                </li>
                <li aria-hidden="true">/</li>
                <li>
                  <Link href="/articulos" className="hover:underline">
                    Artículos
                  </Link>
                </li>
              </ol>
            </nav>

            <div className="mt-5 flex flex-wrap items-center gap-x-3 gap-y-2 text-sm text-slate-500">
              <span className="bg-primary/20 text-lime-ink rounded-full px-3 py-1 text-xs font-bold tracking-wide uppercase">
                {articulo.categoria}
              </span>
              {articulo.publicadoEnIso && (
                <span>
                  Publicado el{" "}
                  <time dateTime={articulo.publicadoEnIso}>
                    {fechaLarga(articulo.publicadoEnIso)}
                  </time>
                </span>
              )}
              {articulo.publicadoEnIso &&
                articulo.actualizadoEnIso &&
                !mismoDia(
                  articulo.publicadoEnIso,
                  articulo.actualizadoEnIso,
                ) && (
                  <span>
                    · Actualizado el{" "}
                    <time dateTime={articulo.actualizadoEnIso}>
                      {fechaLarga(articulo.actualizadoEnIso)}
                    </time>
                  </span>
                )}
              <span>
                · {minutosDeLectura(articulo.resumen, articulo.contenido)} min
                de lectura
              </span>
              <span>· {SITE_LEGAL_NAME}</span>
            </div>

            <p className="font-heading text-navy-800 mt-6 text-[22px] leading-[1.5] font-semibold">
              {articulo.resumen}
            </p>

            {articulo.contenido
              .split(/\n\s*\n/)
              .filter((parrafo) => parrafo.trim())
              .map((parrafo, i) => (
                <p
                  key={i}
                  className="mt-5 text-[17px] leading-relaxed whitespace-pre-line text-slate-700"
                >
                  {parrafo.trim()}
                </p>
              ))}

            <div className="border-border mt-10 border-t pt-6">
              <CompartirArticulo
                url={absoluteUrl(rutaArticulo(articulo.slug))}
                titulo={articulo.titulo}
              />
            </div>

            {fuentes.length > 0 && (
              <section
                aria-labelledby="fuentes"
                className="border-border mt-10 border-t pt-6"
              >
                <h2
                  id="fuentes"
                  className="text-lime-ink text-[13px] font-bold tracking-[0.12em] uppercase"
                >
                  Fuentes
                </h2>
                <ListaFuentes fuentes={fuentes} className="mt-4" />
              </section>
            )}

            <Link
              href="/articulos"
              className="text-navy-700 mt-10 inline-flex items-center gap-2 text-[15px] font-semibold"
            >
              <svg
                aria-hidden="true"
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M19 12H5M11 6l-6 6 6 6" />
              </svg>
              Volver a artículos
            </Link>
          </article>

          <aside className="flex max-w-[340px] min-w-0 flex-[1_1_280px] flex-col gap-5">
            <div className="bg-cchia-blue rounded-medium p-7 text-white">
              <h2 className="text-xl leading-tight font-bold text-white!">
                Sé parte de la cámara
              </h2>
              <p className="mt-3 text-[15px] leading-relaxed text-white/85">
                Eventos exclusivos, networking con expertos y recursos para tu
                organización.
              </p>
              <a
                href={HAZTE_SOCIO_URL}
                target="_blank"
                rel="noopener noreferrer"
                className={buttonClasses("accent", "mt-5 min-h-12")}
              >
                Hazte socio
              </a>
            </div>

            {relacionados.length > 0 && (
              <div className="border-border bg-surface rounded-medium border px-7 py-6">
                <h2 className="text-lime-ink text-[13px] font-bold tracking-[0.12em] uppercase">
                  Seguir leyendo
                </h2>
                <ul className="mt-4 flex flex-col gap-4">
                  {relacionados.map((relacionado) => (
                    <li key={relacionado.id}>
                      <Link
                        href={rutaArticulo(relacionado.slug)}
                        className="font-heading text-navy-800 text-base leading-snug font-bold hover:underline"
                      >
                        {relacionado.titulo}
                      </Link>
                      {relacionado.publicadoEn && (
                        <div className="mt-1 text-[13px] text-slate-500">
                          {relacionado.publicadoEn}
                        </div>
                      )}
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </aside>
        </div>
      </div>
    </main>
  );
}
