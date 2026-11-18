package dev.andrea.acompaname_backend.servicerequest;

import dev.andrea.acompaname_backend.globals.GrupoEdad;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Una persona que recibe el cuidado dentro de una solicitud (nombre y edad).
// Una solicitud puede tener varias (por ejemplo, tres niños o un matrimonio mayor).
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonaCuidada {

    private String nombre;
    private Integer edad;

    // Grupo de edad de esta persona. Es opcional: las solicitudes antiguas no lo tienen (null)
    // y usan el grupo de la solicitud
    @Enumerated(EnumType.STRING)
    private GrupoEdad grupoEdad;

    // Constructor sin grupo de edad, para no romper el código que ya existía
    public PersonaCuidada(String nombre, Integer edad) {
        this(nombre, edad, null);
    }
}
