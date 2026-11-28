-- ACOM-261: la dirección del calendario se guarda cifrada y ocupa más que antes.
-- Se ejecuta UNA vez a mano (en producción Hibernate solo valida, no modifica).
-- Las direcciones antiguas (en claro) siguen funcionando y se cifran solas en la siguiente sincronización.
ALTER TABLE suscripciones_calendario MODIFY url VARCHAR(1000) NOT NULL;
