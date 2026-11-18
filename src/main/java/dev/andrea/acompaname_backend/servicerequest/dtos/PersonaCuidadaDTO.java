package dev.andrea.acompaname_backend.servicerequest.dtos;

import dev.andrea.acompaname_backend.globals.GrupoEdad;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Nombre y edad de una persona que recibe el cuidado (sirve para pedir y para responder)
public record PersonaCuidadaDTO(

                @NotBlank(message = "El nombre de la persona a cuidar no puede estar vacio") @Size(max = 100, message = "El nombre no puede superar 100 caracteres") String nombre,

                @NotNull(message = "La edad de la persona a cuidar no puede ser nula") @Min(value = 0, message = "La edad no puede ser negativa") @Max(value = 120, message = "La edad no puede superar 120 años") Integer edad,

                // Grupo de edad de esta persona (opcional). Si no llega, se usa el de la solicitud
                GrupoEdad grupoEdad

) {

        // Constructor sin grupo de edad, para no romper el código que ya existía
        public PersonaCuidadaDTO(String nombre, Integer edad) {
                this(nombre, edad, null);
        }
}
