import { readFile } from "node:fs/promises";
import { join } from "node:path";
import { ImageResponse } from "next/og";
import { SITE_LEGAL_NAME, truncate } from "@/lib/seo/site";

export const OG_SIZE = { width: 1200, height: 630 };

// Cabeceras de caché: las imágenes sociales casi no cambian y las piden bots.
const CACHE_HEADERS = {
  "Cache-Control":
    "public, max-age=3600, s-maxage=86400, stale-while-revalidate=604800",
};

let logoDataUri: string | null = null;
async function logo(): Promise<string> {
  if (!logoDataUri) {
    const buffer = await readFile(
      join(process.cwd(), "public", "images", "Logo_CCHIA.png"),
    );
    logoDataUri = `data:image/png;base64,${buffer.toString("base64")}`;
  }
  return logoDataUri;
}

export async function imagenSocial({
  titulo,
  etiqueta,
}: {
  titulo: string;
  etiqueta?: string;
}) {
  const logoSrc = await logo();
  const texto = truncate(titulo, 120);
  // Títulos largos se achican para que no se corten.
  const fontSize = texto.length > 90 ? 52 : texto.length > 60 ? 60 : 72;

  return new ImageResponse(
    <div
      style={{
        width: "100%",
        height: "100%",
        display: "flex",
        flexDirection: "column",
        justifyContent: "space-between",
        padding: "64px 72px",
        background:
          "linear-gradient(135deg, #1a365d 0%, #1a365d 35%, #2b77ad 100%)",
        color: "#ffffff",
      }}
    >
      <div style={{ display: "flex", flexDirection: "column", gap: 28 }}>
        {etiqueta ? (
          <div
            style={{
              display: "flex",
              alignSelf: "flex-start",
              padding: "10px 22px",
              borderRadius: 999,
              background: "#b1e346",
              color: "#18181b",
              fontSize: 26,
              letterSpacing: 2,
              textTransform: "uppercase",
            }}
          >
            {etiqueta}
          </div>
        ) : null}
        <div
          style={{
            display: "flex",
            fontSize,
            lineHeight: 1.18,
            letterSpacing: -1,
          }}
        >
          {texto}
        </div>
      </div>

      <div
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
        }}
      >
        <div
          style={{
            display: "flex",
            alignItems: "center",
            padding: "14px 28px",
            borderRadius: 24,
            background: "#ffffff",
          }}
        >
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src={logoSrc} width={210} height={87} alt="" />
        </div>
        <div style={{ display: "flex", fontSize: 26, opacity: 0.85 }}>
          {SITE_LEGAL_NAME}
        </div>
      </div>
    </div>,
    { ...OG_SIZE, headers: CACHE_HEADERS },
  );
}
