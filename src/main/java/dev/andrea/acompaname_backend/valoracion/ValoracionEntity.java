package dev.andrea.acompaname_backend.valoracion;

import java.time.LocalDate;

import dev.andrea.acompaname_backend.solicitud.SolicitudEntity;
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

@Entity
@Table(name = "valoraciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValoracionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String comentario;
    private Integer puntuacion;
    private LocalDate fecha;

    @OneToOne
    @JoinColumn(name = "solicitud_id")
    private SolicitudEntity solicitud;
}