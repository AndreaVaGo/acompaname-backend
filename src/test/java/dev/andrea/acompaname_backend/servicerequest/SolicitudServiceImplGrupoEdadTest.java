package dev.andrea.acompaname_backend.servicerequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.globals.GrupoEdad;
import dev.andrea.acompaname_backend.payment.PagoRepository;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTORequest;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTOResponse;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
public class SolicitudServiceImplGrupoEdadTest {

    private SolicitudServiceImpl service;
    @Mock
    private SolicitudRepository repository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PerfilCuidadorRepository perfilCuidadorRepository;
    @Mock
    private PagoRepository pagoRepository;

    @BeforeEach
    void setup() {
        service = new SolicitudServiceImpl(repository, usuarioRepository, perfilCuidadorRepository, pagoRepository);
    }

    // Limpiamos el usuario logueado para no afectar a otros tests
    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private void loguearComo(String email) {
        Authentication auth = new UsernamePasswordAuthenticationToken(email, null);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private RoleEntity rol(Long id, String nombre) {
        RoleEntity rol = new RoleEntity();
        rol.setId(id);
        rol.setName(nombre);
        return rol;
    }

    private UsuarioEntity crearFamilia() {
        return new UsuarioEntity(1L, "Ana", "ana@test.com", "600111222", "1234", Set.of(rol(1L, "FAMILIA")));
    }

    private PerfilCuidadorEntity crearCuidador() {
        return new PerfilCuidadorEntity(1L, "Geriatría", 4, new java.math.BigDecimal("18.00"), "Bio", true, true,
                new UsuarioEntity(2L, "Pepe", "pepe@test.com", "600333444", "5678", Set.of(rol(2L, "CUIDADOR"))));
    }

    private SolicitudEntity crearSolicitudExistente() {
        return new SolicitudEntity(1L, "Acompañamiento", "Manuel", "Sin notas", 12, LocalDate.of(2026, 9, 10),
                EstadoSolicitud.PENDIENTE, crearFamilia(), crearCuidador());
    }

    // Petición de actualización con el grupo de edad que le pasemos (puede ser null)
    private SolicitudDTORequest crearRequest(GrupoEdad grupo) {
        return new SolicitudDTORequest("Acompañamiento", "Manuel", "Sin notas", 12, LocalDate.of(2026, 9, 10), null,
                1L, 1L, null, null, grupo);
    }

    @Test
    void testStoreEntityGuardaElGrupoDeEdad() {
        loguearComo("ana@test.com");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(crearFamilia()));
        when(perfilCuidadorRepository.findById(1L)).thenReturn(Optional.of(crearCuidador()));
        when(repository.save(Mockito.any(SolicitudEntity.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        SolicitudDTOResponse resultado = service.storeEntity(crearRequest(GrupoEdad.NINOS));

        ArgumentCaptor<SolicitudEntity> captor = ArgumentCaptor.forClass(SolicitudEntity.class);
        Mockito.verify(repository).save(captor.capture());
        assertThat(captor.getValue().getGrupoEdad(), is(equalTo(GrupoEdad.NINOS)));
        assertThat(resultado.grupoEdad(), is(equalTo(GrupoEdad.NINOS)));
    }

    @Test
    void testUpdateGuardaElGrupoDeEdad() {
        loguearComo("ana@test.com");
        SolicitudEntity solicitud = crearSolicitudExistente();
        when(repository.findById(1L)).thenReturn(Optional.of(solicitud));
        when(repository.save(Mockito.any(SolicitudEntity.class))).thenReturn(solicitud);

        SolicitudDTOResponse resultado = service.update(1L, crearRequest(GrupoEdad.ADOLESCENTES));

        assertThat(resultado.grupoEdad(), is(equalTo(GrupoEdad.ADOLESCENTES)));
    }

    @Test
    void testUpdateSinGrupoDeEdadMantieneElQueTenia() {
        loguearComo("ana@test.com");
        SolicitudEntity solicitud = crearSolicitudExistente();
        solicitud.setGrupoEdad(GrupoEdad.MAYORES);
        when(repository.findById(1L)).thenReturn(Optional.of(solicitud));
        when(repository.save(Mockito.any(SolicitudEntity.class))).thenReturn(solicitud);

        SolicitudDTOResponse resultado = service.update(1L, crearRequest(null));

        assertThat(resultado.grupoEdad(), is(equalTo(GrupoEdad.MAYORES)));
    }

    @Test
    void testElCuidadorVeElGrupoDeEdadAunqueNoVeLaDireccion() {
        loguearComo("pepe@test.com");
        SolicitudEntity solicitud = crearSolicitudExistente();
        solicitud.setDireccion("Calle Uría 10, Oviedo");
        solicitud.setGrupoEdad(GrupoEdad.BEBES);
        when(repository.findById(1L)).thenReturn(Optional.of(solicitud));

        SolicitudDTOResponse resultado = service.getById(1L);

        assertThat(resultado.direccion(), is(nullValue()));
        assertThat(resultado.grupoEdad(), is(equalTo(GrupoEdad.BEBES)));
    }
}