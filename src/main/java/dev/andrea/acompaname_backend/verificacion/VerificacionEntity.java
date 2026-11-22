package dev.andrea.acompaname_backend.verificacion;

import java.time.LocalDateTime;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import jakarta.persistence.Column;
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

// Una prueba que la persona cuidadora envía para demostrar algo (su titulación o sus antecedentes).
// Solo hay una por persona y tipo. IMPORTANTE: no se guarda ningún documento, solo una referencia
// (código de verificación del Ministerio, número de colegiado...) que el administrador comprueba en la web oficial.
@Entity
@Table(name = "verificaciones", uniqueConstraints = @UniqueConstraint(columnNames = { "cuidador_id", "tipo" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Si se borra el perfil, se borran también sus verificaciones
    @ManyToOne(optional = false)
    @JoinColumn(name = "cuidador_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private PerfilCuidadorEntity cuidador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoVerificacion tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoVerificacion estado;

    // Lo que ha escrito la persona cuidadora (código de verificación, número de colegiado...)
    @Column(nullable = false, length = 100)
    private String referencia;

    @Column(nullable = false)
    private LocalDateTime solicitadaEn;

    // Cuándo la revisó el administrador (null mientras está pendiente)
    private LocalDateTime revisadaEn;

    // Email de quien la revisó (null mientras está pendiente)
    @Column(length = 100)
    private String revisadaPor;

    // Por qué se rechazó (solo si el estado es RECHAZADO)
    @Column(length = 200)
    private String motivoRechazo;
}
