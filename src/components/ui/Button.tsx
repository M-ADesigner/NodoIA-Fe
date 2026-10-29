import type { ButtonHTMLAttributes } from "react";

type Variant = "primary" | "outline" | "accent" | "ghost";

// primary: gradiente oscuro, para fondos claros.
// outline: borde suave, para fondos claros.
// accent: lima, para fondos azules (hero, banda de socios).
// ghost: contorno blanco, para fondos azules.
const base =
  "inline-flex min-h-12 items-center justify-center rounded-full px-7 py-3 text-[15px] font-semibold tracking-[-0.2px] transition hover:-translate-y-0.5 disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:translate-y-0";

const variants: Record<Variant, string> = {
  primary:
    "bg-gradient-to-b from-btn-start to-btn-end border border-slate-600/30 text-white shadow-btn",
  outline:
    "border-[1.5px] border-border-btn bg-white text-navy-900 hover:border-navy-700",
  accent: "bg-primary text-primary-ink font-bold hover:brightness-95",
  ghost: "border-[1.5px] border-white/55 text-white hover:bg-white/10",
};

export function buttonClasses(variant: Variant = "primary", className = "") {
  return `${base} ${variants[variant]} ${className}`;
}

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: Variant;
};

export function Button({
  variant = "primary",
  className = "",
  ...props
}: ButtonProps) {
  return <button className={buttonClasses(variant, className)} {...props} />;
}
