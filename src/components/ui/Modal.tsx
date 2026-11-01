"use client";

import { useEffect } from "react";
import type { ReactNode } from "react";

// `libre`: solo la carcasa (sin cabecera ni relleno propios); el contenido
// arma su cabecera, cuerpo con scroll y pie fijos. Ver FormularioArticulo.
export function Modal({
  title,
  onClose,
  ancho = "md",
  libre = false,
  children,
}: {
  title: string;
  onClose: () => void;
  ancho?: "md" | "lg" | "xl";
  libre?: boolean;
  children: ReactNode;
}) {
  useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") onClose();
    }
    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
  }, [onClose]);

  const maxAncho = { md: "max-w-md", lg: "max-w-2xl", xl: "max-w-[960px]" }[
    ancho
  ];

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4"
      onClick={onClose}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-label={title}
        onClick={(event) => event.stopPropagation()}
        className={`shadow-box flex max-h-[90vh] w-full flex-col rounded-[28px] bg-white ${maxAncho} ${
          libre ? "overflow-hidden" : "gap-4 overflow-y-auto p-7"
        }`}
      >
        {!libre && (
          <div className="flex items-start justify-between gap-4">
            <h2 className="font-heading text-ink text-lg font-bold">{title}</h2>
            <button
              type="button"
              onClick={onClose}
              aria-label="Cerrar"
              className="text-ink/50 hover:text-ink text-xl leading-none"
            >
              ×
            </button>
          </div>
        )}
        {children}
      </div>
    </div>
  );
}
