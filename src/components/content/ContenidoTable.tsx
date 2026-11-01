"use client";

import { useActionState, useEffect, useId, useRef, useState } from "react";
import {
  eliminarArticulo,
  guardarArticulo,
  type EstadoForm,
} from "@/app/dashboard/contenido/actions";
import { Button } from "@/components/ui/Button";
import { Modal } from "@/components/ui/Modal";
import { Field } from "@/components/ui/Field";
import { EstadoBadge } from "@/components/content/EstadoBadge";
import { EditorFuentes } from "@/components/content/EditorFuentes";
import { HistorialRevisiones } from "@/components/content/HistorialRevisiones";
import { ListaFuentes } from "@/components/content/ListaFuentes";
import { CATEGORIAS } from "@/lib/content/categorias";
import type {
  Articulo,
  EstadoContenido,
  Fuente,
  Revision,
} from "@/types/content";
import type { Usuario } from "@/types/user";

// Modo del modal: null = cerrado, "nuevo" o el artículo que se abre.
type Modo = null | "nuevo" | Articulo;

const inicial: EstadoForm = {};

// Solo para rellenar rápido el formulario al probar; no aparece en producción.
const DATOS_PRUEBA = [
  {
    titulo: "Prueba: IA en las pymes chilenas",
    categoria: "Artículo",
    resumen: "Cómo las pequeñas empresas chilenas empiezan a adoptar IA.",
    contenido:
      "Primer párrafo de prueba sobre adopción de IA.\n\nSegundo párrafo con más detalle.\n\nTercer párrafo de cierre.",
  },
  {
    titulo: "Prueba: Boletín mensual de la cámara",
    categoria: "Boletín",
    resumen: "Resumen de las novedades y actividades del mes en la CCHIA.",
    contenido:
      "Novedades del mes.\n\nPróximas actividades y eventos para socios.",
  },
  {
    titulo: "Prueba: Nueva alianza institucional",
    categoria: "Noticia institucional",
    resumen: "La cámara anuncia una nueva alianza con una universidad.",
    contenido:
      "La CCHIA firmó un convenio de colaboración.\n\nEl acuerdo busca impulsar la formación en IA.",
  },
];

type Pestana = {
  clave: string;
  label: string;
  estados: EstadoContenido[] | null;
};

// Pestañas del listado; "Borradores" agrupa también lo asistido por IA.
const PESTANAS: Pestana[] = [
  { clave: "todos", label: "Todos", estados: null },
  {
    clave: "borrador",
    label: "Borradores",
    estados: ["borrador", "asistido_ia"],
  },
  { clave: "en_revision", label: "En revisión", estados: ["en_revision"] },
  { clave: "aprobado", label: "Aprobados", estados: ["aprobado"] },
  { clave: "publicado", label: "Publicados", estados: ["publicado"] },
];

// Si el último movimiento fue una devolución con motivo, es lo que el autor
// debe corregir; si ya lo reenvió, deja de mostrarse.
function motivoDevolucion(
  articulo: Articulo,
  revisiones: Revision[] | undefined,
) {
  const ultima = revisiones?.[0];
  if (articulo.estado !== "borrador" || !ultima?.comentario) return null;
  return ultima.estadoNuevo === "borrador" ? ultima : null;
}

function AvisoDevolucion({ revision }: { revision: Revision }) {
  return (
    <div
      role="note"
      className="flex items-baseline gap-2 rounded-[10px] bg-red-50 px-3 py-2 text-[13px] leading-snug text-red-950"
    >
      <svg
        aria-hidden="true"
        width="14"
        height="14"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2.4"
        strokeLinecap="round"
        strokeLinejoin="round"
        className="shrink-0 self-center text-red-700"
      >
        <path d="M9 14 4 9l5-5" />
        <path d="M4 9h10a6 6 0 0 1 0 12h-3" />
      </svg>
      <p className="whitespace-pre-line">
        <b className="font-semibold text-red-700">
          {revision.usuarioNombre} lo devolvió
        </b>{" "}
        · {revision.creadoEn}: {revision.comentario}
      </p>
    </div>
  );
}

const esEditable = (articulo: Articulo) =>
  articulo.estado === "borrador" || articulo.estado === "asistido_ia";

function ChipDevuelto({ motivo }: { motivo?: string }) {
  return (
    <span
      title={motivo}
      className="inline-flex items-center gap-1 rounded-full bg-red-50 px-2 py-[3px] text-xs font-semibold text-red-700"
    >
      <svg
        aria-hidden="true"
        width="12"
        height="12"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2.4"
        strokeLinecap="round"
        strokeLinejoin="round"
      >
        <path d="M9 14 4 9l5-5" />
        <path d="M4 9h10a6 6 0 0 1 0 12h-3" />
      </svg>
      Devuelto
    </span>
  );
}

function EliminarArticulo({
  articulo,
  onClose,
}: {
  articulo: Articulo;
  onClose: () => void;
}) {
  const [resultado, eliminar, eliminando] = useActionState(
    eliminarArticulo,
    inicial,
  );

  useEffect(() => {
    if (resultado.ok) onClose();
  }, [resultado.ok, onClose]);

  return (
    <form
      action={eliminar}
      onSubmit={(event) => {
        const aviso =
          articulo.estado === "publicado"
            ? "Es permanente y lo quita del sitio público."
            : "Es permanente.";
        if (!window.confirm(`¿Eliminar «${articulo.titulo}»? ${aviso}`)) {
          event.preventDefault();
        }
      }}
      className="flex flex-col items-start gap-1"
    >
      <input type="hidden" name="id" value={articulo.id} />
      <button
        type="submit"
        disabled={eliminando}
        className="min-h-11 px-2 text-sm font-semibold text-red-700 hover:underline disabled:cursor-not-allowed disabled:opacity-40"
      >
        {eliminando ? "Eliminando…" : "Eliminar"}
      </button>
      {resultado.error && (
        <p role="alert" className="text-sm font-medium text-red-700">
          {resultado.error}
        </p>
      )}
    </form>
  );
}

const LABEL_CLASS =
  "text-ink/80 flex flex-col gap-1.5 text-[13px] font-semibold";
const CAMPO_CLASS =
  "border-border text-ink min-h-11 w-full rounded-xl border px-3.5 py-2.5 text-[15px] font-normal outline-none focus:border-navy-700";

function FormularioArticulo({
  articulo,
  autorNombre,
  revisiones,
  fuentes,
  catalogo,
  puedeEliminar,
  onClose,
}: {
  articulo?: Articulo;
  autorNombre?: string;
  revisiones: Revision[];
  fuentes: Fuente[];
  catalogo: Fuente[];
  puedeEliminar: boolean;
  onClose: () => void;
}) {
  const [guardado, guardar, guardando] = useActionState(
    guardarArticulo,
    inicial,
  );
  const formRef = useRef<HTMLFormElement>(null);
  const formId = useId();
  const [resumenLen, setResumenLen] = useState(articulo?.resumen.length ?? 0);
  const devolucion = articulo ? motivoDevolucion(articulo, revisiones) : null;

  function rellenarPrueba() {
    const datos = DATOS_PRUEBA[Math.floor(Math.random() * DATOS_PRUEBA.length)];
    const campos = formRef.current?.elements;
    if (!campos) return;
    for (const [nombre, valor] of Object.entries(datos)) {
      (campos.namedItem(nombre) as HTMLInputElement | null)!.value = valor;
    }
    setResumenLen(datos.resumen.length);
  }

  useEffect(() => {
    if (guardado.ok) onClose();
  }, [guardado.ok, onClose]);

  return (
    <>
      <header className="border-border flex shrink-0 items-start justify-between gap-4 border-b px-5 pt-6 pb-4 sm:px-7">
        <div className="flex min-w-0 flex-col gap-2">
          <div className="flex flex-wrap items-center gap-2.5">
            <h2 className="font-heading text-ink text-xl leading-tight font-bold">
              {articulo ? articulo.titulo : "Nuevo artículo"}
            </h2>
            {articulo && <EstadoBadge estado={articulo.estado} />}
            {devolucion && <ChipDevuelto motivo={devolucion.comentario} />}
          </div>
          {articulo && (
            <p className="text-ink/60 text-[13px]">
              {autorNombre ? `Autor: ${autorNombre} · ` : ""}Actualizado{" "}
              {articulo.actualizadoEn.toLowerCase()}
            </p>
          )}
        </div>
        <button
          type="button"
          onClick={onClose}
          aria-label="Cerrar"
          className="text-ink/50 hover:text-ink -mt-1 -mr-2 flex h-11 w-11 shrink-0 items-center justify-center text-2xl leading-none"
        >
          ×
        </button>
      </header>

      <div className="flex min-h-0 flex-1 flex-col gap-5 overflow-y-auto px-5 py-5 sm:px-7">
        {devolucion && <AvisoDevolucion revision={devolucion} />}

        <form
          id={formId}
          ref={formRef}
          action={guardar}
          className="grid gap-x-7 gap-y-5 lg:grid-cols-[minmax(0,1fr)_240px] lg:grid-rows-[auto_auto_1fr]"
        >
          {articulo && <input type="hidden" name="id" value={articulo.id} />}

          <label className={`${LABEL_CLASS} lg:col-start-1`}>
            Título
            <input
              name="titulo"
              defaultValue={articulo?.titulo}
              required
              minLength={5}
              maxLength={160}
              className={CAMPO_CLASS}
            />
          </label>

          <label className={`${LABEL_CLASS} lg:col-start-2 lg:row-start-1`}>
            Categoría
            <select
              name="categoria"
              defaultValue={articulo?.categoria ?? CATEGORIAS[0]}
              className={CAMPO_CLASS}
            >
              {CATEGORIAS.map((categoria) => (
                <option key={categoria} value={categoria}>
                  {categoria}
                </option>
              ))}
            </select>
          </label>

          <label className={`${LABEL_CLASS} lg:col-start-1`}>
            <span className="flex items-baseline justify-between gap-3">
              Resumen
              <span className="text-ink/60 text-xs font-normal">
                {resumenLen} / 400
              </span>
            </span>
            <textarea
              name="resumen"
              defaultValue={articulo?.resumen}
              required
              minLength={10}
              maxLength={400}
              rows={2}
              onChange={(event) => setResumenLen(event.target.value.length)}
              className={`${CAMPO_CLASS} resize-none leading-snug`}
            />
          </label>

          <label className={`${LABEL_CLASS} lg:col-start-1`}>
            <span className="flex items-baseline justify-between gap-3">
              Contenido
              <span className="text-ink/60 text-xs font-normal">
                Separa los párrafos con una línea en blanco
              </span>
            </span>
            <textarea
              name="contenido"
              defaultValue={articulo?.contenido}
              maxLength={50000}
              rows={9}
              className={`${CAMPO_CLASS} min-h-56 resize-none leading-relaxed lg:h-full`}
            />
          </label>

          <div className="lg:col-start-1">
            <EditorFuentes iniciales={fuentes} catalogo={catalogo} />
          </div>

          {revisiones.length > 0 && (
            <HistorialRevisiones
              revisiones={revisiones}
              abierto
              className="bg-neutral-soft/60 rounded-2xl p-3.5 lg:col-start-2 lg:row-span-2 lg:row-start-2 lg:self-start"
            />
          )}
        </form>
      </div>

      <footer className="border-border flex shrink-0 flex-col gap-2 border-t px-5 py-4 sm:px-7">
        {guardado.error && (
          <p role="alert" className="text-sm font-medium text-red-700">
            {guardado.error}
          </p>
        )}
        <div className="flex flex-col-reverse gap-2 sm:flex-row sm:items-center">
          <div className="flex flex-col-reverse gap-2 sm:mr-auto sm:flex-row sm:items-center">
            {articulo && puedeEliminar && (
              <EliminarArticulo articulo={articulo} onClose={onClose} />
            )}
            {!articulo && process.env.NODE_ENV !== "production" && (
              <button
                type="button"
                onClick={rellenarPrueba}
                className="text-navy-700 min-h-11 px-2 text-sm font-semibold hover:underline"
              >
                Rellenar con datos de prueba
              </button>
            )}
          </div>
          <Button
            type="button"
            variant="outline"
            className="border-transparent"
            onClick={onClose}
          >
            Cancelar
          </Button>
          <Button
            type="submit"
            form={formId}
            name="accion"
            value="guardar"
            variant="outline"
            disabled={guardando}
          >
            Guardar borrador
          </Button>
          <Button
            type="submit"
            form={formId}
            name="accion"
            value="enviar"
            disabled={guardando}
          >
            {guardando ? "Guardando…" : "Guardar y enviar a revisión"}
          </Button>
        </div>
      </footer>
    </>
  );
}

export function ContenidoTable({
  articulos,
  usuariosPorId,
  historial,
  fuentes,
  catalogo,
  puedeEliminar,
}: {
  articulos: Articulo[];
  usuariosPorId: Record<string, Usuario | undefined>;
  historial: Record<string, Revision[]>;
  fuentes: Record<string, Fuente[]>;
  catalogo: Fuente[];
  puedeEliminar: boolean;
}) {
  const [modo, setModo] = useState<Modo>(null);
  const [pestana, setPestana] = useState("todos");
  const activa = PESTANAS.find((p) => p.clave === pestana) ?? PESTANAS[0];
  const visibles = activa.estados
    ? articulos.filter((a) => activa.estados!.includes(a.estado))
    : articulos;
  const cerrar = () => setModo(null);

  return (
    <>
      <div className="flex flex-col-reverse gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div
          role="tablist"
          aria-label="Filtrar por estado"
          className="flex flex-wrap gap-2"
        >
          {PESTANAS.map((tab) => {
            const cantidad = tab.estados
              ? articulos.filter((a) => tab.estados!.includes(a.estado)).length
              : articulos.length;
            const seleccionada = tab.clave === activa.clave;
            return (
              <button
                key={tab.clave}
                type="button"
                role="tab"
                aria-selected={seleccionada}
                onClick={() => setPestana(tab.clave)}
                className={`rounded-full px-4 py-2 text-sm font-semibold transition ${
                  seleccionada
                    ? "bg-navy-900 text-white"
                    : "border-border text-ink/70 hover:border-navy-700 border bg-white"
                }`}
              >
                {tab.label}{" "}
                <span
                  className={seleccionada ? "text-white/70" : "text-ink/40"}
                >
                  {cantidad}
                </span>
              </button>
            );
          })}
        </div>
        <Button type="button" onClick={() => setModo("nuevo")}>
          + Nuevo artículo
        </Button>
      </div>

      <div className="border-border shadow-nav rounded-medium overflow-hidden border bg-white">
        <div className="overflow-x-auto">
          <table className="w-full min-w-[640px] text-sm">
            <thead>
              <tr className="border-border text-ink/45 border-b text-xs font-bold tracking-widest uppercase">
                <th className="px-6 py-4 text-left">Artículo</th>
                <th className="px-6 py-4 text-left">Estado</th>
                <th className="px-6 py-4 text-left">Actualizado</th>
                <th className="px-6 py-4"></th>
              </tr>
            </thead>
            <tbody>
              {visibles.length === 0 && (
                <tr>
                  <td colSpan={4} className="text-ink/50 px-6 py-8 text-center">
                    {articulos.length === 0
                      ? "Aún no hay artículos. Crea el primero con «Nuevo artículo»."
                      : "No hay artículos en este estado."}
                  </td>
                </tr>
              )}
              {visibles.map((articulo) => (
                <tr
                  key={articulo.id}
                  className="border-neutral-soft hover:bg-row-hover border-b last:border-0"
                >
                  <td className="px-6 py-4">
                    <p className="text-ink font-semibold">{articulo.titulo}</p>
                    <p className="text-ink/50 text-xs">{articulo.categoria}</p>
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex flex-wrap items-center gap-2">
                      <EstadoBadge estado={articulo.estado} />
                      {motivoDevolucion(articulo, historial[articulo.id]) && (
                        <ChipDevuelto
                          motivo={
                            motivoDevolucion(articulo, historial[articulo.id])!
                              .comentario
                          }
                        />
                      )}
                    </div>
                  </td>
                  <td className="text-ink/60 px-6 py-4">
                    {articulo.actualizadoEn}
                  </td>
                  <td className="px-6 py-4">
                    <button
                      type="button"
                      onClick={() => setModo(articulo)}
                      className="text-navy-700 font-semibold hover:underline"
                    >
                      {esEditable(articulo) ? "Editar" : "Ver"}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {modo && (
        <Modal
          title={
            modo === "nuevo"
              ? "Nuevo artículo"
              : esEditable(modo)
                ? `Editar: ${modo.titulo}`
                : modo.titulo
          }
          onClose={cerrar}
          ancho={modo === "nuevo" || esEditable(modo) ? "xl" : "lg"}
          libre={modo === "nuevo" || esEditable(modo)}
        >
          {modo === "nuevo" || esEditable(modo) ? (
            <FormularioArticulo
              // La key reinicia el formulario al cambiar de artículo.
              key={modo === "nuevo" ? "nuevo" : modo.id}
              articulo={modo === "nuevo" ? undefined : modo}
              autorNombre={
                modo === "nuevo"
                  ? undefined
                  : usuariosPorId[modo.autorId]?.nombre
              }
              revisiones={modo === "nuevo" ? [] : (historial[modo.id] ?? [])}
              fuentes={modo === "nuevo" ? [] : (fuentes[modo.id] ?? [])}
              catalogo={catalogo}
              puedeEliminar={puedeEliminar}
              onClose={cerrar}
            />
          ) : (
            <dl className="grid grid-cols-2 gap-3.5 text-sm">
              <Field label="Estado">
                <EstadoBadge estado={modo.estado} />
              </Field>
              <Field label="Categoría">{modo.categoria}</Field>
              <Field label="Autor">
                {usuariosPorId[modo.autorId]?.nombre ?? "—"}
              </Field>
              <Field label="Correo del autor">
                {usuariosPorId[modo.autorId]?.correo ?? "—"}
              </Field>
              <Field label="Actualizado">{modo.actualizadoEn}</Field>
              <Field label="Publicado">
                {modo.publicadoEn ?? "Aún no publicado"}
              </Field>
              <div className="border-border col-span-2 flex flex-col gap-1 border-t pt-3">
                <dt className="text-ink/50 text-xs font-medium tracking-wide uppercase">
                  Resumen
                </dt>
                <dd className="text-ink leading-relaxed">{modo.resumen}</dd>
              </div>
              {(fuentes[modo.id]?.length ?? 0) > 0 && (
                <div className="border-border col-span-2 flex flex-col gap-1.5 border-t pt-3">
                  <dt className="text-ink/50 text-xs font-medium tracking-wide uppercase">
                    Fuentes
                  </dt>
                  <dd>
                    <ListaFuentes fuentes={fuentes[modo.id]} />
                  </dd>
                </div>
              )}
              <p className="text-ink/50 col-span-2 text-xs">
                Este artículo ya está en el flujo de revisión y no se puede
                editar.
              </p>
              <div className="col-span-2">
                <HistorialRevisiones revisiones={historial[modo.id] ?? []} />
              </div>
              {puedeEliminar && (
                <div className="col-span-2">
                  <EliminarArticulo articulo={modo} onClose={cerrar} />
                </div>
              )}
            </dl>
          )}
        </Modal>
      )}
    </>
  );
}
