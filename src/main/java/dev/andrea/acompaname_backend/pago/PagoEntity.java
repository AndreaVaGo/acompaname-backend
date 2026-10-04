package dev.andrea.acompaname_backend.pago;

import java.math.BigDecimal;
import java.time.LocalDate;

import dev.andrea.acompaname_backend.solicitud.SolicitudEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "pagos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private BigDecimal importe;
    @Enumerated(EnumType.STRING)
    private EstadoPago estado;
    private LocalDate fecha;

    @OneToOne
    @JoinColumn(name = "solicitud_id")
    private SolicitudEntity solicitud;
}