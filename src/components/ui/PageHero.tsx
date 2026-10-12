import { NetworkMotif } from "@/components/ui/NetworkMotif";

// Banda superior de las páginas interiores: la misma pieza que el hero de la
// home (degradado azul, motivo de nodos) pero más corta.
export function PageHero({
  kicker,
  title,
  description,
  align = "center",
}: {
  kicker?: string;
  title: string;
  description?: string;
  align?: "center" | "left";
}) {
  const alineacion = align === "center" ? "mx-auto text-center" : "text-left";

  return (
    <section className="bg-cchia-hero relative overflow-hidden">
      <NetworkMotif className="pointer-events-none absolute top-0 right-0 h-full w-[520px] opacity-40 max-md:hidden" />
      <div className="relative mx-auto w-full max-w-[1200px] px-6 py-16 sm:py-20">
        <div className={`flex max-w-[760px] flex-col gap-4 ${alineacion}`}>
          {kicker && (
            <p className="text-lime-soft text-sm font-semibold tracking-[0.12em] uppercase">
              {kicker}
            </p>
          )}
          {/* `!` fuerza el blanco: el h1 global de globals.css tiene mayor
              prioridad que las utilidades de Tailwind. */}
          <h1 className="text-[34px] leading-[1.1] font-extrabold text-white! sm:text-[44px] xl:text-[52px]">
            {title}
          </h1>
          {description && (
            <p className="max-w-[600px] text-lg leading-relaxed text-white/85">
              {description}
            </p>
          )}
        </div>
      </div>
    </section>
  );
}
