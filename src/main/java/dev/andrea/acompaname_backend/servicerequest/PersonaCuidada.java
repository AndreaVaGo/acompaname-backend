package dev.andrea.acompaname_backend.servicerequest;

import jakarta.persistence.Embeddable;
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
}
