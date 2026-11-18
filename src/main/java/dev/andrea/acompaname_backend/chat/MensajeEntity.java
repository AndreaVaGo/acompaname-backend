package dev.andrea.acompaname_backend.chat;

import java.time.LocalDateTime;

import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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

// Un mensaje del chat de una solicitud, escrito por la familia o por la persona cuidadora
@Entity
@Table(name = "mensajes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MensajeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_id")
    private SolicitudEntity solicitud;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emisor_id")
    private UsuarioEntity emisor;

    @Column(nullable = false, length = 1000)
    private String texto;

    // La pone el servidor al guardar el mensaje
    @Column(nullable = false)
    private LocalDateTime fecha;

    // false hasta que la otra persona abre el chat y lo ve.
    // El valor por defecto es para los mensajes que ya existían antes de añadir este campo: se dan por leídos
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean leido;
}
