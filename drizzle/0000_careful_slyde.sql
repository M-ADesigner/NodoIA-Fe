CREATE TYPE "public"."categoria_contenido" AS ENUM('Artículo', 'Boletín', 'Noticia institucional');--> statement-breakpoint
CREATE TYPE "public"."estado_contenido" AS ENUM('borrador', 'asistido_ia', 'en_revision', 'aprobado', 'publicado');--> statement-breakpoint
CREATE TYPE "public"."rol" AS ENUM('administrador', 'editor', 'revisor');--> statement-breakpoint
CREATE TABLE "articulos" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"slug" text NOT NULL,
	"titulo" text NOT NULL,
	"categoria" "categoria_contenido" NOT NULL,
	"resumen" text NOT NULL,
	"contenido" text DEFAULT '' NOT NULL,
	"estado" "estado_contenido" DEFAULT 'borrador' NOT NULL,
	"autor_id" uuid NOT NULL,
	"creado_en" timestamp with time zone DEFAULT now() NOT NULL,
	"actualizado_en" timestamp with time zone DEFAULT now() NOT NULL,
	"publicado_en" timestamp with time zone,
	CONSTRAINT "articulos_slug_unique" UNIQUE("slug")
);
--> statement-breakpoint
CREATE TABLE "usuarios" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"nombre" text NOT NULL,
	"correo" text NOT NULL,
	"rol" "rol" NOT NULL,
	"miembro_desde" timestamp with time zone DEFAULT now() NOT NULL,
	CONSTRAINT "usuarios_correo_unique" UNIQUE("correo")
);
--> statement-breakpoint
ALTER TABLE "articulos" ADD CONSTRAINT "articulos_autor_id_usuarios_id_fk" FOREIGN KEY ("autor_id") REFERENCES "public"."usuarios"("id") ON DELETE no action ON UPDATE no action;