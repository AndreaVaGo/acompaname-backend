package dev.andrea.acompaname_backend.verificacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.caregiverprofile.exceptions.PerfilCuidadorExceptionNotFound;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.verificacion.dtos.AdminVerificacionDTOResponse;
import dev.andrea.acompaname_backend.verificacion.dtos.VerificacionDTOResponse;
import dev.andrea.acompaname_backend.verificacion.dtos.VerificadosDTOResponse;

@ExtendWith(MockitoExtension.class)
public class VerificacionServiceImplTest {

    @Mock
    private VerificacionRepository repository;
    @Mock
    private PerfilCuidadorRepository perfilRepository;

    private VerificacionServiceImpl service;

    @BeforeEach
    void setup() {
        service = new VerificacionServiceImpl(repository, perfilRepository);
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    private void loguearComo(String email) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(email, null));
    }

    private PerfilCuidadorEntity crearPerfil(Long id, String email) {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setEmail(email);
        usuario.setNombre("Pepe");
        PerfilCuidadorEntity perfil = new PerfilCuidadorEntity();
        perfil.setId(id);
        perfil.setUsuario(usuario);
        return perfil;
    }

    private VerificacionEntity crearVerificacion(Long id, PerfilCuidadorEntity perfil, TipoVerificacion tipo,
            EstadoVerificacion estado) {
        return VerificacionEntity.builder()
                .id(id)
                .cuidador(perfil)
                .tipo(tipo)
                .estado(estado)
                .referencia("SD:ABC123")
                .solicitadaEn(LocalDateTime.of(2026, 11, 22, 10, 0))
                .build();
    }

    // ---------- listarMias ----------

    @Test
    void testListarMiasSinPerfilDevuelveListaVacia() {
        loguearComo("pepe@test.com");
        when(perfilRepository.findByUsuarioEmail("pepe@test.com")).thenReturn(Optional.empty());

        assertThat(service.listarMias(), is(empty()));
    }

    @Test
    void testListarMiasDevuelveLasDeSuPerfil() {
        loguearComo("pepe@test.com");
        PerfilCuidadorEntity perfil = crearPerfil(1L, "pepe@test.com");
        when(perfilRepository.findByUsuarioEmail("pepe@test.com")).thenReturn(Optional.of(perfil));
        when(repository.findByCuidadorId(1L)).thenReturn(
                List.of(crearVerificacion(5L, perfil, TipoVerificacion.TITULACION, EstadoVerificacion.PENDIENTE)));

        List<VerificacionDTOResponse> lista = service.listarMias();

        assertThat(lista.size(), is(equalTo(1)));
        assertThat(lista.get(0).tipo(), is(equalTo(TipoVerificacion.TITULACION)));
        assertThat(lista.get(0).estado(), is(equalTo(EstadoVerificacion.PENDIENTE)));
    }

    // ---------- enviar ----------

    @Test
    void testEnviarSinPerfilLanzaNotFound() {
        loguearComo("pepe@test.com");
        when(perfilRepository.findByUsuarioEmail("pepe@test.com")).thenReturn(Optional.empty());

        assertThrows(PerfilCuidadorExceptionNotFound.class,
                () -> service.enviar(TipoVerificacion.TITULACION, "12345"));
        verify(repository, never()).save(any());
    }

    @Test
    void testEnviarCreaUnaVerificacionPendiente() {
        loguearComo("pepe@test.com");
        PerfilCuidadorEntity perfil = crearPerfil(1L, "pepe@test.com");
        when(perfilRepository.findByUsuarioEmail("pepe@test.com")).thenReturn(Optional.of(perfil));
        when(repository.findByCuidadorIdAndTipo(1L, TipoVerificacion.ANTECEDENTES)).thenReturn(Optional.empty());
        when(repository.save(any(VerificacionEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        VerificacionDTOResponse respuesta = service.enviar(TipoVerificacion.ANTECEDENTES, "  SD:ABC123  ");

        assertThat(respuesta.estado(), is(equalTo(EstadoVerificacion.PENDIENTE)));
        // La referencia se guarda sin espacios sobrantes
        assertThat(respuesta.referencia(), is(equalTo("SD:ABC123")));
        assertThat(respuesta.tipo(), is(equalTo(TipoVerificacion.ANTECEDENTES)));
    }

    @Test
    void testEnviarDeNuevoUnaRechazadaLaDejaPendienteYLimpiaElMotivo() {
        loguearComo("pepe@test.com");
        PerfilCuidadorEntity perfil = crearPerfil(1L, "pepe@test.com");
        VerificacionEntity rechazada = crearVerificacion(7L, perfil, TipoVerificacion.TITULACION,
                EstadoVerificacion.RECHAZADO);
        rechazada.setMotivoRechazo("No coincide");
        rechazada.setRevisadaPor("admin@test.com");
        rechazada.setRevisadaEn(LocalDateTime.now());
        when(perfilRepository.findByUsuarioEmail("pepe@test.com")).thenReturn(Optional.of(perfil));
        when(repository.findByCuidadorIdAndTipo(1L, TipoVerificacion.TITULACION)).thenReturn(Optional.of(rechazada));
        when(repository.save(any(VerificacionEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        VerificacionDTOResponse respuesta = service.enviar(TipoVerificacion.TITULACION, "NUEVO-999");

        assertThat(respuesta.id(), is(equalTo(7L)));
        assertThat(respuesta.estado(), is(equalTo(EstadoVerificacion.PENDIENTE)));
        assertThat(respuesta.motivoRechazo(), is(nullValue()));
        assertThat(respuesta.revisadaEn(), is(nullValue()));
        assertThat(rechazada.getRevisadaPor(), is(nullValue()));
    }

    @Test
    void testEnviarUnaYaVerificadaLanzaExcepcion() {
        loguearComo("pepe@test.com");
        PerfilCuidadorEntity perfil = crearPerfil(1L, "pepe@test.com");
        VerificacionEntity verificada = crearVerificacion(7L, perfil, TipoVerificacion.TITULACION,
                EstadoVerificacion.VERIFICADO);
        when(perfilRepository.findByUsuarioEmail("pepe@test.com")).thenReturn(Optional.of(perfil));
        when(repository.findByCuidadorIdAndTipo(1L, TipoVerificacion.TITULACION)).thenReturn(Optional.of(verificada));

        assertThrows(VerificacionException.class, () -> service.enviar(TipoVerificacion.TITULACION, "OTRA"));
        verify(repository, never()).save(any());
    }

    // ---------- publicas ----------

    @Test
    void testPublicasAgrupaPorCuidadorSinEnseñarReferencias() {
        PerfilCuidadorEntity pepe = crearPerfil(1L, "pepe@test.com");
        PerfilCuidadorEntity lucia = crearPerfil(2L, "lucia@test.com");
        when(repository.findByEstadoOrderBySolicitadaEnAsc(EstadoVerificacion.VERIFICADO)).thenReturn(List.of(
                crearVerificacion(1L, pepe, TipoVerificacion.TITULACION, EstadoVerificacion.VERIFICADO),
                crearVerificacion(2L, pepe, TipoVerificacion.ANTECEDENTES, EstadoVerificacion.VERIFICADO),
                crearVerificacion(3L, lucia, TipoVerificacion.ANTECEDENTES, EstadoVerificacion.VERIFICADO)));

        List<VerificadosDTOResponse> lista = service.publicas();

        assertThat(lista.size(), is(equalTo(2)));
        assertThat(lista.get(0), is(equalTo(new VerificadosDTOResponse(1L, true, true))));
        assertThat(lista.get(1), is(equalTo(new VerificadosDTOResponse(2L, false, true))));
    }

    @Test
    void testPublicasSinNingunaVerificadaDevuelveListaVacia() {
        when(repository.findByEstadoOrderBySolicitadaEnAsc(EstadoVerificacion.VERIFICADO)).thenReturn(List.of());

        assertThat(service.publicas(), is(empty()));
    }

    // ---------- listarParaAdmin ----------

    @Test
    void testListarParaAdminSinEstadoDevuelveTodas() {
        PerfilCuidadorEntity pepe = crearPerfil(1L, "pepe@test.com");
        when(repository.findAllByOrderBySolicitadaEnAsc()).thenReturn(
                List.of(crearVerificacion(1L, pepe, TipoVerificacion.TITULACION, EstadoVerificacion.PENDIENTE)));

        List<AdminVerificacionDTOResponse> lista = service.listarParaAdmin(null);

        assertThat(lista.size(), is(equalTo(1)));
        assertThat(lista.get(0).cuidadorNombre(), is(equalTo("Pepe")));
        assertThat(lista.get(0).cuidadorId(), is(equalTo(1L)));
        verify(repository, never()).findByEstadoOrderBySolicitadaEnAsc(any());
    }

    @Test
    void testListarParaAdminConEstadoFiltra() {
        PerfilCuidadorEntity pepe = crearPerfil(1L, "pepe@test.com");
        when(repository.findByEstadoOrderBySolicitadaEnAsc(EstadoVerificacion.PENDIENTE)).thenReturn(
                List.of(crearVerificacion(1L, pepe, TipoVerificacion.ANTECEDENTES, EstadoVerificacion.PENDIENTE)));

        List<AdminVerificacionDTOResponse> lista = service.listarParaAdmin(EstadoVerificacion.PENDIENTE);

        assertThat(lista.size(), is(equalTo(1)));
        verify(repository, never()).findAllByOrderBySolicitadaEnAsc();
    }

    // ---------- resolver ----------

    @Test
    void testResolverUnaQueNoExisteLanzaExcepcion() {
        loguearComo("admin@test.com");
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(VerificacionException.class, () -> service.resolver(99L, true, null));
    }

    @Test
    void testResolverUnaYaRevisadaLanzaExcepcion() {
        loguearComo("admin@test.com");
        PerfilCuidadorEntity pepe = crearPerfil(1L, "pepe@test.com");
        VerificacionEntity hecha = crearVerificacion(1L, pepe, TipoVerificacion.TITULACION,
                EstadoVerificacion.VERIFICADO);
        when(repository.findById(1L)).thenReturn(Optional.of(hecha));

        assertThrows(VerificacionException.class, () -> service.resolver(1L, false, "tarde"));
        verify(repository, never()).save(any());
    }

    @Test
    void testAprobarMarcaVerificadaConQuienLaRevisoYSinMotivo() {
        loguearComo("admin@test.com");
        PerfilCuidadorEntity pepe = crearPerfil(1L, "pepe@test.com");
        VerificacionEntity pendiente = crearVerificacion(1L, pepe, TipoVerificacion.TITULACION,
                EstadoVerificacion.PENDIENTE);
        when(repository.findById(1L)).thenReturn(Optional.of(pendiente));
        when(repository.save(any(VerificacionEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // Aunque llegue un motivo, al aprobar no se guarda
        AdminVerificacionDTOResponse respuesta = service.resolver(1L, true, "no hace falta");

        assertThat(respuesta.estado(), is(equalTo(EstadoVerificacion.VERIFICADO)));
        assertThat(respuesta.motivoRechazo(), is(nullValue()));
        assertThat(respuesta.revisadaEn() != null, is(true));
        assertThat(pendiente.getRevisadaPor(), is(equalTo("admin@test.com")));
    }

    @Test
    void testRechazarGuardaElMotivoSinEspacios() {
        loguearComo("admin@test.com");
        PerfilCuidadorEntity pepe = crearPerfil(1L, "pepe@test.com");
        VerificacionEntity pendiente = crearVerificacion(1L, pepe, TipoVerificacion.ANTECEDENTES,
                EstadoVerificacion.PENDIENTE);
        when(repository.findById(1L)).thenReturn(Optional.of(pendiente));
        when(repository.save(any(VerificacionEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminVerificacionDTOResponse respuesta = service.resolver(1L, false, "  El código no existe  ");

        assertThat(respuesta.estado(), is(equalTo(EstadoVerificacion.RECHAZADO)));
        assertThat(respuesta.motivoRechazo(), is(equalTo("El código no existe")));
    }

    @Test
    void testRechazarSinMotivoLoDejaNulo() {
        loguearComo("admin@test.com");
        PerfilCuidadorEntity pepe = crearPerfil(1L, "pepe@test.com");
        VerificacionEntity pendiente = crearVerificacion(1L, pepe, TipoVerificacion.ANTECEDENTES,
                EstadoVerificacion.PENDIENTE);
        when(repository.findById(1L)).thenReturn(Optional.of(pendiente));
        when(repository.save(any(VerificacionEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminVerificacionDTOResponse respuesta = service.resolver(1L, false, "   ");

        assertThat(respuesta.estado(), is(equalTo(EstadoVerificacion.RECHAZADO)));
        assertThat(respuesta.motivoRechazo(), is(nullValue()));
    }
}
