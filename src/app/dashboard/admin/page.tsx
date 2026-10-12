import { UsersTable } from "@/components/content/UsersTable";
import { listarArticulos, listarUsuarios } from "@/lib/db/queries";

export const dynamic = "force-dynamic";

export default async function AdminPage() {
  const [articulos, usuarios] = await Promise.all([
    listarArticulos(),
    listarUsuarios(),
  ]);

  const stats = [
    {
      label: "Artículos publicados",
      valor: articulos.filter((a) => a.estado === "publicado").length,
    },
    {
      label: "En cola de revisión",
      valor: articulos.filter((a) => a.estado === "en_revision").length,
    },
    { label: "Usuarios activos", valor: usuarios.length },
  ];

  const articulosPorUsuario: Record<string, number> = {};
  for (const articulo of articulos) {
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
          usuarios={usuarios}
          articulosPorUsuario={articulosPorUsuario}
        />
      </section>
    </div>
  );
}
