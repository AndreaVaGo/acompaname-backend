package dev.andrea.acompaname_backend.role;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.andrea.acompaname_backend.user.UsuarioEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "roles")
@Data
public class RoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_role")
    private Long id;
    private String name;

    // Fuera de equals, hashCode y toString: si entraran, cada vez que Hibernate mete un rol en un Set
    // cargaría de la base de datos todos los usuarios de ese rol (una consulta de más, y muy pesada)
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToMany(mappedBy = "roles")
    private Set<UsuarioEntity> users;
}
