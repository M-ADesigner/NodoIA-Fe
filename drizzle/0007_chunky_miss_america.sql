CREATE TABLE "articulo_fuentes" (
	"articulo_id" uuid NOT NULL,
	"fuente_id" uuid NOT NULL,
	"orden" integer DEFAULT 0 NOT NULL,
	CONSTRAINT "articulo_fuentes_articulo_id_fuente_id_pk" PRIMARY KEY("articulo_id","fuente_id")
);
--> statement-breakpoint
CREATE TABLE "fuentes" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"titulo" text NOT NULL,
	"url" text NOT NULL,
	"tipo" text NOT NULL,
	"organismo" text,
	"creado_por" uuid,
	"creado_en" timestamp with time zone DEFAULT now() NOT NULL,
	CONSTRAINT "fuentes_url_unique" UNIQUE("url")
);
--> statement-breakpoint
ALTER TABLE "articulo_fuentes" ADD CONSTRAINT "articulo_fuentes_articulo_id_articulos_id_fk" FOREIGN KEY ("articulo_id") REFERENCES "public"."articulos"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "articulo_fuentes" ADD CONSTRAINT "articulo_fuentes_fuente_id_fuentes_id_fk" FOREIGN KEY ("fuente_id") REFERENCES "public"."fuentes"("id") ON DELETE no action ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "fuentes" ADD CONSTRAINT "fuentes_creado_por_usuarios_id_fk" FOREIGN KEY ("creado_por") REFERENCES "public"."usuarios"("id") ON DELETE set null ON UPDATE no action;