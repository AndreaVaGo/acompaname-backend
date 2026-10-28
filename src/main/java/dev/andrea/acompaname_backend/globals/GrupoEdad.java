package dev.andrea.acompaname_backend.globals;

// Grupos de edad de la persona que recibe el cuidado.
// Está definido UNA sola vez: lo usan el perfil de la persona cuidadora y la solicitud.
public enum GrupoEdad {
    BEBES,                      // de 0 a 3 años
    NINOS,                      // de 4 a 12 años
    ADOLESCENTES,               // de 13 a 17 años
    ADULTOS,                    // de 18 a 64 años
    MAYORES,                    // 65 años o más
    DISCAPACIDAD_DEPENDENCIA;   // a cualquier edad
}