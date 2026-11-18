package dev.andrea.acompaname_backend.servicerequest.dtos;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import dev.andrea.acompaname_backend.globals.GrupoEdad;
import dev.andrea.acompaname_backend.servicerequest.EstadoSolicitud;

public record SolicitudDTOResponse(Long id, String tipoCuidado, String nombrePaciente, String notas,
                Integer edadPaciente, LocalDate fechaCuidado, LocalDate fechaFin, EstadoSolicitud estado,
                Long familiaId, Long cuidadorId, String familiaNombre, String cuidadorNombre,
                List<String> necesidades, String direccion, GrupoEdad grupoEdad, LocalTime horaInicio,
                LocalTime horaFin, List<PersonaCuidadaDTO> personas) {

        // Constructor sin la lista de personas: la lista tiene una sola persona, con el nombre y la edad sueltos
        public SolicitudDTOResponse(Long id, String tipoCuidado, String nombrePaciente, String notas,
                        Integer edadPaciente, LocalDate fechaCuidado, LocalDate fechaFin, EstadoSolicitud estado,
                        Long familiaId, Long cuidadorId, String familiaNombre, String cuidadorNombre,
                        List<String> necesidades, String direccion, GrupoEdad grupoEdad, LocalTime horaInicio,
                        LocalTime horaFin) {
                this(id, tipoCuidado, nombrePaciente, notas, edadPaciente, fechaCuidado, fechaFin, estado, familiaId,
                                cuidadorId, familiaNombre, cuidadorNombre, necesidades, direccion, grupoEdad,
                                horaInicio, horaFin, listaDeUna(nombrePaciente, edadPaciente));
        }

        private static List<PersonaCuidadaDTO> listaDeUna(String nombre, Integer edad) {
                List<PersonaCuidadaDTO> lista = new ArrayList<>();
                if (nombre != null) {
                        lista.add(new PersonaCuidadaDTO(nombre, edad));
                }
                return lista;
        }

        // Constructor sin horas (null = día completo), para no romper el código que ya existía
        public SolicitudDTOResponse(Long id, String tipoCuidado, String nombrePaciente, String notas,
                        Integer edadPaciente, LocalDate fechaCuidado, LocalDate fechaFin, EstadoSolicitud estado,
                        Long familiaId, Long cuidadorId, String familiaNombre, String cuidadorNombre,
                        List<String> necesidades, String direccion, GrupoEdad grupoEdad) {
                this(id, tipoCuidado, nombrePaciente, notas, edadPaciente, fechaCuidado, fechaFin, estado, familiaId,
                                cuidadorId, familiaNombre, cuidadorNombre, necesidades, direccion, grupoEdad, null,
                                null);
        }

        // Constructor sin grupo de edad, para no romper el código que ya existía
        public SolicitudDTOResponse(Long id, String tipoCuidado, String nombrePaciente, String notas,
                        Integer edadPaciente, LocalDate fechaCuidado, LocalDate fechaFin, EstadoSolicitud estado,
                        Long familiaId, Long cuidadorId, String familiaNombre, String cuidadorNombre,
                        List<String> necesidades, String direccion) {
                this(id, tipoCuidado, nombrePaciente, notas, edadPaciente, fechaCuidado, fechaFin, estado, familiaId,
                                cuidadorId, familiaNombre, cuidadorNombre, necesidades, direccion, null);
        }

        // Constructor sin dirección, para no romper el código que ya existía
        public SolicitudDTOResponse(Long id, String tipoCuidado, String nombrePaciente, String notas,
                        Integer edadPaciente, LocalDate fechaCuidado, LocalDate fechaFin, EstadoSolicitud estado,
                        Long familiaId, Long cuidadorId, String familiaNombre, String cuidadorNombre,
                        List<String> necesidades) {
                this(id, tipoCuidado, nombrePaciente, notas, edadPaciente, fechaCuidado, fechaFin, estado, familiaId,
                                cuidadorId, familiaNombre, cuidadorNombre, necesidades, null);
        }

        // Constructor sin necesidades, para no romper el código que ya existía
        public SolicitudDTOResponse(Long id, String tipoCuidado, String nombrePaciente, String notas,
                        Integer edadPaciente, LocalDate fechaCuidado, LocalDate fechaFin, EstadoSolicitud estado,
                        Long familiaId, Long cuidadorId, String familiaNombre, String cuidadorNombre) {
                this(id, tipoCuidado, nombrePaciente, notas, edadPaciente, fechaCuidado, fechaFin, estado, familiaId,
                                cuidadorId, familiaNombre, cuidadorNombre, new ArrayList<>());
        }

        // Constructor sin fecha de fin ni necesidades, para no romper el código que ya existía
        public SolicitudDTOResponse(Long id, String tipoCuidado, String nombrePaciente, String notas,
                        Integer edadPaciente, LocalDate fechaCuidado, EstadoSolicitud estado, Long familiaId,
                        Long cuidadorId, String familiaNombre, String cuidadorNombre) {
                this(id, tipoCuidado, nombrePaciente, notas, edadPaciente, fechaCuidado, null, estado, familiaId,
                                cuidadorId, familiaNombre, cuidadorNombre);
        }
}