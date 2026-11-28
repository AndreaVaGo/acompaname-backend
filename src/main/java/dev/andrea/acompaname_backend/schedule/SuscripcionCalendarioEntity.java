package dev.andrea.acompaname_backend.schedule;

import java.time.LocalDateTime;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

// El calendario externo (Google, Outlook, iCloud...) que una persona cuidadora ha vinculado.
// Solo puede tener uno. La dirección es secreta (cualquiera que la tenga puede leer ese calendario),
// así que NUNCA se devuelve por la API y en la base de datos se guarda CIFRADA (ver CifradorDeUrl).
@Entity
@Table(name = "suscripciones_calendario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuscripcionCalendarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "cuidador_id", nullable = false, unique = true)
    private PerfilCuidadorEntity cuidador;

    @Column(nullable = false, length = 1000)
    private String url;

    // Cuándo se leyó bien por última vez (null si todavía no)
    private LocalDateTime ultimaSincronizacion;

    // Por qué falló la última lectura (null si fue bien)
    @Column(length = 255)
    private String ultimoError;
}
