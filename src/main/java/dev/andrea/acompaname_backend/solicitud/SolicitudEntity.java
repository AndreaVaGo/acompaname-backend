package dev.andrea.acompaname_backend.solicitud;

import java.time.LocalDate;

import dev.andrea.acompaname_backend.perfilcuidador.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.usuario.UsuarioEntity;
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
    @Enumerated(EnumType.STRING)
    private EstadoSolicitud estado;

    @ManyToOne
    @JoinColumn(name = "familia_id")
    private UsuarioEntity familia;

    @ManyToOne
    @JoinColumn(name = "cuidador_id")
    private PerfilCuidadorEntity cuidador;
}