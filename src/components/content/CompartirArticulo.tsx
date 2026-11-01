"use client";

import { useState } from "react";

const BOTON =
  "border-border text-navy-700 hover:border-navy-700 inline-flex min-h-11 items-center rounded-full border bg-white px-4 text-sm font-semibold transition";

// `url` es la dirección pública canónica del artículo (se arma en el servidor).
export function CompartirArticulo({
  url,
  titulo,
}: {
  url: string;
  titulo: string;
}) {
  const [copiado, setCopiado] = useState(false);
  const enlaceUrl = encodeURIComponent(url);

  async function copiar() {
    try {
      await navigator.clipboard.writeText(url);
      setCopiado(true);
      setTimeout(() => setCopiado(false), 2000);
    } catch {
      // Sin permiso del portapapeles: los demás botones siguen sirviendo.
    }
  }

  return (
    <div className="flex flex-wrap items-center gap-2.5">
      <span className="text-sm font-semibold text-slate-600">Compartir</span>
      <a
        href={`https://www.linkedin.com/sharing/share-offsite/?url=${enlaceUrl}`}
        target="_blank"
        rel="noopener noreferrer"
        className={BOTON}
      >
        LinkedIn
      </a>
      <a
        href={`https://wa.me/?text=${encodeURIComponent(`${titulo} ${url}`)}`}
        target="_blank"
        rel="noopener noreferrer"
        className={BOTON}
      >
        WhatsApp
      </a>
      <button type="button" onClick={copiar} className={BOTON}>
        {copiado ? "¡Enlace copiado!" : "Copiar enlace"}
      </button>
      <span role="status" className="sr-only">
        {copiado ? "Enlace copiado al portapapeles" : ""}
      </span>
    </div>
  );
}
