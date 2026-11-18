package dev.andrea.acompaname_backend.schedule;

import java.time.LocalDate;
import java.time.LocalTime;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
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

// Un rato en el que la persona cuidadora NO está disponible por otros motivos
// (su turno en el hospital, una cita médica...). Es de un solo día.
@Entity
@Table(name = "bloques_ocupados")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BloqueOcupadoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cuidador_id")
    private PerfilCuidadorEntity cuidador;

    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;

    // Para qué es (opcional, por ejemplo "Turno en el hospital")
    @Column(length = 100)
    private String motivo;
}
