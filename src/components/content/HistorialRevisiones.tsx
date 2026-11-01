import { etiquetaEstado } from "@/components/content/EstadoBadge";
import type { Revision } from "@/types/content";

// Plegado por defecto para no empujar el formulario; `abierto` lo muestra
// desplegado (p. ej. en la revisión, donde el historial es parte de la decisión).
export function HistorialRevisiones({
  revisiones,
  abierto = false,
  className = "border-border border-t pt-3",
}: {
  revisiones: Revision[];
  abierto?: boolean;
  className?: string;
}) {
  if (revisiones.length === 0) return null;

  return (
    <details open={abierto} className={className}>
      <summary className="text-ink/60 cursor-pointer text-sm font-semibold">
        Historial ({revisiones.length})
      </summary>
      <ol className="mt-2.5 flex max-h-56 flex-col gap-2.5 overflow-y-auto text-sm">
        {revisiones.map((revision) => (
          <li key={revision.id} className="flex flex-col gap-0.5">
            <p className="text-ink">
              <span className="font-semibold">{revision.usuarioNombre}</span>{" "}
              movió de {etiquetaEstado(revision.estadoAnterior)} a{" "}
              <span className="font-semibold">
                {etiquetaEstado(revision.estadoNuevo)}
              </span>
              <span className="text-ink/45"> · {revision.creadoEn}</span>
            </p>
            {revision.comentario && (
              <p className="text-ink/70 border-border border-l-2 pl-3 whitespace-pre-line">
                {revision.comentario}
              </p>
            )}
          </li>
        ))}
      </ol>
    </details>
  );
}
