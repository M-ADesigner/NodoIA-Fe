CREATE TABLE "roles" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"clave" text NOT NULL,
	"nombre" text NOT NULL,
	"descripcion" text DEFAULT '' NOT NULL,
	CONSTRAINT "roles_clave_unique" UNIQUE("clave")
);
--> statement-breakpoint
INSERT INTO "roles" ("clave", "nombre", "descripcion") VALUES
	('administrador', 'Administrador', 'Gestiona usuarios, contenido y publicación.'),
	('revisor', 'Revisor', 'Revisa y aprueba el contenido en cola.'),
	('autor', 'Autor', 'Genera y edita contenido con asistencia de IA.');
--> statement-breakpoint
ALTER TABLE "usuarios" ADD COLUMN "rol_id" uuid;--> statement-breakpoint
UPDATE "usuarios" SET "rol_id" = (
	SELECT "id" FROM "roles" WHERE "clave" = CASE "usuarios"."rol"::text WHEN 'editor' THEN 'autor' ELSE "usuarios"."rol"::text END
);--> statement-breakpoint
ALTER TABLE "usuarios" ALTER COLUMN "rol_id" SET NOT NULL;--> statement-breakpoint
-- '!' no es un hash válido: nadie puede iniciar sesión hasta que el seed (o el admin) asigne contraseña.
ALTER TABLE "usuarios" ADD COLUMN "password_hash" text NOT NULL DEFAULT '!';--> statement-breakpoint
ALTER TABLE "usuarios" ALTER COLUMN "password_hash" DROP DEFAULT;--> statement-breakpoint
ALTER TABLE "usuarios" ADD COLUMN "activo" boolean DEFAULT true NOT NULL;--> statement-breakpoint
ALTER TABLE "usuarios" ADD COLUMN "ultimo_acceso" timestamp with time zone;--> statement-breakpoint
ALTER TABLE "usuarios" ADD CONSTRAINT "usuarios_rol_id_roles_id_fk" FOREIGN KEY ("rol_id") REFERENCES "public"."roles"("id") ON DELETE no action ON UPDATE no action;
