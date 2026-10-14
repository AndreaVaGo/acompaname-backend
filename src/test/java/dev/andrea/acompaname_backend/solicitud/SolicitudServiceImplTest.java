package dev.andrea.acompaname_backend.solicitud;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import dev.andrea.acompaname_backend.payment.PagoRepository;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.solicitud.dtos.SolicitudDTORequest;
import dev.andrea.acompaname_backend.solicitud.dtos.SolicitudDTOResponse;
import dev.andrea.acompaname_backend.usuario.UsuarioEntity;
import dev.andrea.acompaname_backend.usuario.UsuarioRepository;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
public class SolicitudServiceImplTest {
        @InjectMocks
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
                service = new SolicitudServiceImpl(repository, usuarioRepository, perfilCuidadorRepository,
                                pagoRepository);
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

        @Test
        void testGetEntities() {
                Authentication auth = new UsernamePasswordAuthenticationToken("ana@test.com", null);
                SecurityContextHolder.getContext().setAuthentication(auth);
                UsuarioEntity familia = new UsuarioEntity(1L, "Ana", "ana@test.com", "600111222", "1234",
                                Set.of(rolFamilia()));
                PerfilCuidadorEntity cuidador = new PerfilCuidadorEntity(1L, "Geriatría", 4,
                                new java.math.BigDecimal("18.00"),
                                "Bio", true, true, new UsuarioEntity(2L, "Pepe", "pepe@test.com", "600333444", "5678",
                                                Set.of(rolCuidador())));
                List<SolicitudEntity> solicitudesMock = List.of(
                                new SolicitudEntity(1L, "Acompañamiento", "Manuel", "Sin notas", 80,
                                                LocalDate.of(2026, 9, 10),
                                                EstadoSolicitud.PENDIENTE, familia, cuidador));
                when(repository.findAll()).thenReturn(solicitudesMock);

                List<SolicitudDTOResponse> solicitudes = service.getEntities();

                assertThat(solicitudes.size(), is(equalTo(1)));
                assertThat(solicitudes.get(0).tipoCuidado(), is(equalTo("Acompañamiento")));
        }

        @Test
        void testGetById() {
                Authentication auth = new UsernamePasswordAuthenticationToken("ana@test.com", null);
                SecurityContextHolder.getContext().setAuthentication(auth);
                UsuarioEntity familia = new UsuarioEntity(1L, "Ana", "ana@test.com", "600111222", "1234",
                                Set.of(rolFamilia()));
                PerfilCuidadorEntity cuidador = new PerfilCuidadorEntity(1L, "Geriatría", 4,
                                new java.math.BigDecimal("18.00"),
                                "Bio", true, true, new UsuarioEntity(2L, "Pepe", "pepe@test.com", "600333444", "5678",
                                                Set.of(rolCuidador())));
                SolicitudEntity solicitudMock = new SolicitudEntity(1L, "Acompañamiento", "Manuel", "Sin notas", 80,
                                LocalDate.of(2026, 9, 10), EstadoSolicitud.PENDIENTE, familia, cuidador);
                when(repository.findById(1L)).thenReturn(Optional.of(solicitudMock));

                SolicitudDTOResponse solicitud = service.getById(1L);

                assertThat(solicitud.tipoCuidado(), is(equalTo("Acompañamiento")));
                assertThat(solicitud.estado(), is(equalTo(EstadoSolicitud.PENDIENTE)));
        }

        @Test
        void testStoreEntity() {
                Authentication auth = new UsernamePasswordAuthenticationToken("ana@test.com", null);
                SecurityContextHolder.getContext().setAuthentication(auth);
                UsuarioEntity familia = new UsuarioEntity(1L, "Ana", "ana@test.com", "600111222", "1234",
                                Set.of(rolFamilia()));
                when(usuarioRepository.findById(1L)).thenReturn(Optional.of(familia));

                PerfilCuidadorEntity cuidador = new PerfilCuidadorEntity(1L, "Geriatría", 4,
                                new java.math.BigDecimal("18.00"),
                                "Bio", true, true, new UsuarioEntity(2L, "Pepe", "pepe@test.com", "600333444", "5678",
                                                Set.of(rolCuidador())));
                when(perfilCuidadorRepository.findById(1L)).thenReturn(Optional.of(cuidador));

                SolicitudDTORequest dto = new SolicitudDTORequest("Acompañamiento", "Manuel", "Sin notas", 80,
                                LocalDate.of(2026, 9, 10), 1L, 1L);
                when(repository.save(Mockito.any(SolicitudEntity.class))).thenReturn(
                                new SolicitudEntity(1L, dto.tipoCuidado(), dto.nombrePaciente(), dto.notas(),
                                                dto.edadPaciente(),
                                                dto.fechaCuidado(), EstadoSolicitud.PENDIENTE, familia, cuidador));

                SolicitudDTOResponse entity = service.storeEntity(dto);

                assertThat(entity.tipoCuidado(), is(equalTo("Acompañamiento")));
                assertThat(entity.estado(), is(equalTo(EstadoSolicitud.PENDIENTE)));
        }

        @Test
        void testDeleteById() {
                Authentication auth = new UsernamePasswordAuthenticationToken("ana@test.com", null);
                SecurityContextHolder.getContext().setAuthentication(auth);
                UsuarioEntity familia = new UsuarioEntity(1L, "Ana", "ana@test.com", "600111222", "1234",
                                Set.of(rolFamilia()));
                PerfilCuidadorEntity cuidador = new PerfilCuidadorEntity(1L, "Geriatría", 4,
                                new java.math.BigDecimal("18.00"),
                                "Bio", true, true, new UsuarioEntity(2L, "Pepe", "pepe@test.com", "600333444", "5678",
                                                Set.of(rolCuidador())));
                SolicitudEntity solicitudMock = new SolicitudEntity(1L, "Acompañamiento", "Manuel", "Sin notas", 80,
                                LocalDate.of(2026, 9, 10), EstadoSolicitud.PENDIENTE, familia, cuidador);
                when(repository.findById(1L)).thenReturn(Optional.of(solicitudMock));

                service.deleteById(1L);

                Mockito.verify(repository).deleteById(1L);
        }

        @Test
        void testUpdate() {
                UsuarioEntity familia = new UsuarioEntity(1L, "Ana", "ana@test.com", "600111222", "1234",
                                Set.of(rolFamilia()));
                PerfilCuidadorEntity cuidador = new PerfilCuidadorEntity(1L, "Geriatría", 4,
                                new java.math.BigDecimal("18.00"),
                                "Bio", true, true, new UsuarioEntity(2L, "Pepe", "pepe@test.com", "600333444", "5678",
                                                Set.of(rolCuidador())));
                SolicitudEntity solicitudExistente = new SolicitudEntity(1L, "Acompañamiento", "Manuel", "Sin notas",
                                80,
                                LocalDate.of(2026, 9, 10), EstadoSolicitud.PENDIENTE, familia, cuidador);
                when(repository.findById(1L)).thenReturn(Optional.of(solicitudExistente));
                when(repository.save(Mockito.any(SolicitudEntity.class))).thenReturn(solicitudExistente);

                SolicitudDTORequest dto = new SolicitudDTORequest("Enfermería", "Manuel", "Notas nuevas", 82,
                                LocalDate.of(2026, 9, 15), 1L, 1L);
                SolicitudDTOResponse resultado = service.update(1L, dto);

                assertThat(resultado.tipoCuidado(), is(equalTo("Enfermería")));
        }

        @Test
        void testCambiarEstado() {
                Authentication auth = new UsernamePasswordAuthenticationToken("pepe@test.com", null);
                SecurityContextHolder.getContext().setAuthentication(auth);
                UsuarioEntity familia = new UsuarioEntity(1L, "Ana", "ana@test.com", "600111222", "1234",
                                Set.of(rolFamilia()));
                PerfilCuidadorEntity cuidador = new PerfilCuidadorEntity(1L, "Geriatría", 4,
                                new java.math.BigDecimal("18.00"),
                                "Bio", true, true, new UsuarioEntity(2L, "Pepe", "pepe@test.com", "600333444", "5678",
                                                Set.of(rolCuidador())));
                SolicitudEntity solicitudExistente = new SolicitudEntity(1L, "Acompañamiento", "Manuel", "Sin notas",
                                80,
                                LocalDate.of(2026, 9, 10), EstadoSolicitud.PENDIENTE, familia, cuidador);
                when(repository.findById(1L)).thenReturn(Optional.of(solicitudExistente));
                when(repository.save(Mockito.any(SolicitudEntity.class))).thenReturn(solicitudExistente);

                SolicitudDTOResponse resultado = service.cambiarEstado(1L, EstadoSolicitud.ACEPTADA);

                assertThat(resultado.estado(), is(equalTo(EstadoSolicitud.ACEPTADA)));
        }

        @Test
        void testStoreEntityGuardaLasNecesidadesComoTexto() {
                Authentication auth = new UsernamePasswordAuthenticationToken("ana@test.com", null);
                SecurityContextHolder.getContext().setAuthentication(auth);
                UsuarioEntity familia = new UsuarioEntity(1L, "Ana", "ana@test.com", "600111222", "1234",
                                Set.of(rolFamilia()));
                when(usuarioRepository.findById(1L)).thenReturn(Optional.of(familia));
                PerfilCuidadorEntity cuidador = new PerfilCuidadorEntity(1L, "Geriatría", 4,
                                new java.math.BigDecimal("18.00"),
                                "Bio", true, true, new UsuarioEntity(2L, "Pepe", "pepe@test.com", "600333444", "5678",
                                                Set.of(rolCuidador())));
                when(perfilCuidadorRepository.findById(1L)).thenReturn(Optional.of(cuidador));
                when(repository.save(Mockito.any(SolicitudEntity.class)))
                                .thenAnswer(invocacion -> invocacion.getArgument(0));

                SolicitudDTORequest dto = new SolicitudDTORequest("Acompañamiento", "Manuel", "Sin notas", 80,
                                LocalDate.of(2026, 9, 10), null, 1L, 1L, List.of("movilidad", "higiene"));
                SolicitudDTOResponse resultado = service.storeEntity(dto);

                ArgumentCaptor<SolicitudEntity> captor = ArgumentCaptor.forClass(SolicitudEntity.class);
                Mockito.verify(repository).save(captor.capture());
                assertThat(captor.getValue().getNecesidades(), is(equalTo("movilidad,higiene")));
                assertThat(resultado.necesidades().size(), is(equalTo(2)));
                assertThat(resultado.necesidades().get(0), is(equalTo("movilidad")));
                assertThat(resultado.necesidades().get(1), is(equalTo("higiene")));
        }

        @Test
        void testGetByIdDevuelveLasNecesidadesComoLista() {
                Authentication auth = new UsernamePasswordAuthenticationToken("ana@test.com", null);
                SecurityContextHolder.getContext().setAuthentication(auth);
                UsuarioEntity familia = new UsuarioEntity(1L, "Ana", "ana@test.com", "600111222", "1234",
                                Set.of(rolFamilia()));
                PerfilCuidadorEntity cuidador = new PerfilCuidadorEntity(1L, "Geriatría", 4,
                                new java.math.BigDecimal("18.00"),
                                "Bio", true, true, new UsuarioEntity(2L, "Pepe", "pepe@test.com", "600333444", "5678",
                                                Set.of(rolCuidador())));
                SolicitudEntity solicitudMock = new SolicitudEntity(1L, "Acompañamiento", "Manuel", "Sin notas", 80,
                                LocalDate.of(2026, 9, 10), EstadoSolicitud.PENDIENTE, familia, cuidador);
                solicitudMock.setNecesidades("medicacion,nocturno");
                when(repository.findById(1L)).thenReturn(Optional.of(solicitudMock));

                SolicitudDTOResponse solicitud = service.getById(1L);

                assertThat(solicitud.necesidades().size(), is(equalTo(2)));
                assertThat(solicitud.necesidades().get(0), is(equalTo("medicacion")));
                assertThat(solicitud.necesidades().get(1), is(equalTo("nocturno")));
        }

        @Test
        void testGetByIdSinNecesidadesDevuelveListaVacia() {
                Authentication auth = new UsernamePasswordAuthenticationToken("ana@test.com", null);
                SecurityContextHolder.getContext().setAuthentication(auth);
                UsuarioEntity familia = new UsuarioEntity(1L, "Ana", "ana@test.com", "600111222", "1234",
                                Set.of(rolFamilia()));
                PerfilCuidadorEntity cuidador = new PerfilCuidadorEntity(1L, "Geriatría", 4,
                                new java.math.BigDecimal("18.00"),
                                "Bio", true, true, new UsuarioEntity(2L, "Pepe", "pepe@test.com", "600333444", "5678",
                                                Set.of(rolCuidador())));
                SolicitudEntity solicitudMock = new SolicitudEntity(1L, "Acompañamiento", "Manuel", "Sin notas", 80,
                                LocalDate.of(2026, 9, 10), EstadoSolicitud.PENDIENTE, familia, cuidador);
                when(repository.findById(1L)).thenReturn(Optional.of(solicitudMock));

                SolicitudDTOResponse solicitud = service.getById(1L);

                assertThat(solicitud.necesidades().size(), is(equalTo(0)));
        }

        @Test
        void testUpdateCambiaLasNecesidades() {
                Authentication auth = new UsernamePasswordAuthenticationToken("ana@test.com", null);
                SecurityContextHolder.getContext().setAuthentication(auth);
                UsuarioEntity familia = new UsuarioEntity(1L, "Ana", "ana@test.com", "600111222", "1234",
                                Set.of(rolFamilia()));
                PerfilCuidadorEntity cuidador = new PerfilCuidadorEntity(1L, "Geriatría", 4,
                                new java.math.BigDecimal("18.00"),
                                "Bio", true, true, new UsuarioEntity(2L, "Pepe", "pepe@test.com", "600333444", "5678",
                                                Set.of(rolCuidador())));
                SolicitudEntity solicitudExistente = new SolicitudEntity(1L, "Acompañamiento", "Manuel", "Sin notas",
                                80, LocalDate.of(2026, 9, 10), EstadoSolicitud.PENDIENTE, familia, cuidador);
                solicitudExistente.setNecesidades("movilidad");
                when(repository.findById(1L)).thenReturn(Optional.of(solicitudExistente));
                when(repository.save(Mockito.any(SolicitudEntity.class))).thenReturn(solicitudExistente);

                SolicitudDTORequest dto = new SolicitudDTORequest("Acompañamiento", "Manuel", "Sin notas", 80,
                                LocalDate.of(2026, 9, 15), null, 1L, 1L, List.of("higiene", "alimentacion"));
                SolicitudDTOResponse resultado = service.update(1L, dto);

                assertThat(solicitudExistente.getNecesidades(), is(equalTo("higiene,alimentacion")));
                assertThat(resultado.necesidades().size(), is(equalTo(2)));
        }

        @Test
        void testUpdateSinNecesidadesMantieneLasQueHabia() {
                Authentication auth = new UsernamePasswordAuthenticationToken("ana@test.com", null);
                SecurityContextHolder.getContext().setAuthentication(auth);
                UsuarioEntity familia = new UsuarioEntity(1L, "Ana", "ana@test.com", "600111222", "1234",
                                Set.of(rolFamilia()));
                PerfilCuidadorEntity cuidador = new PerfilCuidadorEntity(1L, "Geriatría", 4,
                                new java.math.BigDecimal("18.00"),
                                "Bio", true, true, new UsuarioEntity(2L, "Pepe", "pepe@test.com", "600333444", "5678",
                                                Set.of(rolCuidador())));
                SolicitudEntity solicitudExistente = new SolicitudEntity(1L, "Acompañamiento", "Manuel", "Sin notas",
                                80, LocalDate.of(2026, 9, 10), EstadoSolicitud.PENDIENTE, familia, cuidador);
                solicitudExistente.setNecesidades("movilidad");
                when(repository.findById(1L)).thenReturn(Optional.of(solicitudExistente));
                when(repository.save(Mockito.any(SolicitudEntity.class))).thenReturn(solicitudExistente);

                SolicitudDTORequest dto = new SolicitudDTORequest("Acompañamiento", "Manuel", "Notas nuevas", 80,
                                LocalDate.of(2026, 9, 15), 1L, 1L);
                service.update(1L, dto);

                assertThat(solicitudExistente.getNecesidades(), is(equalTo("movilidad")));
        }

        @Test
        void testUpdateConListaVaciaBorraLasNecesidades() {
                Authentication auth = new UsernamePasswordAuthenticationToken("ana@test.com", null);
                SecurityContextHolder.getContext().setAuthentication(auth);
                UsuarioEntity familia = new UsuarioEntity(1L, "Ana", "ana@test.com", "600111222", "1234",
                                Set.of(rolFamilia()));
                PerfilCuidadorEntity cuidador = new PerfilCuidadorEntity(1L, "Geriatría", 4,
                                new java.math.BigDecimal("18.00"),
                                "Bio", true, true, new UsuarioEntity(2L, "Pepe", "pepe@test.com", "600333444", "5678",
                                                Set.of(rolCuidador())));
                SolicitudEntity solicitudExistente = new SolicitudEntity(1L, "Acompañamiento", "Manuel", "Sin notas",
                                80, LocalDate.of(2026, 9, 10), EstadoSolicitud.PENDIENTE, familia, cuidador);
                solicitudExistente.setNecesidades("movilidad");
                when(repository.findById(1L)).thenReturn(Optional.of(solicitudExistente));
                when(repository.save(Mockito.any(SolicitudEntity.class))).thenReturn(solicitudExistente);

                SolicitudDTORequest dto = new SolicitudDTORequest("Acompañamiento", "Manuel", "Sin notas", 80,
                                LocalDate.of(2026, 9, 15), null, 1L, 1L, List.of());
                SolicitudDTOResponse resultado = service.update(1L, dto);

                assertThat(resultado.necesidades().size(), is(equalTo(0)));
        }
}