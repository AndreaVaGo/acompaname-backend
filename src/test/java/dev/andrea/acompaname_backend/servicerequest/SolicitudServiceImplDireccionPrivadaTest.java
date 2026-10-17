package dev.andrea.acompaname_backend.servicerequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.payment.PagoRepository;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTOResponse;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// Tests de la privacidad de la dirección: el cuidador solo la ve cuando ha aceptado la solicitud
@ExtendWith(MockitoExtension.class)
public class SolicitudServiceImplDireccionPrivadaTest {
        private SolicitudServiceImpl service;
        @Mock
        private SolicitudRepository repository;
        @Mock
        private UsuarioRepository usuarioRepository;
        @Mock
        private PerfilCuidadorRepository perfilCuidadorRepository;
        @Mock
        private PagoRepository pagoRepository;

        private static final String DIRECCION = "Calle Uría 10, 3º B, Oviedo";

        @BeforeEach
        void setup() {
                service = new SolicitudServiceImpl(repository, usuarioRepository, perfilCuidadorRepository,
                                pagoRepository);
        }

        private void iniciarSesionComo(String email) {
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

        private UsuarioEntity crearFamilia() {
                return new UsuarioEntity(1L, "Ana", "ana@test.com", "600111222", "1234", Set.of(rolFamilia()));
        }

        private PerfilCuidadorEntity crearCuidador() {
                return new PerfilCuidadorEntity(1L, "Geriatría", 4, new java.math.BigDecimal("18.00"), "Bio", true,
                                true, new UsuarioEntity(2L, "Pepe", "pepe@test.com", "600333444", "5678",
                                                Set.of(rolCuidador())));
        }

        private SolicitudEntity crearSolicitud(EstadoSolicitud estado) {
                SolicitudEntity solicitud = new SolicitudEntity(1L, "Acompañamiento", "Manuel", "Sin notas", 80,
                                LocalDate.of(2026, 9, 10), estado, crearFamilia(), crearCuidador());
                solicitud.setDireccion(DIRECCION);
                return solicitud;
        }

        @Test
        void testCuidadorNoVeLaDireccionSiLaSolicitudEstaPendiente() {
                iniciarSesionComo("pepe@test.com");
                when(repository.findById(1L)).thenReturn(Optional.of(crearSolicitud(EstadoSolicitud.PENDIENTE)));

                SolicitudDTOResponse solicitud = service.getById(1L);

                assertThat(solicitud.direccion(), is(nullValue()));
        }

        @Test
        void testCuidadorNoVeLaDireccionSiLaSolicitudEstaRechazada() {
                iniciarSesionComo("pepe@test.com");
                when(repository.findById(1L)).thenReturn(Optional.of(crearSolicitud(EstadoSolicitud.RECHAZADA)));

                SolicitudDTOResponse solicitud = service.getById(1L);

                assertThat(solicitud.direccion(), is(nullValue()));
        }

        @Test
        void testCuidadorVeLaDireccionSiLaSolicitudEstaAceptada() {
                iniciarSesionComo("pepe@test.com");
                when(repository.findById(1L)).thenReturn(Optional.of(crearSolicitud(EstadoSolicitud.ACEPTADA)));

                SolicitudDTOResponse solicitud = service.getById(1L);

                assertThat(solicitud.direccion(), is(equalTo(DIRECCION)));
        }

        @Test
        void testCuidadorVeLaDireccionSiLaSolicitudEstaCompletada() {
                iniciarSesionComo("pepe@test.com");
                when(repository.findById(1L)).thenReturn(Optional.of(crearSolicitud(EstadoSolicitud.COMPLETADA)));

                SolicitudDTOResponse solicitud = service.getById(1L);

                assertThat(solicitud.direccion(), is(equalTo(DIRECCION)));
        }

        @Test
        void testFamiliaSiempreVeLaDireccion() {
                iniciarSesionComo("ana@test.com");
                when(repository.findById(1L)).thenReturn(Optional.of(crearSolicitud(EstadoSolicitud.PENDIENTE)));

                SolicitudDTOResponse solicitud = service.getById(1L);

                assertThat(solicitud.direccion(), is(equalTo(DIRECCION)));
        }

        @Test
        void testListaDelCuidadorNoIncluyeLaDireccionDeLasPendientes() {
                iniciarSesionComo("pepe@test.com");
                when(repository.findAll()).thenReturn(List.of(crearSolicitud(EstadoSolicitud.PENDIENTE)));

                List<SolicitudDTOResponse> solicitudes = service.getMisSolicitudes();

                assertThat(solicitudes.get(0).direccion(), is(nullValue()));
        }
}