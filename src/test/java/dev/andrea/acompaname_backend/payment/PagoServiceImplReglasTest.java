package dev.andrea.acompaname_backend.payment;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.payment.exceptions.PagoExceptionConflicto;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.servicerequest.EstadoSolicitud;
import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;
import dev.andrea.acompaname_backend.user.UsuarioEntity;

// Reglas de negocio del pago: el pago se crea solo al completar la solicitud
// (no desde la API) y solo se puede pagar una vez, mientras está pendiente.
@ExtendWith(MockitoExtension.class)
public class PagoServiceImplReglasTest {
    @Mock
    private PagoRepository repository;

    private PagoServiceImpl service;
    private SecurityContext contextoOriginal;

    @BeforeEach
    void setup() {
        contextoOriginal = SecurityContextHolder.getContext();
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        service = new PagoServiceImpl(repository);
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.setContext(contextoOriginal);
    }

    private void loguearComo(String email) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(email, null));
    }

    private RoleEntity rol(Long id, String nombre) {
        RoleEntity rol = new RoleEntity();
        rol.setId(id);
        rol.setName(nombre);
        return rol;
    }

    // Solicitud completada de Ana (familia) a Pepe (cuidador)
    private SolicitudEntity solicitudAnaPepe() {
        UsuarioEntity ana = new UsuarioEntity(1L, "Ana", "ana@test.com", "600111222", "1234",
                Set.of(rol(1L, "FAMILIA")));
        UsuarioEntity pepe = new UsuarioEntity(2L, "Pepe", "pepe@test.com", "600333444", "5678",
                Set.of(rol(2L, "CUIDADOR")));
        PerfilCuidadorEntity perfilPepe = new PerfilCuidadorEntity(1L, "Geriatría", 4, new BigDecimal("18.00"),
                "Bio", true, true, pepe);
        return new SolicitudEntity(1L, "Acompañamiento", "Manuel", "Sin notas", 80, LocalDate.of(2026, 9, 10),
                EstadoSolicitud.COMPLETADA, ana, perfilPepe);
    }

    private PagoEntity pago(EstadoPago estado) {
        return PagoEntity.builder()
                .id(1L)
                .importe(new BigDecimal("90.00"))
                .estado(estado)
                .fecha(LocalDate.of(2026, 9, 11))
                .solicitud(solicitudAnaPepe())
                .build();
    }

    @Test
    void testNoSePuedePagarDosVeces() {
        loguearComo("ana@test.com");
        when(repository.findById(1L)).thenReturn(Optional.of(pago(EstadoPago.COMPLETADO)));

        assertThrows(PagoExceptionConflicto.class, () -> service.marcarComoPagado(1L));
        verify(repository, never()).save(Mockito.any(PagoEntity.class));
    }

    @Test
    void testNoSePuedePagarUnPagoCancelado() {
        loguearComo("ana@test.com");
        when(repository.findById(1L)).thenReturn(Optional.of(pago(EstadoPago.CANCELADO)));

        assertThrows(PagoExceptionConflicto.class, () -> service.marcarComoPagado(1L));
        verify(repository, never()).save(Mockito.any(PagoEntity.class));
    }
}