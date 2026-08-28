package dev.andrea.acompaname_backend.perfilcuidador;

import java.math.BigDecimal;

import dev.andrea.acompaname_backend.usuario.UsuarioEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "perfilcuidadores")
public class PerfilCuidadorEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY )
    private Long id;
    private String especialidad;
    private Integer anosExperiencia;
    private BigDecimal tarifaHora;
    private String bio;
    private boolean tieneVehiculo;
    private boolean disponibleAhora;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", referencedColumnName = "id")
    private UsuarioEntity usuario;

    public PerfilCuidadorEntity() {
    }

    public PerfilCuidadorEntity(Long id, String especialidad, Integer anosExperiencia, BigDecimal tarifaHora,
            String bio, boolean tieneVehiculo, boolean disponibleAhora, UsuarioEntity usuario) {
        this.id = id;
        this.especialidad = especialidad;
        this.anosExperiencia = anosExperiencia;
        this.tarifaHora = tarifaHora;
        this.bio = bio;
        this.tieneVehiculo = tieneVehiculo;
        this.disponibleAhora = disponibleAhora;
        this.usuario = usuario;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    public Integer getAnosExperiencia() {
        return anosExperiencia;
    }

    public void setAnosExperiencia(Integer anosExperiencia) {
        this.anosExperiencia = anosExperiencia;
    }

    public BigDecimal getTarifaHora() {
        return tarifaHora;
    }

    public void setTarifaHora(BigDecimal tarifaHora) {
        this.tarifaHora = tarifaHora;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public boolean isTieneVehiculo() {
        return tieneVehiculo;
    }

    public void setTieneVehiculo(boolean tieneVehiculo) {
        this.tieneVehiculo = tieneVehiculo;
    }

    public boolean isDisponibleAhora() {
        return disponibleAhora;
    }

    public void setDisponibleAhora(boolean disponibleAhora) {
        this.disponibleAhora = disponibleAhora;
    }

    public UsuarioEntity getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioEntity usuario) {
        this.usuario = usuario;
    }

    

}
