import { UsersTable } from "@/components/content/UsersTable";
import {
  articulosEnRevision,
  articulosMock,
  articulosPublicados,
} from "@/lib/content/mock-articulos";
import { usuariosMock } from "@/lib/auth/mock-usuarios";

export default function AdminPage() {
  const stats = [
    { label: "Artículos publicados", valor: articulosPublicados().length },
    { label: "En cola de revisión", valor: articulosEnRevision().length },
    { label: "Usuarios activos", valor: usuariosMock.length },
  ];

  const articulosPorUsuario: Record<string, number> = {};
  for (const articulo of articulosMock) {
    articulosPorUsuario[articulo.autorId] =
      (articulosPorUsuario[articulo.autorId] ?? 0) + 1;
  }

  return (
    <div className="flex flex-col gap-8">
      <div className="flex flex-col gap-2">
        <p className="text-navy-700 text-sm font-bold tracking-widest uppercase">
          Administración
        </p>
        <h1 className="font-heading text-ink text-3xl font-bold sm:text-4xl">
          Panel de control
        </h1>
        <p className="text-ink/60 max-w-xl text-base">
          Gestiona usuarios, revisa el avance del contenido y mantén al día la
          plataforma.
        </p>
      </div>

      <div className="grid grid-cols-1 gap-5 sm:grid-cols-3">
        {stats.map((stat) => (
          <div
            key={stat.label}
            className="bg-cchia-blue shadow-nav flex flex-col gap-2 rounded-medium p-7"
          >
            <span className="font-heading text-white text-4xl font-extrabold">
              {stat.valor}
            </span>
            <span className="text-sm font-medium text-white/80">
              {stat.label}
            </span>
          </div>
        ))}
      </div>

      <section className="flex flex-col gap-4">
        <h2 className="font-heading text-ink text-xl font-bold sm:text-2xl">
          Usuarios
        </h2>
        <UsersTable
          usuarios={usuariosMock}
          articulosPorUsuario={articulosPorUsuario}
        />
      </section>
    </div>
  );
}
