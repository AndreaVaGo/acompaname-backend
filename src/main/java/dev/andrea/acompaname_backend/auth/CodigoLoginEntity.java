package dev.andrea.acompaname_backend.auth;

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

// Un código de "inicio de sesión en dos pasos" que se ha mandado por correo.
// Como con los enlaces de recuperar la contraseña, nunca se guarda el código de verdad, solo su resumen (hash).
// Cada código va unido a un "desafío": un texto largo al azar que el front guarda mientras la persona
// mira su correo. Sin el desafío correcto, el código de 6 cifras no sirve de nada.
@Entity
@Table(name = "codigos_login")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodigoLoginEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Resumen SHA-256 del desafío (64 caracteres). Con él se busca el código en la base de datos
    @Column(nullable = false, unique = true, length = 64)
    private String desafioHash;

    // Resumen SHA-256 del desafío junto con el código (64 caracteres)
    @Column(nullable = false, length = 64)
    private String codigoHash;

    // Si se borra el usuario, la base de datos borra también sus códigos
    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UsuarioEntity usuario;

    // Hasta cuándo se puede usar el código
    @Column(nullable = false)
    private LocalDateTime expiraEn;

    // Cuántas veces se ha fallado ya con este código
    @Builder.Default
    @Column(nullable = false)
    private int intentos = 0;

    // El código ya no sirve si su hora de caducidad es anterior a la que se pasa
    public boolean estaCaducado(LocalDateTime ahora) {
        return expiraEn.isBefore(ahora);
    }

    // Apunta un fallo más
    public void sumarIntento() {
        intentos = intentos + 1;
    }
}
