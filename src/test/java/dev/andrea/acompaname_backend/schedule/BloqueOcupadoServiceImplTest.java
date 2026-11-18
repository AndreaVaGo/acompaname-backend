package dev.andrea.acompaname_backend.schedule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.caregiverprofile.exceptions.PerfilCuidadorExceptionNotFound;
import dev.andrea.acompaname_backend.schedule.dtos.BloqueOcupadoDTORequest;
import dev.andrea.acompaname_backend.schedule.dtos.BloqueOcupadoDTOResponse;
import dev.andrea.acompaname_backend.schedule.exceptions.BloqueOcupadoExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.schedule.exceptions.BloqueOcupadoExceptionFechaInvalida;
import dev.andrea.acompaname_backend.schedule.exceptions.BloqueOcupadoExceptionNotFound;
import dev.andrea.acompaname_backend.user.UsuarioEntity;

@ExtendWith(MockitoExtension.class)
public class BloqueOcupadoServiceImplTest {

    @Mock
    private BloqueOcupadoRepository repository;
    @Mock
    private PerfilCuidadorRepository perfilRepository;

    private BloqueOcupadoServiceImpl service;

    @BeforeEach
    void setup() {
        service = new BloqueOcupadoServiceImpl(repository, perfilRepository);
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
        PerfilCuidadorEntity perfil = new PerfilCuidadorEntity();
        perfil.setId(id);
        perfil.setUsuario(usuario);
        return perfil;
    }

    private BloqueOcupadoDTORequest pedir(LocalDate fecha, LocalTime inicio, LocalTime fin) {
        return new BloqueOcupadoDTORequest(fecha, inicio, fin, "Turno");
    }

    @Test
    void testCrearGuardaElBloqueConLaPersonaCuidadoraLogueada() {
        loguearComo("pepe@test.com");
        PerfilCuidadorEntity pepe = crearPerfil(4L, "pepe@test.com");
        when(perfilRepository.findByUsuarioEmail("pepe@test.com")).thenReturn(Optional.of(pepe));
        when(repository.save(ArgumentMatchers.any(BloqueOcupadoEntity.class))).thenAnswer(invocation -> {
            BloqueOcupadoEntity guardado = invocation.getArgument(0);
            guardado.setId(11L);
            return guardado;
        });
        LocalDate fecha = LocalDate.now().plusDays(2);

        BloqueOcupadoDTOResponse respuesta = service.crear(pedir(fecha, LocalTime.of(8, 0), LocalTime.of(15, 0)));

        ArgumentCaptor<BloqueOcupadoEntity> captor = ArgumentCaptor.forClass(BloqueOcupadoEntity.class);
        Mockito.verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCuidador(), is(equalTo(pepe)));
        assertThat(respuesta.id(), is(equalTo(11L)));
        assertThat(respuesta.fecha(), is(equalTo(fecha)));
    }

    @Test
    void testCrearConLaFechaDeHoyEsValido() {
        loguearComo("pepe@test.com");
        when(perfilRepository.findByUsuarioEmail("pepe@test.com"))
                .thenReturn(Optional.of(crearPerfil(4L, "pepe@test.com")));
        when(repository.save(ArgumentMatchers.any(BloqueOcupadoEntity.class))).thenAnswer(i -> i.getArgument(0));

        BloqueOcupadoDTOResponse respuesta = service
                .crear(pedir(LocalDate.now(), LocalTime.of(8, 0), LocalTime.of(9, 0)));

        assertThat(respuesta.fecha(), is(equalTo(LocalDate.now())));
    }

    @Test
    void testCrearSinPerfilDaNotFoundYNoGuarda() {
        loguearComo("nuevo@test.com");
        when(perfilRepository.findByUsuarioEmail("nuevo@test.com")).thenReturn(Optional.empty());

        assertThrows(PerfilCuidadorExceptionNotFound.class, () -> service
                .crear(pedir(LocalDate.now().plusDays(1), LocalTime.of(8, 0), LocalTime.of(9, 0))));

        Mockito.verify(repository, Mockito.never()).save(ArgumentMatchers.any());
    }

    @Test
    void testCrearConFechaPasadaDaFechaInvalida() {
        loguearComo("pepe@test.com");
        when(perfilRepository.findByUsuarioEmail("pepe@test.com"))
                .thenReturn(Optional.of(crearPerfil(4L, "pepe@test.com")));

        assertThrows(BloqueOcupadoExceptionFechaInvalida.class, () -> service
                .crear(pedir(LocalDate.now().minusDays(1), LocalTime.of(8, 0), LocalTime.of(9, 0))));

        Mockito.verify(repository, Mockito.never()).save(ArgumentMatchers.any());
    }

    @Test
    void testCrearConHoraFinAnteriorDaFechaInvalida() {
        loguearComo("pepe@test.com");
        when(perfilRepository.findByUsuarioEmail("pepe@test.com"))
                .thenReturn(Optional.of(crearPerfil(4L, "pepe@test.com")));

        assertThrows(BloqueOcupadoExceptionFechaInvalida.class, () -> service
                .crear(pedir(LocalDate.now().plusDays(1), LocalTime.of(15, 0), LocalTime.of(8, 0))));

        Mockito.verify(repository, Mockito.never()).save(ArgumentMatchers.any());
    }

    @Test
    void testCrearConHoraFinIgualALaDeInicioDaFechaInvalida() {
        loguearComo("pepe@test.com");
        when(perfilRepository.findByUsuarioEmail("pepe@test.com"))
                .thenReturn(Optional.of(crearPerfil(4L, "pepe@test.com")));

        assertThrows(BloqueOcupadoExceptionFechaInvalida.class, () -> service
                .crear(pedir(LocalDate.now().plusDays(1), LocalTime.of(8, 0), LocalTime.of(8, 0))));
    }

    @Test
    void testListarMiosDevuelveLosBloquesDeSuPerfil() {
        loguearComo("pepe@test.com");
        when(perfilRepository.findByUsuarioEmail("pepe@test.com"))
                .thenReturn(Optional.of(crearPerfil(4L, "pepe@test.com")));
        BloqueOcupadoEntity bloque = BloqueOcupadoEntity.builder().id(1L).fecha(LocalDate.of(2026, 12, 1))
                .horaInicio(LocalTime.of(8, 0)).horaFin(LocalTime.of(15, 0)).motivo("Turno").build();
        when(repository.findByCuidadorIdOrderByFechaAscHoraInicioAsc(4L)).thenReturn(List.of(bloque));

        List<BloqueOcupadoDTOResponse> lista = service.listarMios();

        assertThat(lista.size(), is(equalTo(1)));
        assertThat(lista.get(0).motivo(), is(equalTo("Turno")));
    }

    @Test
    void testListarMiosSinPerfilDevuelveListaVacia() {
        loguearComo("nuevo@test.com");
        when(perfilRepository.findByUsuarioEmail("nuevo@test.com")).thenReturn(Optional.empty());

        assertThat(service.listarMios(), is(empty()));
    }

    @Test
    void testBorrarElBloqueDeSuDuenoLoElimina() {
        loguearComo("pepe@test.com");
        BloqueOcupadoEntity bloque = BloqueOcupadoEntity.builder().id(1L)
                .cuidador(crearPerfil(4L, "pepe@test.com")).build();
        when(repository.findById(1L)).thenReturn(Optional.of(bloque));

        service.borrar(1L);

        Mockito.verify(repository).delete(bloque);
    }

    @Test
    void testBorrarElBloqueDeOtraPersonaDaAccesoDenegadoYNoBorra() {
        loguearComo("lucia@test.com");
        BloqueOcupadoEntity bloque = BloqueOcupadoEntity.builder().id(1L)
                .cuidador(crearPerfil(4L, "pepe@test.com")).build();
        when(repository.findById(1L)).thenReturn(Optional.of(bloque));

        assertThrows(BloqueOcupadoExceptionAccesoDenegado.class, () -> service.borrar(1L));

        Mockito.verify(repository, Mockito.never()).delete(ArgumentMatchers.any(BloqueOcupadoEntity.class));
    }

    @Test
    void testBorrarUnBloqueQueNoExisteDaNotFound() {
        loguearComo("pepe@test.com");
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BloqueOcupadoExceptionNotFound.class, () -> service.borrar(99L));
    }
}
