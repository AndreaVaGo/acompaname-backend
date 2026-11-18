package dev.andrea.acompaname_backend.passwordreset;

import java.time.LocalDateTime;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import dev.andrea.acompaname_backend.user.UsuarioEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

// Un enlace de "Olvidé mi contraseña" que se ha mandado por correo.
// Nunca se guarda el token de verdad, solo su resumen (hash): así, aunque alguien viera la base de datos,
// no podría usar los enlaces. El token de verdad solo existe en el correo que recibe la persona.
@Entity
@Table(name = "tokens_recuperacion_password")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Resumen SHA-256 del token, en hexadecimal (siempre 64 caracteres)
    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    // Si se borra el usuario, la base de datos borra también sus tokens
    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UsuarioEntity usuario;

    // Hasta cuándo se puede usar el enlace
    @Column(nullable = false)
    private LocalDateTime expiraEn;

    // El token ya no sirve si su hora de caducidad es anterior a la que se pasa
    public boolean estaCaducado(LocalDateTime ahora) {
        return expiraEn.isBefore(ahora);
    }
}
