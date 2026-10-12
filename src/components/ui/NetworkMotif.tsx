// Motivo decorativo de "nodos conectados" (la idea detrás del nombre del
// proyecto). Las líneas se dibujan de a poco, los nodos aparecen al
// conectarse y luego "respiran". Solo es ornamento: oculto a lectores de
// pantalla. Las animaciones viven en globals.css (.nodo-*) y se desactivan
// con prefers-reduced-motion.

// Cada trazo se dibuja completo antes de que empiece el siguiente.
const TRAZOS = [
  "M70 120 L210 60 L350 150 L520 90 L640 210",
  "M210 60 L190 240 L350 150",
  "M190 240 L360 330 L520 90",
  "M360 330 L560 400 L640 210",
  "M190 240 L90 380 L250 470 L360 330",
  "M250 470 L470 500 L560 400",
];

const TRAZO_DURACION = 1.1; // s
const TRAZO_PASO = 0.7; // s entre el inicio de un trazo y el siguiente

// Cada nodo aparece cuando el primer trazo que lo toca llega hasta él.
const NODOS = [
  { x: 70, y: 120, r: 5, t: 0.05 },
  { x: 210, y: 60, r: 7, t: 0.3 },
  { x: 350, y: 150, r: 5, t: 0.55 },
  { x: 520, y: 90, r: 8, t: 0.85 },
  { x: 640, y: 210, r: 5, t: 1.1 },
  { x: 190, y: 240, r: 9, t: 1.45 },
  { x: 360, y: 330, r: 7, t: 2.25 },
  { x: 560, y: 400, r: 6, t: 3.0 },
  { x: 90, y: 380, r: 5, t: 3.45 },
  { x: 250, y: 470, r: 7, t: 3.85 },
  { x: 470, y: 500, r: 5, t: 4.3 },
];

const HALOS = [
  { x: 190, y: 240, r: 14 },
  { x: 520, y: 90, r: 16 },
  { x: 360, y: 330, r: 12 },
];

// Instante en que termina de dibujarse la red completa.
const FIN = (TRAZOS.length - 1) * TRAZO_PASO + TRAZO_DURACION;

export function NetworkMotif({ className = "" }: { className?: string }) {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 700 560"
      className={className}
      preserveAspectRatio="xMaxYMid slice"
    >
      <g stroke="#b1e346" strokeOpacity="0.45" strokeWidth="1.2" fill="none">
        {TRAZOS.map((d, i) => (
          <path
            key={d}
            d={d}
            pathLength={1}
            className="nodo-linea"
            style={{ animationDelay: `${i * TRAZO_PASO}s` }}
          />
        ))}
      </g>

      <g fill="#b1e346">
        {HALOS.map((halo, i) => (
          <circle
            key={`${halo.x}-${halo.y}`}
            cx={halo.x}
            cy={halo.y}
            r={halo.r}
            className="nodo-halo"
            style={{ animationDelay: `${FIN + i * 0.9}s` }}
          />
        ))}
      </g>

      <g fill="#ffffff">
        {NODOS.map((nodo) => (
          <circle
            key={`${nodo.x}-${nodo.y}`}
            cx={nodo.x}
            cy={nodo.y}
            r={nodo.r}
            className="nodo-punto"
            style={{ animationDelay: `${nodo.t}s` }}
          />
        ))}
      </g>

      {/* Pulso de luz que recorre la red una vez conectada. */}
      <circle r="4" fill="#b1e346" opacity="0" className="nodo-viajero">
        <animate
          attributeName="opacity"
          values="0;1;1;0"
          keyTimes="0;0.1;0.9;1"
          dur="5s"
          begin={`${FIN}s`}
          repeatCount="indefinite"
        />
        <animateMotion
          dur="5s"
          begin={`${FIN}s`}
          repeatCount="indefinite"
          path="M70 120 L210 60 L350 150 L520 90 L640 210 L560 400 L470 500 L250 470 L360 330 L190 240 L210 60"
        />
      </circle>
    </svg>
  );
}
