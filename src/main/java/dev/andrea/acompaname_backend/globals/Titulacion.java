package dev.andrea.acompaname_backend.globals;

// Titulaciones y formación que una persona cuidadora puede marcar en su perfil.
// Solo son una declaración: la app no comprueba ni guarda ningún documento que lo acredite
public enum Titulacion {
    ENFERMERIA,                 // Grado o diplomatura en Enfermería
    AUXILIAR_ENFERMERIA,        // Técnico en Cuidados Auxiliares de Enfermería (TCAE)
    ATENCION_DEPENDENCIA,       // Técnico en atención a personas en situación de dependencia
    ATENCION_SOCIOSANITARIA,    // Certificado de profesionalidad en atención sociosanitaria
    PRIMEROS_AUXILIOS;          // Curso de primeros auxilios
}
