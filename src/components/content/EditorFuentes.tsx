"use client";

import { useState } from "react";
import {
  MAX_FUENTES,
  normalizarUrl,
  TIPOS_FUENTE,
} from "@/lib/content/fuentes";
import type { Fuente, TipoFuente } from "@/types/content";

// Una fuente en edición: del catálogo (con `id`) o nueva (sin `id`).
type Item =
  | {
      id: string;
      titulo: string;
      url: string;
      tipo: TipoFuente;
      organismo?: string;
    }
  | { titulo: string; url: string; tipo: TipoFuente; organismo?: string };

const CAMPO =
  "border-border text-ink min-h-11 w-full rounded-xl border px-3.5 py-2.5 text-[15px] font-normal outline-none focus:border-navy-700";

function aItem(fuente: Fuente): Item {
  return { ...fuente };
}

// Va dentro del formulario del artículo y viaja como JSON en `fuentes`. Las
// del catálogo solo envían su `id`; las nuevas, sus datos.
export function EditorFuentes({
  iniciales,
  catalogo,
}: {
  iniciales: Fuente[];
  catalogo: Fuente[];
}) {
  const [items, setItems] = useState<Item[]>(() => iniciales.map(aItem));
  const [titulo, setTitulo] = useState("");
  const [url, setUrl] = useState("");
  const [tipo, setTipo] = useState<TipoFuente>(TIPOS_FUENTE[0]);
  const [organismo, setOrganismo] = useState("");
  const [aviso, setAviso] = useState("");

  const usadas = new Set(items.flatMap((i) => ("id" in i ? [i.id] : [])));
  const disponibles = catalogo.filter((f) => !usadas.has(f.id));
  const lleno = items.length >= MAX_FUENTES;

  function agregarExistente(id: string) {
    const fuente = catalogo.find((f) => f.id === id);
    if (fuente && !lleno) setItems((prev) => [...prev, aItem(fuente)]);
  }

  function agregarNueva() {
    const urlLimpia = normalizarUrl(url);
    if (titulo.trim().length < 3) {
      setAviso("Escribe un título de al menos 3 caracteres.");
      return;
    }
    if (!urlLimpia) {
      setAviso("La URL debe empezar con http:// o https://.");
      return;
    }
    // Misma URL que una ya agregada o del catálogo: se reutiliza esa.
    const existente = catalogo.find((f) => f.url === urlLimpia);
    if (items.some((i) => i.url === urlLimpia)) {
      setAviso("Esa fuente ya está en la lista.");
      return;
    }
    setAviso("");
    setItems((prev) => [
      ...prev,
      existente
        ? aItem(existente)
        : {
            titulo: titulo.trim(),
            url: urlLimpia,
            tipo,
            organismo: organismo.trim() || undefined,
          },
    ]);
    setTitulo("");
    setUrl("");
    setOrganismo("");
  }

  // Enter en estos campos agrega la fuente en vez de enviar el artículo.
  function alTeclear(event: React.KeyboardEvent) {
    if (event.key === "Enter") {
      event.preventDefault();
      agregarNueva();
    }
  }

  const envio = items.map((i) =>
    "id" in i
      ? { id: i.id }
      : {
          titulo: i.titulo,
          url: i.url,
          tipo: i.tipo,
          organismo: i.organismo,
        },
  );

  return (
    <fieldset className="border-border flex flex-col gap-3 rounded-2xl border p-4">
      <legend className="text-ink/80 px-1 text-[13px] font-semibold">
        Fuentes{" "}
        <span className="text-ink/60 font-normal">
          (opcional · {items.length}/{MAX_FUENTES})
        </span>
      </legend>
      <input type="hidden" name="fuentes" value={JSON.stringify(envio)} />

      {items.length > 0 && (
        <ul className="flex flex-col gap-2">
          {items.map((item, indice) => (
            <li
              key={item.url}
              className="bg-neutral-soft/60 flex items-start justify-between gap-3 rounded-xl px-3 py-2 text-sm"
            >
              <div className="min-w-0">
                <p className="text-ink font-semibold break-words">
                  {item.titulo}
                </p>
                <p className="text-ink/60 text-xs break-all">
                  {item.tipo}
                  {item.organismo ? ` · ${item.organismo}` : ""} · {item.url}
                </p>
              </div>
              <button
                type="button"
                onClick={() =>
                  setItems((prev) => prev.filter((_, i) => i !== indice))
                }
                aria-label={`Quitar la fuente ${item.titulo}`}
                className="min-h-11 shrink-0 px-2 text-sm font-semibold text-red-700 hover:underline"
              >
                Quitar
              </button>
            </li>
          ))}
        </ul>
      )}

      {disponibles.length > 0 && !lleno && (
        <label className="text-ink/80 flex flex-col gap-1.5 text-[13px] font-semibold">
          Reutilizar una fuente existente
          <select
            value=""
            onChange={(event) => agregarExistente(event.target.value)}
            className={CAMPO}
          >
            <option value="">Elegir del catálogo…</option>
            {disponibles.map((fuente) => (
              <option key={fuente.id} value={fuente.id}>
                {fuente.titulo} ({fuente.tipo})
              </option>
            ))}
          </select>
        </label>
      )}

      {!lleno && (
        <div className="flex flex-col gap-2">
          <p className="text-ink/80 text-[13px] font-semibold">
            Agregar una fuente nueva
          </p>
          <div className="grid gap-2 sm:grid-cols-2">
            <input
              value={titulo}
              onChange={(e) => setTitulo(e.target.value)}
              onKeyDown={alTeclear}
              placeholder="Título"
              maxLength={200}
              aria-label="Título de la fuente"
              className={CAMPO}
            />
            <input
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              onKeyDown={alTeclear}
              placeholder="https://…"
              inputMode="url"
              maxLength={500}
              aria-label="URL de la fuente"
              className={CAMPO}
            />
            <select
              value={tipo}
              onChange={(e) => setTipo(e.target.value as TipoFuente)}
              aria-label="Tipo de fuente"
              className={CAMPO}
            >
              {TIPOS_FUENTE.map((t) => (
                <option key={t} value={t}>
                  {t}
                </option>
              ))}
            </select>
            <input
              value={organismo}
              onChange={(e) => setOrganismo(e.target.value)}
              onKeyDown={alTeclear}
              placeholder="Organismo o autor (opcional)"
              maxLength={120}
              aria-label="Organismo o autor"
              className={CAMPO}
            />
          </div>
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={agregarNueva}
              className="border-border text-navy-700 hover:border-navy-700 min-h-11 rounded-full border px-4 text-sm font-semibold"
            >
              + Agregar fuente
            </button>
            {aviso && (
              <p role="alert" className="text-sm font-medium text-red-700">
                {aviso}
              </p>
            )}
          </div>
        </div>
      )}
    </fieldset>
  );
}
