const ZONA = "America/Santiago";

// Fecha completa y estable para el sitio público ("1 de noviembre de 2026").
export function fechaLarga(iso: string): string {
  return new Date(iso).toLocaleDateString("es-CL", {
    day: "numeric",
    month: "long",
    year: "numeric",
    timeZone: ZONA,
  });
}

// Mismo día calendario en Chile: evita "Actualizado" cuando solo se corrigió
// algo minutos después de publicar.
export function mismoDia(a: string, b: string): boolean {
  return fechaLarga(a) === fechaLarga(b);
}

// ~200 palabras por minuto; nunca menos de 1.
export function minutosDeLectura(...textos: string[]): number {
  const palabras = textos.join(" ").split(/\s+/).filter(Boolean).length;
  return Math.max(1, Math.round(palabras / 200));
}
