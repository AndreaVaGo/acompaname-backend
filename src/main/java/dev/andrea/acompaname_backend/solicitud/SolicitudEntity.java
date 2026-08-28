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

@Entity
@Table(name = "solicitudes")
public class SolicitudEntity {
    
    @Id @GeneratedValue (strategy = GenerationType.IDENTITY)
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

    public SolicitudEntity() {
    }

    public SolicitudEntity(Long id, String tipoCuidado, String nombrePaciente, String notas, Integer edadPaciente,
            LocalDate fechaCuidado, EstadoSolicitud estado, UsuarioEntity familia, PerfilCuidadorEntity cuidador) {
        this.id = id;
        this.tipoCuidado = tipoCuidado;
        this.nombrePaciente = nombrePaciente;
        this.notas = notas;
        this.edadPaciente = edadPaciente;
        this.fechaCuidado = fechaCuidado;
        this.estado = estado;
        this.familia = familia;
        this.cuidador = cuidador;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTipoCuidado() {
        return tipoCuidado;
    }

    public void setTipoCuidado(String tipoCuidado) {
        this.tipoCuidado = tipoCuidado;
    }

    public String getNombrePaciente() {
        return nombrePaciente;
    }

    public void setNombrePaciente(String nombrePaciente) {
        this.nombrePaciente = nombrePaciente;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    public Integer getEdadPaciente() {
        return edadPaciente;
    }

    public void setEdadPaciente(Integer edadPaciente) {
        this.edadPaciente = edadPaciente;
    }

    public LocalDate getFechaCuidado() {
        return fechaCuidado;
    }

    public void setFechaCuidado(LocalDate fechaCuidado) {
        this.fechaCuidado = fechaCuidado;
    }

    public EstadoSolicitud getEstado() {
        return estado;
    }

    public void setEstado(EstadoSolicitud estado) {
        this.estado = estado;
    }

    public UsuarioEntity getFamilia() {
        return familia;
    }

    public void setFamilia(UsuarioEntity familia) {
        this.familia = familia;
    }

    public PerfilCuidadorEntity getCuidador() {
        return cuidador;
    }

    public void setCuidador(PerfilCuidadorEntity cuidador) {
        this.cuidador = cuidador;
    }

    


}
