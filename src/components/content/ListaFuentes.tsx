import type { Fuente } from "@/types/content";

// Lista de solo lectura: panel interno (revisión, vista del artículo) y sitio
// público. Las URL son http(s) validadas al guardar.
export function ListaFuentes({
  fuentes,
  className = "",
}: {
  fuentes: Fuente[];
  className?: string;
}) {
  if (fuentes.length === 0) return null;

  return (
    <ul className={`flex flex-col gap-2 ${className}`}>
      {fuentes.map((fuente) => (
        <li key={fuente.id} className="leading-snug">
          <a
            href={fuente.url}
            target="_blank"
            rel="noopener noreferrer"
            className="text-navy-700 font-semibold break-words hover:underline"
          >
            {fuente.titulo}
          </a>
          <span className="block text-sm text-slate-500">
            {fuente.tipo}
            {fuente.organismo ? ` · ${fuente.organismo}` : ""}
          </span>
        </li>
      ))}
    </ul>
  );
}
