package dev.andrea.acompaname_backend.perfilcuidador;

import java.math.BigDecimal;

import dev.andrea.acompaname_backend.usuario.UsuarioEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "perfilcuidadores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerfilCuidadorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    // Enlace a la foto de perfil (puede estar vacío)
    @Column(length = 500)
    private String foto;

    // Constructor sin foto: el mismo que había antes de añadir este campo
    public PerfilCuidadorEntity(Long id, String especialidad, Integer anosExperiencia, BigDecimal tarifaHora,
            String bio, boolean tieneVehiculo, boolean disponibleAhora, UsuarioEntity usuario) {
        this(id, especialidad, anosExperiencia, tarifaHora, bio, tieneVehiculo, disponibleAhora, usuario, null);
    }
}