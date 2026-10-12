import type { ButtonHTMLAttributes } from "react";

type Variant = "primary" | "outline";

// Specs reales del sitio (ver .claude/skills/cchia-design/SKILL.md): el
// primario es un gradiente oscuro con texto blanco, NO lima — el lima es
// solo un color de acento.
const base =
  "rounded-full px-[30px] py-[13px] text-base font-medium tracking-[-0.3px] transition-transform hover:scale-105 disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:scale-100";

const variants: Record<Variant, string> = {
  primary:
    "bg-gradient-to-b from-btn-start to-btn-end border border-slate-600/30 text-white shadow-btn",
  outline: "border border-border-btn bg-white text-ink",
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
