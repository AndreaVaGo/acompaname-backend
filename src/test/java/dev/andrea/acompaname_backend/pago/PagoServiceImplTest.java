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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import dev.andrea.acompaname_backend.pago.dtos.PagoDTORequest;
import dev.andrea.acompaname_backend.pago.dtos.PagoDTOResponse;
import dev.andrea.acompaname_backend.pago.exceptions.PagoExceptionNotFound;
import dev.andrea.acompaname_backend.perfilcuidador.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.solicitud.EstadoSolicitud;
import dev.andrea.acompaname_backend.solicitud.SolicitudEntity;
import dev.andrea.acompaname_backend.solicitud.SolicitudRepository;
import dev.andrea.acompaname_backend.solicitud.exceptions.SolicitudExceptionNotFound;
import dev.andrea.acompaname_backend.usuario.UsuarioEntity;

@ExtendWith(MockitoExtension.class)
public class PagoServiceImplTest {
    @InjectMocks
    private PagoServiceImpl service;
    @Mock
    private PagoRepository repository;
    @Mock
    private SolicitudRepository solicitudRepository;

    private SecurityContext contextoOriginal;

    @BeforeEach
    void setup() {
        // Guardo el contexto de seguridad que hubiera, entro como Ana (la familia
        // de los pagos de prueba) y al terminar lo devuelvo.
        contextoOriginal = SecurityContextHolder.getContext();
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(new UsernamePasswordAuthenticationToken("ana@test.com", null));
        SecurityContextHolder.setContext(contexto);
        service = new PagoServiceImpl(repository, solicitudRepository);
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.setContext(contextoOriginal);
    }

    private RoleEntity rolFamilia() {
        RoleEntity rol = new RoleEntity();
        rol.setId(1L);
        rol.setName("FAMILIA");
        return rol;
    }

    private RoleEntity rolCuidador() {
        RoleEntity rol = new RoleEntity();
        rol.setId(2L);
        rol.setName("CUIDADOR");
        return rol;
    }

    private SolicitudEntity crearSolicitudMock() {
        UsuarioEntity familia = new UsuarioEntity(1L, "Ana", "ana@test.com", "600111222", "1234",
                Set.of(rolFamilia()));
        PerfilCuidadorEntity cuidador = new PerfilCuidadorEntity(1L, "Geriatría", 4, new BigDecimal("18.00"),
                "Bio", true, true, new UsuarioEntity(2L, "Pepe", "pepe@test.com", "600333444", "5678",
                        Set.of(rolCuidador())));
        return new SolicitudEntity(1L, "Acompañamiento", "Manuel", "Sin notas", 80, LocalDate.of(2026, 9, 10),
                EstadoSolicitud.COMPLETADA, familia, cuidador);
    }

    private PagoEntity crearPagoMock(EstadoPago estado) {
        return PagoEntity.builder()
                .id(1L)
                .importe(new BigDecimal("90.00"))
                .estado(estado)
                .fecha(LocalDate.of(2026, 9, 11))
                .solicitud(crearSolicitudMock())
                .build();
    }

    @Test
    void testGetEntities() {
        when(repository.findAll()).thenReturn(List.of(crearPagoMock(EstadoPago.PENDIENTE)));

        List<PagoDTOResponse> pagos = service.getEntities();

        assertThat(pagos.size(), is(equalTo(1)));
        assertThat(pagos.get(0).importe(), is(equalTo(new BigDecimal("90.00"))));
    }

    @Test
    void testGetById() {
        when(repository.findById(1L)).thenReturn(Optional.of(crearPagoMock(EstadoPago.PENDIENTE)));

        PagoDTOResponse pago = service.getById(1L);

        assertThat(pago.id(), is(equalTo(1L)));
        assertThat(pago.estado(), is(equalTo(EstadoPago.PENDIENTE)));
        assertThat(pago.solicitudId(), is(equalTo(1L)));
    }

    @Test
    void testGetByIdNoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(PagoExceptionNotFound.class, () -> service.getById(99L));
    }

    @Test
    void testStoreEntityCreaElPagoComoPendiente() {
        SolicitudEntity solicitud = crearSolicitudMock();
        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitud));
        when(repository.save(Mockito.any(PagoEntity.class))).thenAnswer(invocation -> {
            PagoEntity pago = invocation.getArgument(0);
            pago.setId(1L);
            return pago;
        });

        PagoDTOResponse pago = service.storeEntity(new PagoDTORequest(new BigDecimal("90.00"), 1L));

        assertThat(pago.estado(), is(equalTo(EstadoPago.PENDIENTE)));
        assertThat(pago.importe(), is(equalTo(new BigDecimal("90.00"))));
        assertThat(pago.solicitudId(), is(equalTo(1L)));
    }

    @Test
    void testStoreEntitySolicitudNoExiste() {
        when(solicitudRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(SolicitudExceptionNotFound.class,
                () -> service.storeEntity(new PagoDTORequest(new BigDecimal("90.00"), 99L)));
        verify(repository, never()).save(Mockito.any(PagoEntity.class));
    }

    @Test
    void testDeleteById() {
        when(repository.findById(1L)).thenReturn(Optional.of(crearPagoMock(EstadoPago.PENDIENTE)));

        service.deleteById(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void testDeleteByIdNoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(PagoExceptionNotFound.class, () -> service.deleteById(99L));
        verify(repository, never()).deleteById(99L);
    }

    @Test
    void testUpdate() {
        PagoEntity pagoExistente = crearPagoMock(EstadoPago.PENDIENTE);
        when(repository.findById(1L)).thenReturn(Optional.of(pagoExistente));
        when(repository.save(Mockito.any(PagoEntity.class))).thenReturn(pagoExistente);

        PagoDTOResponse resultado = service.update(1L, new PagoDTORequest(new BigDecimal("120.00"), 1L));

        assertThat(resultado.importe(), is(equalTo(new BigDecimal("120.00"))));
    }

    @Test
    void testUpdateNoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(PagoExceptionNotFound.class,
                () -> service.update(99L, new PagoDTORequest(new BigDecimal("120.00"), 1L)));
    }

    @Test
    void testMarcarComoPagado() {
        PagoEntity pagoPendiente = crearPagoMock(EstadoPago.PENDIENTE);
        when(repository.findById(1L)).thenReturn(Optional.of(pagoPendiente));
        when(repository.save(Mockito.any(PagoEntity.class))).thenReturn(pagoPendiente);

        PagoDTOResponse resultado = service.marcarComoPagado(1L);

        assertThat(resultado.estado(), is(equalTo(EstadoPago.COMPLETADO)));
    }

    @Test
    void testMarcarComoPagadoNoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(PagoExceptionNotFound.class, () -> service.marcarComoPagado(99L));
        verify(repository, never()).save(Mockito.any(PagoEntity.class));
    }
}