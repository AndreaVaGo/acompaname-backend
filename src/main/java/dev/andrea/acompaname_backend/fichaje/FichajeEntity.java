package dev.andrea.acompaname_backend.fichaje;

import java.time.LocalDateTime;

import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Un fichaje: cuándo empezó o terminó el servicio y si la persona cuidadora estaba en el lugar.
// Privacidad: NO guarda latitud ni longitud, solo la hora y el resultado (ResultadoUbicacion).
// Cada solicitud tiene como mucho un fichaje de inicio y uno de fin (la restricción única lo garantiza)
@Entity
@Table(name = "fichajes", uniqueConstraints = @UniqueConstraint(columnNames = { "solicitud_id", "tipo" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FichajeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "solicitud_id", nullable = false)
    private SolicitudEntity solicitud;

    @Enumerated(EnumType.STRING)
    private TipoFichaje tipo;

    private LocalDateTime momento;

    @Enumerated(EnumType.STRING)
    private ResultadoUbicacion ubicacion;
}
