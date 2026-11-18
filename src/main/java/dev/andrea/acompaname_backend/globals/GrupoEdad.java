package dev.andrea.acompaname_backend.globals;

// Grupos de edad de la persona que recibe el cuidado.
// Está definido UNA sola vez: lo usan el perfil de la persona cuidadora y la solicitud.
public enum GrupoEdad {
    BEBES("Bebés"),                                      // de 0 a 3 años
    NINOS("Niñas y niños"),                              // de 4 a 12 años
    ADOLESCENTES("Adolescentes"),                        // de 13 a 17 años
    ADULTOS("Personas adultas"),                         // de 18 a 64 años
    MAYORES("Personas mayores"),                         // 65 años o más
    DISCAPACIDAD_DEPENDENCIA("Discapacidad o dependencia"); // a cualquier edad

    private final String etiqueta;

    GrupoEdad(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    // Nombre para enseñar en los mensajes (no cambia lo que viaja por la API, que es el nombre del enum)
    public String etiqueta() {
        return etiqueta;
    }
}
