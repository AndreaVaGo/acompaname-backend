package dev.andrea.acompaname_backend.pago;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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

import dev.andrea.acompaname_backend.pago.dtos.PagoDTORequest;
import dev.andrea.acompaname_backend.pago.dtos.PagoDTOResponse;
import dev.andrea.acompaname_backend.pago.exceptions.PagoExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.perfilcuidador.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.solicitud.EstadoSolicitud;
import dev.andrea.acompaname_backend.solicitud.SolicitudEntity;
import dev.andrea.acompaname_backend.solicitud.SolicitudRepository;
import dev.andrea.acompaname_backend.usuario.UsuarioEntity;

// Tests de seguridad del módulo de Pago: solo la familia y el cuidador de la
// solicitud pueden ver o tocar su pago, y solo la familia puede pagarlo (403).
@ExtendWith(MockitoExtension.class)
public class PagoServiceImplSeguridadTest {
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

    private UsuarioEntity usuario(Long id, String nombre, String email, RoleEntity rol) {
        return new UsuarioEntity(id, nombre, email, "600111222", "1234", Set.of(rol));
    }

    // Solicitud de Ana (familia) a Pepe (cuidador)
    private SolicitudEntity solicitudAnaPepe() {
        UsuarioEntity ana = usuario(1L, "Ana", "ana@test.com", rol(1L, "FAMILIA"));
        UsuarioEntity pepe = usuario(2L, "Pepe", "pepe@test.com", rol(2L, "CUIDADOR"));
        PerfilCuidadorEntity perfilPepe = new PerfilCuidadorEntity(1L, "Geriatría", 4, new BigDecimal("18.00"),
                "Bio", true, true, pepe);
        return new SolicitudEntity(1L, "Acompañamiento", "Manuel", "Sin notas", 80, LocalDate.of(2026, 9, 10),
                EstadoSolicitud.ACEPTADA, ana, perfilPepe);
    }

    // Solicitud de Luis (familia) a Marta (cuidadora), sin relación con Ana ni Pepe
    private SolicitudEntity solicitudLuisMarta() {
        UsuarioEntity luis = usuario(3L, "Luis", "luis@test.com", rol(1L, "FAMILIA"));
        UsuarioEntity marta = usuario(4L, "Marta", "marta@test.com", rol(2L, "CUIDADOR"));
        PerfilCuidadorEntity perfilMarta = new PerfilCuidadorEntity(2L, "Enfermería", 6, new BigDecimal("20.00"),
                "Bio", false, true, marta);
        return new SolicitudEntity(2L, "Hospital", "Carmen", "Sin notas", 75, LocalDate.of(2026, 9, 12),
                EstadoSolicitud.ACEPTADA, luis, perfilMarta);
    }

    private PagoEntity pago(Long id, SolicitudEntity solicitud) {
        return PagoEntity.builder()
                .id(id)
                .importe(new BigDecimal("90.00"))
                .estado(EstadoPago.PENDIENTE)
                .fecha(LocalDate.of(2026, 9, 11))
                .solicitud(solicitud)
                .build();
    }

    @Test
    void testGetEntitiesSoloDevuelveMisPagos() {
        loguearComo("ana@test.com");
        when(repository.findAll()).thenReturn(List.of(pago(1L, solicitudAnaPepe()), pago(2L, solicitudLuisMarta())));

        List<PagoDTOResponse> mios = service.getEntities();

        assertThat(mios.size(), is(equalTo(1)));
        assertThat(mios.get(0).id(), is(equalTo(1L)));
    }

    @Test
    void testGetByIdDeOtraPersonaDaAccesoDenegado() {
        loguearComo("intruso@test.com");
        when(repository.findById(1L)).thenReturn(Optional.of(pago(1L, solicitudAnaPepe())));

        assertThrows(PagoExceptionAccesoDenegado.class, () -> service.getById(1L));
    }

    @Test
    void testElCuidadorDeLaSolicitudPuedeVerElPago() {
        loguearComo("pepe@test.com");
        when(repository.findById(1L)).thenReturn(Optional.of(pago(1L, solicitudAnaPepe())));

        PagoDTOResponse resultado = service.getById(1L);

        assertThat(resultado.id(), is(equalTo(1L)));
    }

    @Test
    void testStoreEnSolicitudAjenaDaAccesoDenegadoYNoGuarda() {
        loguearComo("intruso@test.com");
        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitudAnaPepe()));

        assertThrows(PagoExceptionAccesoDenegado.class,
                () -> service.storeEntity(new PagoDTORequest(new BigDecimal("90.00"), 1L)));
        verify(repository, never()).save(Mockito.any(PagoEntity.class));
    }

    @Test
    void testUpdateDeOtraPersonaDaAccesoDenegadoYNoGuarda() {
        loguearComo("intruso@test.com");
        when(repository.findById(1L)).thenReturn(Optional.of(pago(1L, solicitudAnaPepe())));

        assertThrows(PagoExceptionAccesoDenegado.class,
                () -> service.update(1L, new PagoDTORequest(new BigDecimal("1.00"), 1L)));
        verify(repository, never()).save(Mockito.any(PagoEntity.class));
    }

    @Test
    void testDeleteDeOtraPersonaDaAccesoDenegadoYNoBorra() {
        loguearComo("intruso@test.com");
        when(repository.findById(1L)).thenReturn(Optional.of(pago(1L, solicitudAnaPepe())));

        assertThrows(PagoExceptionAccesoDenegado.class, () -> service.deleteById(1L));
        verify(repository, never()).deleteById(Mockito.anyLong());
    }

    @Test
    void testMarcarComoPagadoDeOtraPersonaDaAccesoDenegadoYNoGuarda() {
        loguearComo("intruso@test.com");
        when(repository.findById(1L)).thenReturn(Optional.of(pago(1L, solicitudAnaPepe())));

        assertThrows(PagoExceptionAccesoDenegado.class, () -> service.marcarComoPagado(1L));
        verify(repository, never()).save(Mockito.any(PagoEntity.class));
    }

    @Test
    void testElCuidadorNoPuedePagarSuPropioPago() {
        loguearComo("pepe@test.com");
        when(repository.findById(1L)).thenReturn(Optional.of(pago(1L, solicitudAnaPepe())));

        assertThrows(PagoExceptionAccesoDenegado.class, () -> service.marcarComoPagado(1L));
        verify(repository, never()).save(Mockito.any(PagoEntity.class));
    }
}