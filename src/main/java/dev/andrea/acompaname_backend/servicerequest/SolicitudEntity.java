package dev.andrea.acompaname_backend.servicerequest;

import java.time.LocalDate;
import java.time.LocalDateTime;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.globals.GrupoEdad;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "solicitudes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String tipoCuidado;
    private String nombrePaciente;
    private String notas;
    private Integer edadPaciente;
    private LocalDate fechaCuidado;
    private LocalDate fechaFin;
    @Enumerated(EnumType.STRING)
    private EstadoSolicitud estado;

    @ManyToOne
    @JoinColumn(name = "familia_id")
    private UsuarioEntity familia;

    @ManyToOne
    @JoinColumn(name = "cuidador_id")
    private PerfilCuidadorEntity cuidador;

    // Necesidades del paciente, guardadas como códigos separados por comas (por ejemplo "movilidad,higiene")
    private String necesidades;

    // Dirección del domicilio o nombre del hospital (planta, habitación...) donde se hará el cuidado
    private String direccion;

    // Momento en que se creó la solicitud (para las estadísticas del administrador)
    private LocalDateTime fechaCreacion;

    // Momento en que el cuidador la aceptó; es null mientras no esté aceptada
    private LocalDateTime fechaAceptacion;

    // Grupo de edad de la persona cuidada. Es opcional: las solicitudes antiguas no lo tienen (null)
    @Enumerated(EnumType.STRING)
    private GrupoEdad grupoEdad;

    // Constructor sin grupo de edad, para no romper el código que ya existía
    public SolicitudEntity(Long id, String tipoCuidado, String nombrePaciente, String notas, Integer edadPaciente,
            LocalDate fechaCuidado, LocalDate fechaFin, EstadoSolicitud estado, UsuarioEntity familia,
            PerfilCuidadorEntity cuidador, String necesidades, String direccion, LocalDateTime fechaCreacion,
            LocalDateTime fechaAceptacion) {
        this(id, tipoCuidado, nombrePaciente, notas, edadPaciente, fechaCuidado, fechaFin, estado, familia, cuidador,
                necesidades, direccion, fechaCreacion, fechaAceptacion, null);
    }

    // Constructor sin las fechas de creación y aceptación, para no romper el código que ya existía
    public SolicitudEntity(Long id, String tipoCuidado, String nombrePaciente, String notas, Integer edadPaciente,
            LocalDate fechaCuidado, LocalDate fechaFin, EstadoSolicitud estado, UsuarioEntity familia,
            PerfilCuidadorEntity cuidador, String necesidades, String direccion) {
        this(id, tipoCuidado, nombrePaciente, notas, edadPaciente, fechaCuidado, fechaFin, estado, familia, cuidador,
                necesidades, direccion, null, null);
    }

    // Constructor sin dirección, para no romper el código que ya existía
    public SolicitudEntity(Long id, String tipoCuidado, String nombrePaciente, String notas, Integer edadPaciente,
            LocalDate fechaCuidado, LocalDate fechaFin, EstadoSolicitud estado, UsuarioEntity familia,
            PerfilCuidadorEntity cuidador, String necesidades) {
        this(id, tipoCuidado, nombrePaciente, notas, edadPaciente, fechaCuidado, fechaFin, estado, familia, cuidador,
                necesidades, null);
    }

    // Constructor sin necesidades, para no romper el código que ya existía
    public SolicitudEntity(Long id, String tipoCuidado, String nombrePaciente, String notas, Integer edadPaciente,
            LocalDate fechaCuidado, LocalDate fechaFin, EstadoSolicitud estado, UsuarioEntity familia,
            PerfilCuidadorEntity cuidador) {
        this(id, tipoCuidado, nombrePaciente, notas, edadPaciente, fechaCuidado, fechaFin, estado, familia, cuidador,
                null);
    }

    // Constructor sin fecha de fin ni necesidades, para no romper el código que ya existía
    public SolicitudEntity(Long id, String tipoCuidado, String nombrePaciente, String notas, Integer edadPaciente,
            LocalDate fechaCuidado, EstadoSolicitud estado, UsuarioEntity familia, PerfilCuidadorEntity cuidador) {
        this(id, tipoCuidado, nombrePaciente, notas, edadPaciente, fechaCuidado, null, estado, familia, cuidador);
    }
}