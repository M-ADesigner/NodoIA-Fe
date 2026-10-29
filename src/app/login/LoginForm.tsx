"use client";

import { useActionState } from "react";
import { Button } from "@/components/ui/Button";
import { iniciarSesion, type LoginState } from "./actions";

const inicial: LoginState = {};

const INPUT_CLASS =
  "min-h-[50px] rounded-[14px] border-[1.5px] border-border-btn bg-white px-4 text-base text-ink outline-none focus:border-navy-700 focus:ring-4 focus:ring-navy-700/20";

export function LoginForm({ next }: { next: string }) {
  const [estado, accion, pendiente] = useActionState(iniciarSesion, inicial);

  return (
    <form action={accion} className="flex flex-col gap-[18px]">
      <input type="hidden" name="next" value={next} />

      <label className="text-navy-800 flex flex-col gap-2 text-sm font-semibold">
        Correo
        <input
          name="correo"
          type="email"
          autoComplete="username"
          placeholder="nombre@cchia.cl"
          required
          className={INPUT_CLASS}
        />
      </label>

      <label className="text-navy-800 flex flex-col gap-2 text-sm font-semibold">
        Contraseña
        <input
          name="password"
          type="password"
          autoComplete="current-password"
          placeholder="Tu contraseña"
          required
          className={INPUT_CLASS}
        />
      </label>

      {estado.error && (
        <p
          role="alert"
          className="rounded-xl bg-red-50 px-3.5 py-3 text-sm leading-snug text-red-800"
        >
          {estado.error}
        </p>
      )}

      <Button type="submit" disabled={pendiente} className="mt-1 min-h-[52px]">
        {pendiente ? "Ingresando…" : "Ingresar al panel"}
      </Button>
    </form>
  );
}
