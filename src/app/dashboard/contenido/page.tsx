import { Button } from "@/components/ui/Button";
import { ContenidoTable } from "@/components/content/ContenidoTable";
import { listarArticulos, listarUsuarios } from "@/lib/db/queries";

export const dynamic = "force-dynamic";

export default async function ContenidoPage() {
  const [articulos, usuarios] = await Promise.all([
    listarArticulos(),
    listarUsuarios(),
  ]);
  const usuariosPorId = Object.fromEntries(
    usuarios.map((usuario) => [usuario.id, usuario]),
  );

  return (
    <div className="flex flex-col gap-8">
      <div className="flex flex-col items-start gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-col gap-2">
          <p className="text-navy-700 text-sm font-bold tracking-widest uppercase">
            Contenido
          </p>
          <h1 className="font-heading text-ink text-3xl font-bold sm:text-4xl">
            Mi contenido
          </h1>
        </div>
        <Button>+ Nuevo artículo con IA</Button>
      </div>

      <div className="border-primary/30 bg-primary/10 flex flex-col items-start gap-4 rounded-medium border border-dashed px-6 py-5 shadow-nav sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p className="text-[#5C7A12] mb-1 text-xs font-bold tracking-widest uppercase">
            Sugerencia del asistente
          </p>
          <p className="text-ink text-base">
            Resumir el boletín de minería en 150 palabras para redes sociales.
          </p>
        </div>
        <Button variant="outline" className="shrink-0">
          Generar
        </Button>
      </div>

      <section className="flex flex-col gap-4">
        <h2 className="font-heading text-ink text-xl font-bold sm:text-2xl">
          Artículos
        </h2>
        <ContenidoTable articulos={articulos} usuariosPorId={usuariosPorId} />
      </section>
    </div>
  );
}
