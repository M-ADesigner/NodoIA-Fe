import type { Metadata } from "next";
import Link from "next/link";
import { ArticleCard } from "@/components/content/ArticleCard";
import { buttonClasses } from "@/components/ui/Button";
import { NetworkMotif } from "@/components/ui/NetworkMotif";
import { articulosPublicados } from "@/lib/db/queries";
import { JsonLd, organizacionLd, sitioWebLd } from "@/lib/seo/json-ld";
import { construirMetadata } from "@/lib/seo/metadata";
import {
  rutaArticulo,
  SITE_DESCRIPTION,
  SITE_LEGAL_NAME,
  SITE_NAME,
} from "@/lib/seo/site";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  ...construirMetadata({
    title: `${SITE_NAME} | ${SITE_LEGAL_NAME}`,
    description: SITE_DESCRIPTION,
    path: "/",
    imageAlt: SITE_LEGAL_NAME,
  }),
  // La home usa el título completo, sin el sufijo " | CCHIA".
  title: { absolute: `${SITE_NAME} | ${SITE_LEGAL_NAME}` },
};

const HAZTE_SOCIO_URL =
  "https://docs.google.com/forms/d/e/1FAIpQLSdRNGNM10IHgnvAnfOl75xvsgVm1hqwepqN-uqEy4ZjFOiQEg/viewform?usp=send_form";

const BENEFICIOS = [
  {
    titulo: "Eventos exclusivos",
    texto:
      "Acceso a encuentros, seminarios y actividades del ecosistema de inteligencia artificial.",
    icono:
      "M8 2v4M16 2v4M3 9h18M5 5h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z",
  },
  {
    titulo: "Networking con expertos",
    texto:
      "Conecta con profesionales, empresas e instituciones que están construyendo la IA en Chile.",
    icono:
      "M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75",
  },
  {
    titulo: "Recursos y capacitaciones",
    texto:
      "Material y formación para llevar la inteligencia artificial a tu organización.",
    icono:
      "M4 19.5A2.5 2.5 0 0 1 6.5 17H20M4 19.5A2.5 2.5 0 0 0 6.5 22H20V2H6.5A2.5 2.5 0 0 0 4 4.5v15z",
  },
];

function FlechaIcon() {
  return (
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
      <path d="M5 12h14M13 6l6 6-6 6" />
    </svg>
  );
}

export default async function Home() {
  const articulos = await articulosPublicados();
  const destacado = articulos[0];
  const recientes = articulos.slice(0, 3);

  return (
    <main id="contenido">
      <JsonLd data={[organizacionLd(), sitioWebLd()]} />

      {/* Hero */}
      <section className="bg-cchia-hero relative overflow-hidden">
        <NetworkMotif className="pointer-events-none absolute top-0 right-0 h-full w-[700px] opacity-55 max-lg:hidden" />
        <div className="relative mx-auto flex w-full max-w-[1200px] flex-wrap items-center gap-14 px-6 py-20 sm:py-24 lg:py-28">
          <div className="max-w-[640px] min-w-0 flex-[1_1_480px]">
            <span className="bg-primary/15 text-lime-soft inline-flex rounded-full px-4 py-2 text-[13px] font-semibold tracking-[0.08em] uppercase">
              Cámara Chilena de Inteligencia Artificial
            </span>
            <h1 className="mt-6 text-[38px] leading-[1.06] font-extrabold tracking-[-0.025em] !text-white sm:text-[52px] xl:text-[64px]">
              Impulsando la Inteligencia Artificial en Chile.
            </h1>
            <p className="mt-6 max-w-[540px] text-[19px] leading-relaxed text-white/85">
              Únete a la organización líder en IA de Chile. Conecta, innova y
              transforma el futuro con nosotros.
            </p>
            <div className="mt-9 flex flex-wrap gap-3.5">
              <a
                href={HAZTE_SOCIO_URL}
                target="_blank"
                rel="noopener noreferrer"
                className={buttonClasses("accent", "px-8")}
              >
                Hazte socio
              </a>
              <Link
                href="/articulos"
                className={buttonClasses("ghost", "px-8")}
              >
                Leer artículos
              </Link>
            </div>
          </div>

          {destacado && (
            <div className="ml-auto max-w-[460px] min-w-0 flex-[1_1_340px]">
              <Link
                href={rutaArticulo(destacado.slug)}
                className="shadow-box block rounded-[24px] bg-white p-7 transition hover:-translate-y-0.5"
              >
                <span className="text-lime-ink text-xs font-bold tracking-[0.12em] uppercase">
                  Último {destacado.categoria.toLowerCase()}
                </span>
                <h2 className="font-heading text-navy-800 mt-3 text-2xl leading-tight font-bold">
                  {destacado.titulo}
                </h2>
                <p className="mt-3 text-[15px] leading-relaxed text-slate-600">
                  {destacado.resumen}
                </p>
                <span className="text-navy-700 mt-5 inline-flex items-center gap-2 text-[15px] font-semibold">
                  Leer artículo <FlechaIcon />
                </span>
              </Link>
            </div>
          )}
        </div>
      </section>

      {/* Beneficios */}
      <section className="bg-white">
        <div className="mx-auto w-full max-w-[1200px] px-6 pt-20 pb-6 sm:pt-24">
          <div className="max-w-[640px]">
            <p className="text-lime-ink text-[13px] font-bold tracking-[0.12em] uppercase">
              Ser parte de la cámara
            </p>
            <h2 className="text-navy-800 mt-3 text-[30px] leading-[1.15] font-extrabold sm:text-[40px]">
              Un nodo que conecta al ecosistema de IA
            </h2>
          </div>
          <ul className="mt-10 grid grid-cols-[repeat(auto-fit,minmax(260px,1fr))] gap-6">
            {BENEFICIOS.map((beneficio) => (
              <li
                key={beneficio.titulo}
                className="border-border bg-surface rounded-medium flex flex-col gap-4 border p-8"
              >
                <span className="bg-navy-900 inline-flex h-[52px] w-[52px] items-center justify-center rounded-2xl">
                  <svg
                    aria-hidden="true"
                    width="26"
                    height="26"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="#b1e346"
                    strokeWidth="1.8"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  >
                    <path d={beneficio.icono} />
                  </svg>
                </span>
                <h3 className="font-heading text-navy-800 text-[21px] font-bold">
                  {beneficio.titulo}
                </h3>
                <p className="text-[15px] leading-relaxed text-slate-600">
                  {beneficio.texto}
                </p>
              </li>
            ))}
          </ul>
        </div>
      </section>

      {/* Últimos artículos */}
      <section className="bg-white">
        <div className="mx-auto w-full max-w-[1200px] px-6 pt-16 pb-20 sm:pb-24">
          <div className="flex flex-wrap items-end justify-between gap-4">
            <h2 className="text-navy-800 text-[30px] leading-[1.15] font-extrabold sm:text-[40px]">
              Últimos artículos
            </h2>
            <Link
              href="/articulos"
              className="text-navy-700 inline-flex items-center gap-2 text-[15px] font-semibold"
            >
              Ver todos los artículos <FlechaIcon />
            </Link>
          </div>
          {recientes.length === 0 ? (
            <p className="text-ink/60 mt-10">
              Aún no hay artículos publicados. Vuelve pronto.
            </p>
          ) : (
            <div className="mt-9 grid grid-cols-[repeat(auto-fit,minmax(300px,1fr))] gap-6">
              {recientes.map((articulo) => (
                <ArticleCard key={articulo.id} articulo={articulo} />
              ))}
            </div>
          )}
        </div>
      </section>

      {/* Invitación a socios */}
      <section className="bg-white">
        <div className="mx-auto w-full max-w-[1200px] px-6 pb-20 sm:pb-24">
          <div className="bg-cchia-blue flex flex-wrap items-center justify-between gap-8 rounded-[32px] px-8 py-12 sm:px-12 sm:py-14">
            <div className="max-w-[640px] min-w-0 flex-[1_1_380px]">
              <h2 className="text-[26px] leading-[1.15] font-extrabold !text-white sm:text-[38px]">
                Únete a la organización líder en IA de Chile
              </h2>
              <p className="mt-4 text-[17px] leading-relaxed text-white/85">
                Accede a eventos exclusivos, networking con expertos y recursos
                y capacitaciones.
              </p>
            </div>
            <a
              href={HAZTE_SOCIO_URL}
              target="_blank"
              rel="noopener noreferrer"
              className={buttonClasses("accent", "min-h-[54px] px-9")}
            >
              Hazte socio
            </a>
          </div>
        </div>
      </section>
    </main>
  );
}
