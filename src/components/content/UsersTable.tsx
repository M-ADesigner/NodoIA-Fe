"use client";

import { useActionState, useEffect, useState } from "react";
import {
  eliminarUsuario,
  guardarUsuario,
  type EstadoForm,
} from "@/app/dashboard/admin/actions";
import { Button } from "@/components/ui/Button";
import { Modal } from "@/components/ui/Modal";
import { ROL_LABEL } from "@/lib/auth/roles";
import type { Rol, Usuario } from "@/types/user";

const ROL_CLASS: Record<Rol, string> = {
  administrador: "bg-navy-900/10 text-navy-900",
  autor: "bg-lime-soft text-lime-ink",
  revisor: "bg-navy-700/12 text-navy-700",
};

const INPUT_CLASS =
  "border-border text-ink w-full rounded-lg border px-3 py-2 text-sm outline-none focus:border-navy-700";

function iniciales(nombre: string) {
  return nombre
    .split(" ")
    .map((parte) => parte[0])
    .slice(0, 2)
    .join("")
    .toUpperCase();
}

type RolOpcion = { clave: Rol; nombre: string };

// Modo del modal: null = cerrado, "nuevo" o el usuario que se edita.
type Modo = null | "nuevo" | Usuario;

const inicial: EstadoForm = {};

function FormularioUsuario({
  usuario,
  roles,
  esYo,
  articulos,
  onClose,
}: {
  usuario?: Usuario;
  roles: RolOpcion[];
  esYo: boolean;
  articulos: number;
  onClose: () => void;
}) {
  const [guardado, guardar, guardando] = useActionState(
    guardarUsuario,
    inicial,
  );
  const [borrado, borrar, borrando] = useActionState(eliminarUsuario, inicial);

  useEffect(() => {
    if (guardado.ok || borrado.ok) onClose();
  }, [guardado.ok, borrado.ok, onClose]);

  const error = guardado.error ?? borrado.error;

  return (
    <div className="flex flex-col gap-4">
      <form action={guardar} className="flex flex-col gap-3.5">
        {usuario && <input type="hidden" name="id" value={usuario.id} />}

        <label className="text-ink flex flex-col gap-1 text-xs font-medium tracking-wide uppercase">
          Nombre
          <input
            name="nombre"
            defaultValue={usuario?.nombre}
            required
            maxLength={80}
            className={INPUT_CLASS}
          />
        </label>

        <label className="text-ink flex flex-col gap-1 text-xs font-medium tracking-wide uppercase">
          Correo
          <input
            name="correo"
            type="email"
            defaultValue={usuario?.correo}
            required
            className={INPUT_CLASS}
          />
        </label>

        <label className="text-ink flex flex-col gap-1 text-xs font-medium tracking-wide uppercase">
          Rol
          <select
            name="rol"
            defaultValue={usuario?.rol ?? "autor"}
            className={INPUT_CLASS}
          >
            {roles.map((rol) => (
              <option key={rol.clave} value={rol.clave}>
                {rol.nombre}
              </option>
            ))}
          </select>
        </label>

        <label className="text-ink flex flex-col gap-1 text-xs font-medium tracking-wide uppercase">
          {usuario ? "Nueva contraseña (opcional)" : "Contraseña inicial"}
          <input
            name="password"
            type="password"
            autoComplete="new-password"
            required={!usuario}
            minLength={8}
            placeholder={
              usuario ? "Dejar vacío para no cambiarla" : "Mínimo 8 caracteres"
            }
            className={INPUT_CLASS}
          />
        </label>

        {usuario && (
          <label className="text-ink flex items-center gap-2 text-sm font-medium">
            <input
              name="activo"
              type="checkbox"
              defaultChecked={usuario.activo}
              disabled={esYo}
            />
            Cuenta activa
            {esYo && (
              <span className="text-ink/45 text-xs">(es tu cuenta)</span>
            )}
          </label>
        )}
        {/* Un checkbox deshabilitado no se envía: se reenvía como oculto. */}
        {usuario && esYo && <input type="hidden" name="activo" value="on" />}

        {error && (
          <p role="alert" className="text-sm font-medium text-red-600">
            {error}
          </p>
        )}

        <div className="flex justify-end gap-3">
          <Button type="button" variant="outline" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" disabled={guardando || borrando}>
            {guardando ? "Guardando…" : "Guardar"}
          </Button>
        </div>
      </form>

      {usuario && !esYo && (
        <form
          action={borrar}
          onSubmit={(event) => {
            if (!window.confirm(`¿Eliminar a ${usuario.nombre}?`)) {
              event.preventDefault();
            }
          }}
          className="border-border flex items-center justify-between gap-3 border-t pt-3"
        >
          <input type="hidden" name="id" value={usuario.id} />
          <p className="text-ink/50 text-xs">
            {articulos > 0
              ? `Tiene ${articulos} artículo(s): solo se puede desactivar.`
              : "Eliminar es permanente."}
          </p>
          <button
            type="submit"
            disabled={borrando || guardando || articulos > 0}
            className="text-sm font-semibold text-red-600 hover:underline disabled:cursor-not-allowed disabled:opacity-40"
          >
            Eliminar
          </button>
        </form>
      )}
    </div>
  );
}

export function UsersTable({
  usuarios,
  articulosPorUsuario,
  roles,
  usuarioActualId,
}: {
  usuarios: Usuario[];
  articulosPorUsuario: Record<string, number>;
  roles: RolOpcion[];
  usuarioActualId: string;
}) {
  const [modo, setModo] = useState<Modo>(null);
  const cerrar = () => setModo(null);

  return (
    <>
      <div className="flex justify-end">
        <Button type="button" onClick={() => setModo("nuevo")}>
          + Nuevo usuario
        </Button>
      </div>

      <div className="border-border shadow-nav rounded-medium overflow-hidden border bg-white">
        <div className="overflow-x-auto">
          <table className="w-full min-w-[520px] text-sm">
            <thead>
              <tr className="border-border text-ink/45 border-b text-xs font-bold tracking-widest uppercase">
                <th className="px-6 py-4 text-left">Usuario</th>
                <th className="px-6 py-4 text-left">Rol</th>
                <th className="px-6 py-4 text-left">Estado</th>
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
                        <p className="text-ink font-semibold">
                          {usuario.nombre}
                          {usuario.id === usuarioActualId && (
                            <span className="text-ink/45 ml-2 text-xs font-medium">
                              (tú)
                            </span>
                          )}
                        </p>
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
                    <span
                      className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold ${
                        usuario.activo
                          ? "bg-[#E4F1E6] text-[#3F7D4B]"
                          : "bg-[#ECEFE6] text-[#74766D]"
                      }`}
                    >
                      {usuario.activo ? "Activo" : "Inactivo"}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-right">
                    <button
                      type="button"
                      onClick={() => setModo(usuario)}
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

      {modo && (
        <Modal
          title={modo === "nuevo" ? "Nuevo usuario" : `Editar: ${modo.nombre}`}
          onClose={cerrar}
        >
          <FormularioUsuario
            // La key reinicia el formulario (y sus estados) al cambiar de usuario.
            key={modo === "nuevo" ? "nuevo" : modo.id}
            usuario={modo === "nuevo" ? undefined : modo}
            roles={roles}
            esYo={modo !== "nuevo" && modo.id === usuarioActualId}
            articulos={
              modo === "nuevo" ? 0 : (articulosPorUsuario[modo.id] ?? 0)
            }
            onClose={cerrar}
          />
        </Modal>
      )}
    </>
  );
}
