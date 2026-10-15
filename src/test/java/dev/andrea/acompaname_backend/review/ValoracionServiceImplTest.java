package dev.andrea.acompaname_backend.review;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.servicerequest.EstadoSolicitud;
import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;
import dev.andrea.acompaname_backend.servicerequest.SolicitudRepository;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.review.dtos.ValoracionDTORequest;
import dev.andrea.acompaname_backend.review.dtos.ValoracionDTOResponse;
import dev.andrea.acompaname_backend.review.exceptions.ValoracionExceptionAccesoDenegado;

@ExtendWith(MockitoExtension.class)
public class ValoracionServiceImplTest {
    @InjectMocks
    private ValoracionServiceImpl service;
    @Mock
    private ValoracionRepository repository;
    @Mock
    private SolicitudRepository solicitudRepository;

    @BeforeEach
    void setup() {
        service = new ValoracionServiceImpl(repository, solicitudRepository);
    }

    // Limpia el usuario logueado para que un test no afecte a los siguientes
    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    private void loguearComo(String email) {
        Authentication auth = new UsernamePasswordAuthenticationToken(email, null);
        SecurityContextHolder.getContext().setAuthentication(auth);
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
        PerfilCuidadorEntity cuidador = new PerfilCuidadorEntity(1L, "Geriatría", 4, new java.math.BigDecimal("18.00"),
                "Bio", true, true, new UsuarioEntity(2L, "Pepe", "pepe@test.com", "600333444", "5678",
                        Set.of(rolCuidador())));
        return new SolicitudEntity(1L, "Acompañamiento", "Manuel", "Sin notas", 80, LocalDate.of(2026, 9, 10),
                EstadoSolicitud.COMPLETADA, familia, cuidador);
    }

    @Test
    void testGetEntities() {
        loguearComo("ana@test.com");
        SolicitudEntity solicitud = crearSolicitudMock();
        List<ValoracionEntity> valoracionesMock = List.of(
                new ValoracionEntity(1L, "Muy buena atención", 5, LocalDate.of(2026, 9, 11), solicitud));
        when(repository.findBySolicitudFamiliaEmail("ana@test.com")).thenReturn(valoracionesMock);

        List<ValoracionDTOResponse> valoraciones = service.getEntities();

        assertThat(valoraciones.size(), is(equalTo(1)));
        assertThat(valoraciones.get(0).comentario(), is(equalTo("Muy buena atención")));
    }

    @Test
    void testGetEntitiesComoCuidadorDevuelveLasRecibidas() {
        loguearComo("pepe@test.com");
        SolicitudEntity solicitud = crearSolicitudMock();
        List<ValoracionEntity> valoracionesMock = List.of(
                new ValoracionEntity(1L, "Muy buena atención", 5, LocalDate.of(2026, 9, 11), solicitud));
        when(repository.findBySolicitudCuidadorUsuarioEmail("pepe@test.com")).thenReturn(valoracionesMock);

        List<ValoracionDTOResponse> valoraciones = service.getEntities();

        assertThat(valoraciones.size(), is(equalTo(1)));
        assertThat(valoraciones.get(0).puntuacion(), is(equalTo(5)));
    }

    @Test
    void testGetEntitiesDeUsuarioSinValoracionesDevuelveListaVacia() {
        loguearComo("otro@test.com");

        List<ValoracionDTOResponse> valoraciones = service.getEntities();

        assertThat(valoraciones.size(), is(equalTo(0)));
        // Nunca se piden todas las valoraciones de la base de datos
        Mockito.verify(repository, Mockito.never()).findAll();
    }

    @Test
    void testGetById() {
        loguearComo("ana@test.com");
        SolicitudEntity solicitud = crearSolicitudMock();
        ValoracionEntity valoracionMock = new ValoracionEntity(1L, "Muy buena atención", 5,
                LocalDate.of(2026, 9, 11), solicitud);
        when(repository.findById(1L)).thenReturn(Optional.of(valoracionMock));

        ValoracionDTOResponse valoracion = service.getById(1L);

        assertThat(valoracion.comentario(), is(equalTo("Muy buena atención")));
        assertThat(valoracion.puntuacion(), is(equalTo(5)));
    }

    @Test
    void testGetByIdComoCuidadorDeLaSolicitud() {
        loguearComo("pepe@test.com");
        SolicitudEntity solicitud = crearSolicitudMock();
        ValoracionEntity valoracionMock = new ValoracionEntity(1L, "Muy buena atención", 5,
                LocalDate.of(2026, 9, 11), solicitud);
        when(repository.findById(1L)).thenReturn(Optional.of(valoracionMock));

        ValoracionDTOResponse valoracion = service.getById(1L);

        assertThat(valoracion.puntuacion(), is(equalTo(5)));
    }

    @Test
    void testGetByIdDeOtroUsuarioLanzaAccesoDenegado() {
        loguearComo("otro@test.com");
        SolicitudEntity solicitud = crearSolicitudMock();
        ValoracionEntity valoracionMock = new ValoracionEntity(1L, "Muy buena atención", 5,
                LocalDate.of(2026, 9, 11), solicitud);
        when(repository.findById(1L)).thenReturn(Optional.of(valoracionMock));

        assertThrows(ValoracionExceptionAccesoDenegado.class, () -> service.getById(1L));
    }

    @Test
    void testStoreEntity() {
        loguearComo("ana@test.com");

        SolicitudEntity solicitud = crearSolicitudMock();
        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitud));

        ValoracionDTORequest dto = new ValoracionDTORequest("Excelente trato", 5, LocalDate.of(2026, 9, 12), 1L);
        when(repository.save(Mockito.any(ValoracionEntity.class))).thenReturn(
                new ValoracionEntity(1L, dto.comentario(), dto.puntuacion(), dto.fecha(), solicitud));

        ValoracionDTOResponse entity = service.storeEntity(dto);

        assertThat(entity.comentario(), is(equalTo("Excelente trato")));
        assertThat(entity.puntuacion(), is(equalTo(5)));
    }

    @Test
    void testDeleteById() {
        loguearComo("ana@test.com");

        SolicitudEntity solicitud = crearSolicitudMock();
        ValoracionEntity valoracionMock = new ValoracionEntity(1L, "Muy buena atención", 5,
                LocalDate.of(2026, 9, 11), solicitud);
        when(repository.findById(1L)).thenReturn(Optional.of(valoracionMock));

        service.deleteById(1L);

        Mockito.verify(repository).deleteById(1L);
    }

    @Test
    void testUpdate() {
        loguearComo("ana@test.com");

        SolicitudEntity solicitud = crearSolicitudMock();
        ValoracionEntity valoracionExistente = new ValoracionEntity(1L, "Muy buena atención", 5,
                LocalDate.of(2026, 9, 11), solicitud);
        when(repository.findById(1L)).thenReturn(Optional.of(valoracionExistente));
        when(repository.save(Mockito.any(ValoracionEntity.class))).thenReturn(valoracionExistente);

        ValoracionDTORequest dto = new ValoracionDTORequest("Comentario actualizado", 4, LocalDate.of(2026, 9, 13), 1L);
        ValoracionDTOResponse resultado = service.update(1L, dto);

        assertThat(resultado.comentario(), is(equalTo("Comentario actualizado")));
        assertThat(resultado.puntuacion(), is(equalTo(4)));
    }
}