"use client";

import { useActionState, useEffect, useState } from "react";
import {
  decidirArticulo,
  type EstadoForm,
} from "@/app/dashboard/revision/actions";
import { Button } from "@/components/ui/Button";
import { Modal } from "@/components/ui/Modal";
import { Field } from "@/components/ui/Field";
import { EstadoBadge } from "@/components/content/EstadoBadge";
import { HistorialRevisiones } from "@/components/content/HistorialRevisiones";
import { ListaFuentes } from "@/components/content/ListaFuentes";
import type {
  Articulo,
  EstadoContenido,
  Fuente,
  Revision,
} from "@/types/content";
import type { Usuario } from "@/types/user";

type Hacia = Extract<
  EstadoContenido,
  "aprobado" | "borrador" | "en_revision" | "publicado"
>;

// Decisiones disponibles según el estado del artículo (ver workflow.ts).
const DECISIONES: Record<
  "en_revision" | "aprobado",
  {
    secundaria: { hacia: Hacia; boton: string; pregunta: string };
    principal: { hacia: Hacia; boton: string; pregunta: string };
  }
> = {
  en_revision: {
    secundaria: {
      hacia: "borrador",
      boton: "Devolver a borrador",
      pregunta:
        "¿Confirmas devolver este artículo a borrador para que el autor lo ajuste?",
    },
    principal: {
      hacia: "aprobado",
      boton: "Aprobar",
      pregunta: "¿Confirmas que este artículo pasa a estado aprobado?",
    },
  },
  aprobado: {
    secundaria: {
      hacia: "en_revision",
      boton: "Volver a revisión",
      pregunta: "¿Confirmas devolver este artículo a la cola de revisión?",
    },
    principal: {
      hacia: "publicado",
      boton: "Publicar",
      pregunta:
        "¿Confirmas publicar este artículo? Quedará visible en el sitio público.",
    },
  },
};

const inicial: EstadoForm = {};

type Seleccion = {
  articulo: Articulo;
  decision: { hacia: Hacia; boton: string; pregunta: string };
};

function ConfirmarDecision({
  seleccion,
  autor,
  revisiones,
  fuentes,
  onClose,
}: {
  seleccion: Seleccion;
  autor: Usuario | undefined;
  revisiones: Revision[];
  fuentes: Fuente[];
  onClose: () => void;
}) {
  const [resultado, decidir, pendiente] = useActionState(
    decidirArticulo,
    inicial,
  );
  const { articulo, decision } = seleccion;
  // Devolver exige motivo; aprobar y publicar lo dejan opcional.
  const exigeMotivo =
    decision.hacia === "borrador" || decision.hacia === "en_revision";

  useEffect(() => {
    if (resultado.ok) onClose();
  }, [resultado.ok, onClose]);

  return (
    <form action={decidir} className="flex flex-col gap-4">
      <input type="hidden" name="id" value={articulo.id} />
      <input type="hidden" name="hacia" value={decision.hacia} />
      <dl className="grid grid-cols-2 gap-3.5 text-sm">
        <Field label="Categoría">{articulo.categoria}</Field>
        <Field label="Actualizado">{articulo.actualizadoEn}</Field>
        <Field label="Autor">{autor?.nombre ?? "—"}</Field>
        <Field label="Correo del autor">{autor?.correo ?? "—"}</Field>
        <div className="border-border col-span-2 flex flex-col gap-1 border-t pt-3">
          <dt className="text-ink/50 text-xs font-medium tracking-wide uppercase">
            Resumen
          </dt>
          <dd className="text-ink leading-relaxed">{articulo.resumen}</dd>
        </div>
        {articulo.contenido && (
          <div className="border-border col-span-2 flex flex-col gap-1 border-t pt-3">
            <dt className="text-ink/50 text-xs font-medium tracking-wide uppercase">
              Contenido
            </dt>
            <dd className="text-ink max-h-56 overflow-y-auto leading-relaxed whitespace-pre-line">
              {articulo.contenido}
            </dd>
          </div>
        )}
        {fuentes.length > 0 && (
          <div className="border-border col-span-2 flex flex-col gap-1.5 border-t pt-3">
            <dt className="text-ink/50 text-xs font-medium tracking-wide uppercase">
              Fuentes ({fuentes.length})
            </dt>
            <dd className="max-h-40 overflow-y-auto">
              <ListaFuentes fuentes={fuentes} />
            </dd>
          </div>
        )}
      </dl>
      <HistorialRevisiones revisiones={revisiones} abierto />
      <label className="text-ink flex flex-col gap-1 text-xs font-medium tracking-wide uppercase">
        {exigeMotivo ? "Motivo (obligatorio)" : "Comentario (opcional)"}
        <textarea
          name="comentario"
          required={exigeMotivo}
          minLength={exigeMotivo ? 10 : undefined}
          maxLength={500}
          rows={3}
          placeholder={
            exigeMotivo
              ? "Explica al autor qué debe ajustar."
              : "Agrega una nota para el historial."
          }
          className="border-border text-ink focus:border-navy-700 w-full rounded-lg border px-3 py-2 text-sm font-normal normal-case outline-none"
        />
      </label>
      <p className="text-ink text-sm">{decision.pregunta}</p>
      {resultado.error && (
        <p role="alert" className="text-sm font-medium text-red-600">
          {resultado.error}
        </p>
      )}
      <div className="flex justify-end gap-2.5">
        <Button type="button" variant="outline" onClick={onClose}>
          Cancelar
        </Button>
        <Button type="submit" disabled={pendiente}>
          {pendiente ? "Procesando…" : "Confirmar"}
        </Button>
      </div>
    </form>
  );
}

export function RevisionList({
  pendientes,
  usuariosPorId,
  historial,
  fuentes,
}: {
  pendientes: Articulo[];
  usuariosPorId: Record<string, Usuario | undefined>;
  historial: Record<string, Revision[]>;
  fuentes: Record<string, Fuente[]>;
}) {
  const [seleccion, setSeleccion] = useState<Seleccion | null>(null);
  const cerrar = () => setSeleccion(null);

  return (
    <>
      <div className="flex flex-col gap-4">
        {pendientes.length === 0 && (
          <p className="border-border text-ink/50 rounded-medium border bg-white px-6 py-8 text-center text-sm">
            No hay artículos esperando revisión.
          </p>
        )}
        {pendientes.map((articulo) => {
          const autor = usuariosPorId[articulo.autorId];
          const opciones =
            DECISIONES[articulo.estado as "en_revision" | "aprobado"];
          if (!opciones) return null;
          return (
            <div
              key={articulo.id}
              className="border-border shadow-nav rounded-medium hover:shadow-box flex flex-col items-start gap-4 border bg-white px-6 py-5 transition-shadow sm:flex-row sm:items-center sm:justify-between"
            >
              <div>
                <div className="flex flex-wrap items-center gap-2.5">
                  <p className="text-ink font-heading text-base font-bold">
                    {articulo.titulo}
                  </p>
                  <EstadoBadge estado={articulo.estado} />
                </div>
                <p className="text-ink/55 mt-1 text-sm">
                  Por {autor?.nombre ?? "—"} · {articulo.actualizadoEn}
                </p>
              </div>
              <div className="flex w-full gap-2.5 sm:w-auto sm:shrink-0">
                <Button
                  variant="outline"
                  className="flex-1 sm:flex-none"
                  onClick={() =>
                    setSeleccion({ articulo, decision: opciones.secundaria })
                  }
                >
                  {opciones.secundaria.boton}
                </Button>
                <Button
                  className="flex-1 sm:flex-none"
                  onClick={() =>
                    setSeleccion({ articulo, decision: opciones.principal })
                  }
                >
                  {opciones.principal.boton}
                </Button>
              </div>
            </div>
          );
        })}
      </div>

      {seleccion && (
        <Modal title={seleccion.decision.boton} onClose={cerrar} ancho="lg">
          <ConfirmarDecision
            key={`${seleccion.articulo.id}-${seleccion.decision.hacia}`}
            seleccion={seleccion}
            autor={usuariosPorId[seleccion.articulo.autorId]}
            revisiones={historial[seleccion.articulo.id] ?? []}
            fuentes={fuentes[seleccion.articulo.id] ?? []}
            onClose={cerrar}
          />
        </Modal>
      )}
    </>
  );
}
