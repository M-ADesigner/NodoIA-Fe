import { RevisionList } from "@/components/content/RevisionList";
import { articulosEnRevision } from "@/lib/content/mock-articulos";
import { usuariosMock } from "@/lib/auth/mock-usuarios";

export default function RevisionPage() {
  const pendientes = articulosEnRevision();
  const usuariosPorId = Object.fromEntries(
    usuariosMock.map((usuario) => [usuario.id, usuario]),
  );

  return (
    <div className="flex flex-col gap-8">
      <div className="flex flex-col gap-2">
        <p className="text-navy-700 text-sm font-bold tracking-widest uppercase">
          Revisión
        </p>
        <div className="flex flex-wrap items-baseline justify-between gap-2">
          <h1 className="font-heading text-ink text-3xl font-bold sm:text-4xl">
            Cola de revisión
          </h1>
          <span className="bg-primary/20 text-[#4A5F0E] rounded-full px-3 py-1 text-sm font-bold">
            {pendientes.length} pendientes
          </span>
        </div>
      </div>

      <RevisionList pendientes={pendientes} usuariosPorId={usuariosPorId} />
    </div>
  );
}
