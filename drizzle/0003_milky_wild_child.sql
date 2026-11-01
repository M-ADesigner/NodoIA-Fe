CREATE TABLE "revisiones" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"articulo_id" uuid NOT NULL,
	"usuario_id" uuid NOT NULL,
	"estado_anterior" "estado_contenido" NOT NULL,
	"estado_nuevo" "estado_contenido" NOT NULL,
	"comentario" text,
	"creado_en" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
ALTER TABLE "revisiones" ADD CONSTRAINT "revisiones_articulo_id_articulos_id_fk" FOREIGN KEY ("articulo_id") REFERENCES "public"."articulos"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "revisiones" ADD CONSTRAINT "revisiones_usuario_id_usuarios_id_fk" FOREIGN KEY ("usuario_id") REFERENCES "public"."usuarios"("id") ON DELETE no action ON UPDATE no action;