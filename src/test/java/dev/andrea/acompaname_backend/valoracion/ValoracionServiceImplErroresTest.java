package dev.andrea.acompaname_backend.valoracion;

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

import dev.andrea.acompaname_backend.perfilcuidador.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.solicitud.EstadoSolicitud;
import dev.andrea.acompaname_backend.solicitud.SolicitudEntity;
import dev.andrea.acompaname_backend.solicitud.SolicitudRepository;
import dev.andrea.acompaname_backend.solicitud.exceptions.SolicitudExceptionNotFound;
import dev.andrea.acompaname_backend.usuario.UsuarioEntity;
import dev.andrea.acompaname_backend.valoracion.dtos.ValoracionDTORequest;
import dev.andrea.acompaname_backend.valoracion.exceptions.ValoracionExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.valoracion.exceptions.ValoracionExceptionConflicto;
import dev.andrea.acompaname_backend.valoracion.exceptions.ValoracionExceptionNotFound;

// Tests de los casos de error y de seguridad de ValoracionServiceImpl:
// valoración que no existe (404) y valoraciones que no son mías (403).
@ExtendWith(MockitoExtension.class)
public class ValoracionServiceImplErroresTest {
    @Mock
    private ValoracionRepository repository;
    @Mock
    private SolicitudRepository solicitudRepository;

    private ValoracionServiceImpl service;
    private SecurityContext contextoOriginal;

    @BeforeEach
    void setup() {
        // Uso un contexto de seguridad vacío para no afectar a los otros tests
        contextoOriginal = SecurityContextHolder.getContext();
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        service = new ValoracionServiceImpl(repository, solicitudRepository);
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

    // Solicitud completada de Ana (familia) con Pepe (cuidador)
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

    private ValoracionEntity valoracionDeAna() {
        return new ValoracionEntity(1L, "Muy buena atención", 5, LocalDate.of(2026, 9, 11), solicitudAnaPepe());
    }

    private ValoracionDTORequest dtoCualquiera() {
        return new ValoracionDTORequest("Comentario nuevo", 4, LocalDate.of(2026, 9, 13), 1L);
    }

    @Test
    void testGetByIdNoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ValoracionExceptionNotFound.class, () -> service.getById(99L));
    }

    @Test
    void testStoreConSolicitudQueNoExiste() {
        when(solicitudRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(SolicitudExceptionNotFound.class,
                () -> service.storeEntity(new ValoracionDTORequest("Bien", 5, LocalDate.of(2026, 9, 12), 99L)));
        verify(repository, never()).save(Mockito.any(ValoracionEntity.class));
    }

    @Test
    void testNoSePuedeValorarLaSolicitudDeOtraFamilia() {
        loguearComo("intruso@test.com");
        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitudAnaPepe()));

        assertThrows(ValoracionExceptionAccesoDenegado.class,
                () -> service.storeEntity(new ValoracionDTORequest("Bien", 5, LocalDate.of(2026, 9, 12), 1L)));
        verify(repository, never()).save(Mockito.any(ValoracionEntity.class));
    }

    @Test
    void testNoSePuedeValorarUnServicioQueNoEstaCompletado() {
        loguearComo("ana@test.com");
        SolicitudEntity solicitud = solicitudAnaPepe();
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitud));

        assertThrows(ValoracionExceptionConflicto.class, () -> service.storeEntity(dtoCualquiera()));
        verify(repository, never()).save(Mockito.any(ValoracionEntity.class));
    }

    @Test
    void testNoSePuedeValorarDosVecesLaMismaSolicitud() {
        loguearComo("ana@test.com");
        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitudAnaPepe()));
        when(repository.existsBySolicitudId(1L)).thenReturn(true);

        assertThrows(ValoracionExceptionConflicto.class, () -> service.storeEntity(dtoCualquiera()));
        verify(repository, never()).save(Mockito.any(ValoracionEntity.class));
    }

    @Test
    void testUpdateDeLaValoracionDeOtraPersonaDaAccesoDenegadoYNoGuarda() {
        loguearComo("intruso@test.com");
        when(repository.findById(1L)).thenReturn(Optional.of(valoracionDeAna()));

        assertThrows(ValoracionExceptionAccesoDenegado.class, () -> service.update(1L, dtoCualquiera()));
        verify(repository, never()).save(Mockito.any(ValoracionEntity.class));
    }

    @Test
    void testDeleteDeLaValoracionDeOtraPersonaDaAccesoDenegadoYNoBorra() {
        loguearComo("intruso@test.com");
        when(repository.findById(1L)).thenReturn(Optional.of(valoracionDeAna()));

        assertThrows(ValoracionExceptionAccesoDenegado.class, () -> service.deleteById(1L));
        verify(repository, never()).deleteById(Mockito.anyLong());
    }

    @Test
    void testElCuidadorNoPuedeBorrarLaValoracionQueLeHanHecho() {
        loguearComo("pepe@test.com");
        when(repository.findById(1L)).thenReturn(Optional.of(valoracionDeAna()));

        assertThrows(ValoracionExceptionAccesoDenegado.class, () -> service.deleteById(1L));
        verify(repository, never()).deleteById(Mockito.anyLong());
    }

    @Test
    void testDeleteNoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ValoracionExceptionNotFound.class, () -> service.deleteById(99L));
        verify(repository, never()).deleteById(Mockito.anyLong());
    }
}