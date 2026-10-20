package dev.andrea.acompaname_backend.user;

import java.util.Set;

import dev.andrea.acompaname_backend.role.RoleEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private String email;
    private String telefono;
    private String password;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "roles_usuarios", joinColumns = @JoinColumn(name = "usuario_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<RoleEntity> roles;

    // Si es false, el usuario está bloqueado y no puede iniciar sesión.
    // El "default true" hace que los usuarios que ya existían en la base de datos queden activos
    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean activo = true;

    // Constructor sin activo (queda activo), para no romper el código que ya existía
    public UsuarioEntity(Long id, String nombre, String email, String telefono, String password,
            Set<RoleEntity> roles) {
        this(id, nombre, email, telefono, password, roles, true);
    }
}