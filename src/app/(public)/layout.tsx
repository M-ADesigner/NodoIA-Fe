import Image from "next/image";
import Link from "next/link";
import { buttonClasses } from "@/components/ui/Button";
import { HAZTE_SOCIO_URL, SITE_LEGAL_NAME } from "@/lib/seo/site";

const NAV = [
  { href: "/", label: "Inicio" },
  { href: "/articulos", label: "Artículos" },
];

export default function PublicLayout({ children }: LayoutProps<"/">) {
  return (
    <div className="flex flex-1 flex-col bg-white text-base antialiased">
      <a
        href="#contenido"
        className="bg-ink sr-only z-[100] rounded-lg px-4 py-2 text-white focus:not-sr-only focus:fixed focus:top-3 focus:left-3"
      >
        Saltar al contenido
      </a>

      <header className="border-border sticky top-0 z-50 border-b bg-white/95 backdrop-blur">
        <div className="mx-auto flex w-full max-w-[1200px] items-center justify-between gap-4 px-6 py-3">
          <Link
            href="/"
            className="shrink-0"
            aria-label={`${SITE_LEGAL_NAME}, inicio`}
          >
            <Image
              src="/images/Logo_CCHIA.png"
              alt={`Logo ${SITE_LEGAL_NAME}`}
              width={126}
              height={52}
              className="h-[52px] w-auto object-contain"
              priority
            />
          </Link>

          <div className="flex items-center gap-6 sm:gap-8">
            <nav aria-label="Principal" className="hidden sm:block">
              <ul className="flex items-center gap-8 text-[15px] font-medium text-slate-600">
                {NAV.map((item) => (
                  <li key={item.href}>
                    <Link
                      href={item.href}
                      className="hover:text-navy-900 transition-colors"
                    >
                      {item.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </nav>

            <a
              href={HAZTE_SOCIO_URL}
              target="_blank"
              rel="noopener noreferrer"
              className={buttonClasses("primary", "min-h-11 px-6 py-2")}
            >
              Hazte socio
            </a>
          </div>
        </div>
      </header>

      {children}

      <footer className="bg-navy-950 text-white/80">
        <div className="mx-auto flex w-full max-w-[1200px] flex-wrap items-center justify-between gap-6 px-6 py-10">
          <div className="flex flex-col gap-1.5">
            <strong className="font-heading text-lg text-white">CCHIA</strong>
            <span className="text-sm">{SITE_LEGAL_NAME}</span>
          </div>
          <nav aria-label="Pie de página">
            <ul className="flex flex-wrap items-center gap-6 text-sm font-semibold">
              {NAV.map((item) => (
                <li key={item.href}>
                  <Link href={item.href} className="text-white hover:underline">
                    {item.label}
                  </Link>
                </li>
              ))}
            </ul>
          </nav>
          <p className="text-[13px]">
            © 2026 CCHIA. Todos los derechos reservados.
          </p>
        </div>
      </footer>
    </div>
  );
}
