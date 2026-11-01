CREATE TABLE "intentos_login" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"clave" text NOT NULL,
	"creado_en" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE INDEX "intentos_login_clave_creado_en_idx" ON "intentos_login" USING btree ("clave","creado_en");