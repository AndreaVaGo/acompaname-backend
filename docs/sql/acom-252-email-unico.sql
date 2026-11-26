-- ACOM-252: el email de usuarios pasa a ser único y obligatorio en la base de datos.
-- Solo hace falta en bases de datos que ya existían. Una base nueva creada con
-- schema-completo.sql ya lo trae.
--
-- 1) Antes de nada, comprueba que no hay emails repetidos ni vacíos (debe devolver 0 filas):
--    SELECT email, COUNT(*) FROM usuarios GROUP BY email HAVING COUNT(*) > 1 OR email IS NULL;
-- 2) Haz una copia de seguridad.
-- 3) Ejecuta:
ALTER TABLE usuarios MODIFY email VARCHAR(255) NOT NULL;
ALTER TABLE usuarios ADD CONSTRAINT uk_usuarios_email UNIQUE (email);
