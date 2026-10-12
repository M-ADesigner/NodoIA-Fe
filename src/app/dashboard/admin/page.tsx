import { UsersTable } from "@/components/content/UsersTable";
import { requerirPermiso } from "@/lib/auth/session";
import { listarArticulos, listarRoles, listarUsuarios } from "@/lib/db/queries";

export const dynamic = "force-dynamic";

const ICONOS = {
  publicados:
    "M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9zM14 3v6h6",
  revision: "M12 8v4l3 2M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0z",
  usuarios:
    "M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z",
};

export default async function AdminPage() {
  const actual = await requerirPermiso("gestionar_usuarios");
  const [articulos, usuarios, roles] = await Promise.all([
    listarArticulos(),
    listarUsuarios(),
    listarRoles(),
  ]);

  const stats = [
    {
      label: "Artículos publicados",
      valor: articulos.filter((a) => a.estado === "publicado").length,
      icono: ICONOS.publicados,
      tono: "bg-lime-soft text-lime-ink",
    },
    {
      label: "En cola de revisión",
      valor: articulos.filter((a) => a.estado === "en_revision").length,
      icono: ICONOS.revision,
      tono: "bg-navy-100 text-navy-700",
    },
    {
      label: "Usuarios activos",
      valor: usuarios.filter((u) => u.activo).length,
      icono: ICONOS.usuarios,
      tono: "bg-[#e1e8f2] text-navy-900",
    },
  ];

  const articulosPorUsuario: Record<string, number> = {};
  for (const articulo of articulos) {
    articulosPorUsuario[articulo.autorId] =
      (articulosPorUsuario[articulo.autorId] ?? 0) + 1;
  }

  return (
    <div className="flex flex-col gap-7">
      <div className="flex flex-col gap-2">
        <p className="text-navy-700 text-[13px] font-bold tracking-[0.12em] uppercase">
          Administración
        </p>
        <h1 className="text-navy-800 text-3xl font-extrabold sm:text-[34px]">
          Panel de control
        </h1>
        <p className="max-w-xl text-base leading-relaxed text-slate-600">
          Gestiona usuarios y roles, y revisa el avance del contenido.
        </p>
      </div>

      <ul className="grid grid-cols-[repeat(auto-fit,minmax(220px,1fr))] gap-5">
        {stats.map((stat) => (
          <li
            key={stat.label}
            className="border-border shadow-nav flex items-center gap-[18px] rounded-[20px] border bg-white p-6"
          >
            <span
              className={`inline-flex h-[52px] w-[52px] shrink-0 items-center justify-center rounded-2xl ${stat.tono}`}
            >
              <svg
                aria-hidden="true"
                width="24"
                height="24"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.9"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d={stat.icono} />
              </svg>
            </span>
            <div>
              <div className="font-heading text-navy-800 text-[34px] leading-none font-extrabold">
                {stat.valor}
              </div>
              <div className="mt-1.5 text-sm text-slate-600">{stat.label}</div>
            </div>
          </li>
        ))}
      </ul>

      <section className="flex flex-col gap-4">
        <h2 className="font-heading text-navy-800 text-xl font-bold sm:text-2xl">
          Usuarios
        </h2>
        <UsersTable
          usuarios={usuarios}
          articulosPorUsuario={articulosPorUsuario}
          roles={roles}
          usuarioActualId={actual.id}
        />
      </section>
    </div>
  );
}
