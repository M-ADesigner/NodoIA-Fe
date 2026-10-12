import Image from "next/image";
import Link from "next/link";
import { buttonClasses } from "@/components/ui/Button";

const HAZTE_SOCIO_URL =
  "https://docs.google.com/forms/d/e/1FAIpQLSdRNGNM10IHgnvAnfOl75xvsgVm1hqwepqN-uqEy4ZjFOiQEg/viewform?usp=send_form";

export default function PublicLayout({ children }: LayoutProps<"/">) {
  return (
    <div className="bg-cchia-light flex flex-1 flex-col text-base antialiased">
      <header className="fixed top-0 left-0 z-[1000000000] w-full bg-white pt-8 pb-2">
        <nav className="mx-auto flex w-full max-w-[1290px] items-center justify-between gap-4 px-5">
          <Link href="/" className="shrink-0">
            <Image
              src="/images/Logo_CCHIA.png"
              alt="Logo Cámara Chilena de Inteligencia Artificial"
              width={200}
              height={64}
              className="h-16 w-auto max-w-[200px] object-contain"
              priority
            />
          </Link>
          <a
            href={HAZTE_SOCIO_URL}
            target="_blank"
            rel="noopener noreferrer"
            className={buttonClasses("primary", "px-6 py-2")}
          >
            Hazte Socio
          </a>
        </nav>
      </header>

      {children}

      <footer className="bg-white py-4 text-center text-sm font-bold text-gray-600">
        © 2026 CCHIA - Cámara Chilena de Inteligencia Artificial. Todos los
        derechos reservados.
      </footer>
    </div>
  );
}