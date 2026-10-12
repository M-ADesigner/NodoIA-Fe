"use client";

import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import type { ReactNode } from "react";

// Contenedor del panel con menú hamburguesa:
// - móvil (< md): la barra lateral es un cajón que se abre sobre el contenido;
// - escritorio (>= md): la hamburguesa colapsa o expande la barra lateral.
// El contenido de la barra lateral llega ya renderizado desde el servidor.
export function DashboardShell({
  sidebar,
  children,
}: {
  sidebar: ReactNode;
  children: ReactNode;
}) {
  const pathname = usePathname();
  // El cajón móvil guarda la ruta en la que se abrió: al navegar a otra ruta
  // deja de coincidir y se considera cerrado, sin efectos ni setState extra.
  const [abiertoEn, setAbiertoEn] = useState<string | null>(null);
  const [colapsado, setColapsado] = useState(false);
  const movilAbierto = abiertoEn !== null && abiertoEn === pathname;
  const setMovilAbierto = (abrir: boolean) =>
    setAbiertoEn(abrir ? pathname : null);

  useEffect(() => {
    if (!movilAbierto) return;
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") setAbiertoEn(null);
    }
    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
  }, [movilAbierto]);

  function alternar() {
    const esEscritorio = window.matchMedia("(min-width: 768px)").matches;
    if (esEscritorio) setColapsado((valor) => !valor);
    else setMovilAbierto(!movilAbierto);
  }

  // `aria-expanded` refleja el estado de la vista actual.
  const expandido = movilAbierto || !colapsado;

  return (
    <div className="bg-surface flex min-h-screen flex-1">
      {movilAbierto && (
        <div
          aria-hidden="true"
          onClick={() => setMovilAbierto(false)}
          className="fixed inset-0 z-40 bg-black/50 md:hidden"
        />
      )}

      <aside
        id="panel-lateral"
        className={`bg-navy-950 fixed inset-y-0 left-0 z-50 flex w-[280px] max-w-[86vw] flex-col gap-7 overflow-y-auto px-5 py-6 text-white transition-transform duration-200 md:sticky md:top-0 md:z-auto md:h-screen md:max-w-none md:shrink-0 md:translate-x-0 ${
          movilAbierto ? "translate-x-0" : "-translate-x-full"
        } ${colapsado ? "md:hidden" : ""}`}
      >
        {sidebar}
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        <div className="border-border sticky top-0 z-30 flex h-14 items-center gap-3 border-b bg-white/90 px-4 backdrop-blur sm:px-6">
          <button
            type="button"
            onClick={alternar}
            aria-label={expandido ? "Cerrar menú" : "Abrir menú"}
            aria-expanded={expandido}
            aria-controls="panel-lateral"
            className="text-navy-800 hover:bg-navy-100 inline-flex h-11 w-11 items-center justify-center rounded-xl transition-colors"
          >
            <span aria-hidden="true" className="relative block h-4 w-5">
              <span
                className={`absolute left-0 block h-0.5 w-5 rounded bg-current transition-all duration-200 ${
                  movilAbierto ? "top-[7px] rotate-45" : "top-0"
                }`}
              />
              <span
                className={`absolute top-[7px] left-0 block h-0.5 w-5 rounded bg-current transition-opacity duration-200 ${
                  movilAbierto ? "opacity-0" : "opacity-100"
                }`}
              />
              <span
                className={`absolute left-0 block h-0.5 w-5 rounded bg-current transition-all duration-200 ${
                  movilAbierto ? "top-[7px] -rotate-45" : "top-[14px]"
                }`}
              />
            </span>
          </button>
          <span className="text-navy-800 text-sm font-bold tracking-[0.12em] uppercase">
            Panel interno
          </span>
        </div>

        <main
          id="contenido"
          className="flex min-w-0 flex-1 flex-col px-5 py-8 sm:px-10 sm:py-10"
        >
          <div className="mx-auto flex w-full max-w-[1100px] flex-1 flex-col">
            {children}
          </div>
        </main>
      </div>
    </div>
  );
}
