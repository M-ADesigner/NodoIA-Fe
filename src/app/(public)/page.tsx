import Link from "next/link";
import { buttonClasses } from "@/components/ui/Button";
import { ArticleCard } from "@/components/content/ArticleCard";
import { articulosPublicados } from "@/lib/content/mock-articulos";

const HAZTE_SOCIO_URL =
  "https://docs.google.com/forms/d/e/1FAIpQLSdRNGNM10IHgnvAnfOl75xvsgVm1hqwepqN-uqEy4ZjFOiQEg/viewform?usp=send_form";

const BENEFICIOS = [
  "Acceso a eventos exclusivos",
  "Networking con expertos",
  "Recursos y capacitaciones",
];

function CheckIcon() {
  return (
    <svg
      width="20"
      height="20"
      viewBox="0 0 20 20"
      fill="none"
      aria-hidden="true"
    >
      <rect width="20" height="20" rx="10" className="fill-primary" />
      <path
        d="M9.31661 13.7561L14.7491 8.42144C15.0836 8.0959 15.0836 7.5697 14.7491 7.24416C14.4145 6.91861 13.8736 6.91861 13.539 7.24416L8.7116 11.9901L6.46096 9.78807C6.12636 9.46253 5.58554 9.46253 5.25095 9.78807C4.91635 10.1136 4.91635 10.6398 5.25095 10.9654L8.1066 13.7561C8.27347 13.9184 8.49253 14 8.7116 14C8.93067 14 9.14974 13.9184 9.31661 13.7561Z"
        className="fill-white"
      />
    </svg>
  );
}

export default function Home() {
  const articulos = articulosPublicados();

  return (
    <>
      <section className="bg-cchia-hero relative overflow-hidden pt-[230px] pb-0 max-lg:pt-[160px]">
        <div className="mx-auto w-full max-w-[1290px] px-5">
          <div className="grid max-lg:gap-y-10 relative z-10 grid-cols-12 items-center gap-5">
            <div className="text-white relative col-span-12 mb-[60px] text-center lg:mb-[100px]">
              <p className="max-lg:mb-4 mb-8 font-medium text-white uppercase">
                Cámara Chilena de Inteligencia Artificial
              </p>
              <h1 className="max-md:mb-8 text-[36px] leading-[1.33] font-bold text-white xl:leading-[1.2] xl:text-[64px]">
                Impulsando la Inteligencia Artificial <br /> en Chile.
              </h1>
              <p className="max-md:mb-8 mx-auto mb-12 max-w-[590px] text-white">
                Únete a la organización líder en IA de Chile. Conecta, innova y
                transforma el futuro con nosotros.
              </p>
              <div className=" mx-auto mb-10 flex max-md:flex-col items-center justify-center gap-4">
                <Link href="/request-info" className={buttonClasses("primary")}>
                  Contáctanos
                </Link>
                <a
                  href={HAZTE_SOCIO_URL}
                  target="_blank"
                  rel="noopener noreferrer"
                  className={buttonClasses("outline")}
                >
                  Hazte Socio
                </a>
              </div>
              <ul className="mx-auto mt-5 flex max-w-[815px] max-md:flex-col max-md:gap-5 items-center justify-between">
                {BENEFICIOS.map((beneficio) => (
                  <li key={beneficio} className="flex items-center gap-3.5">
                    <CheckIcon />
                    <p className="text-white">{beneficio}</p>
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </div>
      </section>

      <section className="bg-white">
        <div className="mx-auto flex w-full max-w-[1290px] flex-col items-center gap-8 px-5 py-20 sm:gap-10 sm:py-24">
          <h2 className="text-ink text-center text-[32px] leading-[1.33] font-bold xl:text-[36px]">
            Últimos artículos
          </h2>
          <div className="grid w-full grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
            {articulos.map((articulo) => (
              <ArticleCard key={articulo.id} articulo={articulo} />
            ))}
          </div>
        </div>
      </section>
    </>
  );
}