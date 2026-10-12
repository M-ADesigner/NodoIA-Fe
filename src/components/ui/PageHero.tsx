// Banda superior de las páginas interiores: misma pieza que el hero de la
// home (degradado azul, texto blanco) pero más corta.
export function PageHero({
  kicker,
  title,
  description,
}: {
  kicker?: string;
  title: string;
  description?: string;
}) {
  return (
    <section className="bg-cchia-hero relative overflow-hidden pt-[170px] pb-14 max-lg:pt-[150px]">
      <div className="mx-auto w-full max-w-[1290px] px-5">
        <div className="mx-auto flex max-w-[760px] flex-col gap-4 text-center">
          {kicker && (
            <p className="text-sm font-medium tracking-widest text-white/80 uppercase">
              {kicker}
            </p>
          )}
          {/* `!` fuerza el blanco: el h1 global de globals.css tiene mayor
              prioridad que las utilidades de Tailwind. */}
          <h1 className="text-[32px] leading-[1.33] font-bold text-white! xl:text-[44px] xl:leading-[1.2]">
            {title}
          </h1>
          {description && (
            <p className="mx-auto max-w-[590px] text-white/85">{description}</p>
          )}
        </div>
      </div>
    </section>
  );
}
