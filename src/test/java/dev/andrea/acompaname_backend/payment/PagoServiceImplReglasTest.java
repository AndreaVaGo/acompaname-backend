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
import dev.andrea.acompaname_backend.payment.dtos.PagoDTORequest;
import dev.andrea.acompaname_backend.payment.exceptions.PagoExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.payment.exceptions.PagoExceptionConflicto;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.servicerequest.EstadoSolicitud;
import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;
import dev.andrea.acompaname_backend.servicerequest.SolicitudRepository;
import dev.andrea.acompaname_backend.user.UsuarioEntity;

// Reglas de negocio del pago: solo la familia lo crea, lo cambia o lo borra,
// solo mientras está pendiente, y solo hay un pago por solicitud.
@ExtendWith(MockitoExtension.class)
public class PagoServiceImplReglasTest {
    @Mock
    private PagoRepository repository;
    @Mock
    private SolicitudRepository solicitudRepository;

    private PagoServiceImpl service;
    private SecurityContext contextoOriginal;

    @BeforeEach
    void setup() {
        contextoOriginal = SecurityContextHolder.getContext();
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        service = new PagoServiceImpl(repository, solicitudRepository);
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

    // Solicitud de Ana (familia) a Pepe (cuidador) con el estado que se pida
    private SolicitudEntity solicitudAnaPepe(EstadoSolicitud estado) {
        UsuarioEntity ana = new UsuarioEntity(1L, "Ana", "ana@test.com", "600111222", "1234",
                Set.of(rol(1L, "FAMILIA")));
        UsuarioEntity pepe = new UsuarioEntity(2L, "Pepe", "pepe@test.com", "600333444", "5678",
                Set.of(rol(2L, "CUIDADOR")));
        PerfilCuidadorEntity perfilPepe = new PerfilCuidadorEntity(1L, "Geriatría", 4, new BigDecimal("18.00"),
                "Bio", true, true, pepe);
        return new SolicitudEntity(1L, "Acompañamiento", "Manuel", "Sin notas", 80, LocalDate.of(2026, 9, 10),
                estado, ana, perfilPepe);
    }

    private PagoEntity pago(EstadoPago estado) {
        return PagoEntity.builder()
                .id(1L)
                .importe(new BigDecimal("90.00"))
                .estado(estado)
                .fecha(LocalDate.of(2026, 9, 11))
                .solicitud(solicitudAnaPepe(EstadoSolicitud.ACEPTADA))
                .build();
    }

    @Test
    void testElCuidadorNoPuedeCrearElPago() {
        loguearComo("pepe@test.com");
        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitudAnaPepe(EstadoSolicitud.ACEPTADA)));

        assertThrows(PagoExceptionAccesoDenegado.class,
                () -> service.storeEntity(new PagoDTORequest(new BigDecimal("90.00"), 1L)));
        verify(repository, never()).save(Mockito.any(PagoEntity.class));
    }

    @Test
    void testNoSeCreaElPagoDeUnaSolicitudPendiente() {
        loguearComo("ana@test.com");
        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitudAnaPepe(EstadoSolicitud.PENDIENTE)));

        assertThrows(PagoExceptionConflicto.class,
                () -> service.storeEntity(new PagoDTORequest(new BigDecimal("90.00"), 1L)));
        verify(repository, never()).save(Mockito.any(PagoEntity.class));
    }

    @Test
    void testNoSeCreaUnSegundoPagoDeLaMismaSolicitud() {
        loguearComo("ana@test.com");
        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitudAnaPepe(EstadoSolicitud.ACEPTADA)));
        when(repository.existsBySolicitudId(1L)).thenReturn(true);

        assertThrows(PagoExceptionConflicto.class,
                () -> service.storeEntity(new PagoDTORequest(new BigDecimal("90.00"), 1L)));
        verify(repository, never()).save(Mockito.any(PagoEntity.class));
    }

    @Test
    void testNoSePuedePagarDosVeces() {
        loguearComo("ana@test.com");
        when(repository.findById(1L)).thenReturn(Optional.of(pago(EstadoPago.COMPLETADO)));

        assertThrows(PagoExceptionConflicto.class, () -> service.marcarComoPagado(1L));
        verify(repository, never()).save(Mockito.any(PagoEntity.class));
    }

    @Test
    void testNoSePuedeCambiarElImporteDeUnPagoYaPagado() {
        loguearComo("ana@test.com");
        when(repository.findById(1L)).thenReturn(Optional.of(pago(EstadoPago.COMPLETADO)));

        assertThrows(PagoExceptionConflicto.class,
                () -> service.update(1L, new PagoDTORequest(new BigDecimal("1.00"), 1L)));
        verify(repository, never()).save(Mockito.any(PagoEntity.class));
    }

    @Test
    void testElCuidadorNoPuedeCambiarElImporte() {
        loguearComo("pepe@test.com");
        when(repository.findById(1L)).thenReturn(Optional.of(pago(EstadoPago.PENDIENTE)));

        assertThrows(PagoExceptionAccesoDenegado.class,
                () -> service.update(1L, new PagoDTORequest(new BigDecimal("1.00"), 1L)));
        verify(repository, never()).save(Mockito.any(PagoEntity.class));
    }

    @Test
    void testNoSePuedeBorrarUnPagoYaPagado() {
        loguearComo("ana@test.com");
        when(repository.findById(1L)).thenReturn(Optional.of(pago(EstadoPago.COMPLETADO)));

        assertThrows(PagoExceptionConflicto.class, () -> service.deleteById(1L));
        verify(repository, never()).deleteById(Mockito.anyLong());
    }

    @Test
    void testElCuidadorNoPuedeBorrarElPago() {
        loguearComo("pepe@test.com");
        when(repository.findById(1L)).thenReturn(Optional.of(pago(EstadoPago.PENDIENTE)));

        assertThrows(PagoExceptionAccesoDenegado.class, () -> service.deleteById(1L));
        verify(repository, never()).deleteById(Mockito.anyLong());
    }
}