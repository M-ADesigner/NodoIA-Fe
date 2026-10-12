"use client";

import { useState } from "react";
import { Modal } from "@/components/ui/Modal";
import { Field } from "@/components/ui/Field";
import type { Rol, Usuario } from "@/types/user";

const ROL_LABEL: Record<Rol, string> = {
  administrador: "Administrador",
  editor: "Editor",
  revisor: "Revisor",
};

const ROL_CLASS: Record<Rol, string> = {
  administrador: "bg-navy-900/10 text-navy-900",
  editor: "bg-primary/28 text-[#4A5F0E]",
  revisor: "bg-navy-700/12 text-navy-700",
};

function iniciales(nombre: string) {
  return nombre
    .split(" ")
    .map((parte) => parte[0])
    .slice(0, 2)
    .join("")
    .toUpperCase();
}

export function UsersTable({
  usuarios,
  articulosPorUsuario,
}: {
  usuarios: Usuario[];
  articulosPorUsuario: Record<string, number>;
}) {
  const [seleccionado, setSeleccionado] = useState<Usuario | null>(null);

  return (
    <>
      <div className="border-border shadow-nav overflow-hidden rounded-medium border bg-white">
        <div className="overflow-x-auto">
          <table className="w-full min-w-[420px] text-sm">
            <thead>
              <tr className="border-border text-ink/45 border-b text-xs font-bold tracking-widest uppercase">
                <th className="px-6 py-4 text-left">Usuario</th>
                <th className="px-6 py-4 text-left">Rol</th>
                <th className="px-6 py-4"></th>
              </tr>
            </thead>
            <tbody>
              {usuarios.map((usuario) => (
                <tr
                  key={usuario.id}
                  className="border-b border-[#ECEFE6] last:border-0 hover:bg-[#F7FAF0]"
                >
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-3">
                      <span className="bg-navy-100 text-navy-900 flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-xs font-bold">
                        {iniciales(usuario.nombre)}
                      </span>
                      <div>
                        <p className="text-ink font-semibold">{usuario.nombre}</p>
                        <p className="text-ink/45 text-xs">{usuario.correo}</p>
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <span
                      className={`inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold ${ROL_CLASS[usuario.rol]}`}
                    >
                      {ROL_LABEL[usuario.rol]}
                    </span>
                  </td>
                  <td className="px-6 py-4">
                    <button
                      type="button"
                      onClick={() => setSeleccionado(usuario)}
                      className="text-navy-700 font-semibold hover:underline"
                    >
                      Editar
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {seleccionado && (
        <Modal
          title={seleccionado.nombre}
          onClose={() => setSeleccionado(null)}
        >
          <dl className="grid grid-cols-2 gap-3.5 text-sm">
            <div className="col-span-2 flex flex-col gap-1">
              <dt className="text-ink/50 text-xs font-medium tracking-wide uppercase">
                Correo
              </dt>
              <dd className="text-ink text-sm font-medium">
                {seleccionado.correo}
              </dd>
            </div>
            <Field label="Rol">
              <span
                className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold ${ROL_CLASS[seleccionado.rol]}`}
              >
                {ROL_LABEL[seleccionado.rol]}
              </span>
            </Field>
            <Field label="Miembro desde">{seleccionado.miembroDesde}</Field>
            <Field label="Artículos asignados">
              {articulosPorUsuario[seleccionado.id] ?? 0}
            </Field>
          </dl>
          <p className="text-ink/40 text-xs">
            Vista previa — datos de ejemplo, todavía no hay edición real de
            usuarios conectada.
          </p>
        </Modal>
      )}
    </>
  );
}
