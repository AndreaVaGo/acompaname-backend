package dev.andrea.acompaname_backend.servicerequest;

import java.util.ArrayList;
import java.util.List;

import dev.andrea.acompaname_backend.servicerequest.dtos.PersonaCuidadaDTO;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTORequest;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionPersonasInvalidas;

// Reglas de la lista de personas a cuidar de una solicitud
public class PersonasDeSolicitud {

    public static final int MAXIMO = 5;

    // Saca la lista de personas de lo que envía el usuario.
    // Si no llega la lista pero sí el nombre y la edad sueltos (forma antigua), es una sola persona.
    public static List<PersonaCuidada> desdePeticion(SolicitudDTORequest dto) {
        List<PersonaCuidada> personas = new ArrayList<>();

        if (dto.personas() != null && !dto.personas().isEmpty()) {
            for (PersonaCuidadaDTO persona : dto.personas()) {
                personas.add(new PersonaCuidada(persona.nombre().trim(), persona.edad()));
            }
        } else if (dto.nombrePaciente() != null && !dto.nombrePaciente().isBlank() && dto.edadPaciente() != null) {
            personas.add(new PersonaCuidada(dto.nombrePaciente().trim(), dto.edadPaciente()));
        }

        if (personas.isEmpty()) {
            throw new SolicitudExceptionPersonasInvalidas("Hay que indicar al menos una persona a cuidar");
        }
        if (personas.size() > MAXIMO) {
            throw new SolicitudExceptionPersonasInvalidas(
                    "No se pueden indicar más de " + MAXIMO + " personas a cuidar");
        }
        return personas;
    }

    // Pasa las personas guardadas a DTO. Las solicitudes antiguas no tienen lista:
    // cuentan como una sola persona, con el nombre y la edad que ya tenían
    public static List<PersonaCuidadaDTO> aDTO(SolicitudEntity solicitud) {
        List<PersonaCuidadaDTO> lista = new ArrayList<>();
        if (solicitud.getPersonas() != null && !solicitud.getPersonas().isEmpty()) {
            for (PersonaCuidada persona : solicitud.getPersonas()) {
                lista.add(new PersonaCuidadaDTO(persona.getNombre(), persona.getEdad()));
            }
        } else if (solicitud.getNombrePaciente() != null) {
            lista.add(new PersonaCuidadaDTO(solicitud.getNombrePaciente(), solicitud.getEdadPaciente()));
        }
        return lista;
    }
}
