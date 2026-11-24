-- ACOM-243: fecha de alta del usuario
-- Producción usa ddl-auto=validate, así que esta columna hay que crearla A MANO
-- ANTES de desplegar la versión del back que trae el campo fechaAlta.
-- En local (ddl-auto=update) Hibernate la crea solo.
-- Los usuarios que ya existían quedan con fecha_alta NULL (no cuentan como nuevos).

ALTER TABLE usuarios ADD COLUMN fecha_alta DATETIME(6) NULL;
