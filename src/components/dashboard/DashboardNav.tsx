"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

// Trazos de iconos (24x24, stroke) por vista del panel.
const ICONOS: Record<string, string> = {
  "/dashboard/contenido":
    "M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9zM14 3v6h6M8 13h8M8 17h5",
  "/dashboard/revision":
    "M9 11l3 3 8-8M20 12v7a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h9",
  "/dashboard/admin":
    "M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75",
};

export function DashboardNav({
  items,
}: {
  items: { href: string; label: string }[];
}) {
  const pathname = usePathname();

  return (
    <nav aria-label="Panel">
      <ul className="flex flex-col gap-1">
        {items.map((item) => {
          const actual = pathname?.startsWith(item.href);
          return (
            <li key={item.href}>
              <Link
                href={item.href}
                aria-current={actual ? "page" : undefined}
                className={
                  actual
                    ? "bg-primary/16 text-lime-soft flex min-h-[46px] items-center gap-3 rounded-xl px-3 text-[15px] font-bold"
                    : "flex min-h-[46px] items-center gap-3 rounded-xl px-3 text-[15px] font-semibold text-white/80 transition-colors hover:bg-white/8 hover:text-white"
                }
              >
                <svg
                  aria-hidden="true"
                  width="20"
                  height="20"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="1.8"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <path
                    d={ICONOS[item.href] ?? ICONOS["/dashboard/contenido"]}
                  />
                </svg>
                {item.label}
              </Link>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
