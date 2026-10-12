"use client";

import Image from "next/image";
import Link from "next/link";
import { usePathname } from "next/navigation";

const NAV_ITEMS = [
  { href: "/dashboard/contenido", label: "Contenido" },
  { href: "/dashboard/revision", label: "Revisión" },
  { href: "/dashboard/admin", label: "Administración" },
];

// Sin autenticación todavía: falta definir con el cliente quién ocupa cada
// rol (ver Guion_Entrevista_Cliente_CCHIA, sección "Roles y usuarios").
export default function DashboardLayout({
  children,
}: LayoutProps<"/dashboard">) {
  const pathname = usePathname();

  return (
    <div className="flex flex-1 flex-col bg-white">
      <header className="sticky top-0 z-50 border-b border-border bg-white/95 backdrop-blur">
        <div className="mx-auto flex w-full max-w-[1290px] items-center justify-between px-4 py-4 sm:px-8 lg:px-16">
          <div className="flex items-center gap-3">
            <Image
              src="/images/Logo_CCHIA.png"
              alt="Logo CCHIA"
              width={120}
              height={48}
              className="h-9 w-auto object-contain sm:h-11"
            />
            <span className="text-navy-700 hidden text-xs font-bold tracking-widest uppercase sm:inline">
              Panel interno
            </span>
          </div>
        </div>

        <nav className="mx-auto flex w-full max-w-[1290px] gap-6 overflow-x-auto px-4 whitespace-nowrap sm:gap-8 sm:px-8 lg:px-16">
          {NAV_ITEMS.map((item) => {
            const current = pathname?.startsWith(item.href);
            return (
              <Link
                key={item.href}
                href={item.href}
                className={
                  current
                    ? "border-primary text-ink -mb-px border-b-2 pt-1 pb-3.5 text-sm font-bold"
                    : "text-ink/55 hover:text-ink border-b-2 border-transparent pt-1 pb-3.5 text-sm font-medium transition"
                }
              >
                {item.label}
              </Link>
            );
          })}
        </nav>
      </header>

      <main className="bg-cchia-light flex flex-1 flex-col">
        <div className="mx-auto flex w-full max-w-[1290px] flex-1 flex-col px-4 py-8 sm:px-8 sm:py-12 lg:px-16">
          {children}
        </div>
      </main>
    </div>
  );
}