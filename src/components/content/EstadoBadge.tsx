import type { EstadoContenido } from "@/types/content";

const ESTADOS: Record<EstadoContenido, { label: string; className: string }> = {
  borrador: {
    label: "Borrador",
    className: "bg-neutral-soft text-neutral-ink",
  },
  asistido_ia: {
    label: "Asistido por IA",
    className: "bg-primary/20 text-lime-ink",
  },
  en_revision: {
    label: "En revisión",
    className: "bg-navy-700/12 text-navy-700",
  },
  aprobado: {
    label: "Aprobado",
    className: "bg-success-soft text-success-ink",
  },
  publicado: {
    label: "Publicado",
    className: "bg-navy-900/10 text-navy-900",
  },
};

export function EstadoBadge({ estado }: { estado: EstadoContenido }) {
  const { label, className } = ESTADOS[estado];
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold ${className}`}
    >
      {label}
    </span>
  );
}
