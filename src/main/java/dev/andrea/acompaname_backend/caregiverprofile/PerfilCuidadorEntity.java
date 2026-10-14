package dev.andrea.acompaname_backend.caregiverprofile;

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

    // Foto de perfil guardada como texto (puede estar vacía)
    @Column(columnDefinition = "LONGTEXT")
    private String foto;

    // Tipo de cuidado que ofrece: hospitalario, domicilio o ambos (si es null se trata como ambos)
    private String tipoCuidado;

    // Servicios en los que tiene experiencia, separados por comas (por ejemplo "movilidad,medicacion").
    // Si es null o está vacío, no ha marcado ninguno
    private String servicios;

    // Sexo del cuidador: "MUJER" o "HOMBRE". Es opcional: si es null, no lo ha indicado
    private String sexo;

    // Ciudad donde trabaja el cuidador (puede estar vacía)
    private String ciudad;

    // Constructor sin ciudad: el mismo que había antes de añadir este campo
    public PerfilCuidadorEntity(Long id, String especialidad, Integer anosExperiencia, BigDecimal tarifaHora,
            String bio, boolean tieneVehiculo, boolean disponibleAhora, UsuarioEntity usuario, String foto,
            String tipoCuidado, String servicios, String sexo) {
        this(id, especialidad, anosExperiencia, tarifaHora, bio, tieneVehiculo, disponibleAhora, usuario, foto,
                tipoCuidado, servicios, sexo, null);
    }

    // Constructor sin sexo: el mismo que había antes de añadir ese campo
    public PerfilCuidadorEntity(Long id, String especialidad, Integer anosExperiencia, BigDecimal tarifaHora,
            String bio, boolean tieneVehiculo, boolean disponibleAhora, UsuarioEntity usuario, String foto,
            String tipoCuidado, String servicios) {
        this(id, especialidad, anosExperiencia, tarifaHora, bio, tieneVehiculo, disponibleAhora, usuario, foto,
                tipoCuidado, servicios, null, null);
    }

    // Constructor sin servicios: el mismo que había antes de añadir ese campo
    public PerfilCuidadorEntity(Long id, String especialidad, Integer anosExperiencia, BigDecimal tarifaHora,
            String bio, boolean tieneVehiculo, boolean disponibleAhora, UsuarioEntity usuario, String foto,
            String tipoCuidado) {
        this(id, especialidad, anosExperiencia, tarifaHora, bio, tieneVehiculo, disponibleAhora, usuario, foto,
                tipoCuidado, null, null);
    }

    // Constructor sin tipo de cuidado: el mismo que había antes de añadir ese campo
    public PerfilCuidadorEntity(Long id, String especialidad, Integer anosExperiencia, BigDecimal tarifaHora,
            String bio, boolean tieneVehiculo, boolean disponibleAhora, UsuarioEntity usuario, String foto) {
        this(id, especialidad, anosExperiencia, tarifaHora, bio, tieneVehiculo, disponibleAhora, usuario, foto,
                null, null, null);
    }

    // Constructor sin foto: el mismo que había antes de añadir ese campo
    public PerfilCuidadorEntity(Long id, String especialidad, Integer anosExperiencia, BigDecimal tarifaHora,
            String bio, boolean tieneVehiculo, boolean disponibleAhora, UsuarioEntity usuario) {
        this(id, especialidad, anosExperiencia, tarifaHora, bio, tieneVehiculo, disponibleAhora, usuario, null,
                null, null, null);
    }
}