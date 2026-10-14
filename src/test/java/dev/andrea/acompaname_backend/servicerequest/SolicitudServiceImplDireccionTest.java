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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.payment.PagoRepository;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTORequest;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTOResponse;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// Tests de la dirección o centro donde se hará el cuidado
@ExtendWith(MockitoExtension.class)
public class SolicitudServiceImplDireccionTest {
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
                Authentication auth = new UsernamePasswordAuthenticationToken("ana@test.com", null);
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

        private SolicitudEntity crearSolicitudExistente() {
                return new SolicitudEntity(1L, "Acompañamiento", "Manuel", "Sin notas", 80,
                                LocalDate.of(2026, 9, 10), EstadoSolicitud.PENDIENTE, crearFamilia(),
                                crearCuidador());
        }

        @Test
        void testStoreEntityGuardaLaDireccion() {
                when(usuarioRepository.findById(1L)).thenReturn(Optional.of(crearFamilia()));
                when(perfilCuidadorRepository.findById(1L)).thenReturn(Optional.of(crearCuidador()));
                when(repository.save(Mockito.any(SolicitudEntity.class)))
                                .thenAnswer(invocacion -> invocacion.getArgument(0));

                SolicitudDTORequest dto = new SolicitudDTORequest("Acompañamiento", "Manuel", "Sin notas", 80,
                                LocalDate.of(2026, 9, 10), null, 1L, 1L, List.of("movilidad"),
                                "Calle Uría 10, 3º B, Oviedo");
                SolicitudDTOResponse resultado = service.storeEntity(dto);

                ArgumentCaptor<SolicitudEntity> captor = ArgumentCaptor.forClass(SolicitudEntity.class);
                Mockito.verify(repository).save(captor.capture());
                assertThat(captor.getValue().getDireccion(), is(equalTo("Calle Uría 10, 3º B, Oviedo")));
                assertThat(resultado.direccion(), is(equalTo("Calle Uría 10, 3º B, Oviedo")));
        }

        @Test
        void testGetByIdDevuelveLaDireccion() {
                SolicitudEntity solicitudMock = crearSolicitudExistente();
                solicitudMock.setDireccion("Hospital de Cabueñes, planta 2, habitación 214");
                when(repository.findById(1L)).thenReturn(Optional.of(solicitudMock));

                SolicitudDTOResponse solicitud = service.getById(1L);

                assertThat(solicitud.direccion(), is(equalTo("Hospital de Cabueñes, planta 2, habitación 214")));
        }

        @Test
        void testGetByIdSinDireccionDevuelveNull() {
                when(repository.findById(1L)).thenReturn(Optional.of(crearSolicitudExistente()));

                SolicitudDTOResponse solicitud = service.getById(1L);

                assertThat(solicitud.direccion(), is(nullValue()));
        }

        @Test
        void testUpdateCambiaLaDireccion() {
                SolicitudEntity solicitudExistente = crearSolicitudExistente();
                solicitudExistente.setDireccion("Calle Uría 10, Oviedo");
                when(repository.findById(1L)).thenReturn(Optional.of(solicitudExistente));
                when(repository.save(Mockito.any(SolicitudEntity.class))).thenReturn(solicitudExistente);

                SolicitudDTORequest dto = new SolicitudDTORequest("Acompañamiento", "Manuel", "Sin notas", 80,
                                LocalDate.of(2026, 9, 15), null, 1L, 1L, null, "Calle Gascona 5, Oviedo");
                SolicitudDTOResponse resultado = service.update(1L, dto);

                assertThat(solicitudExistente.getDireccion(), is(equalTo("Calle Gascona 5, Oviedo")));
                assertThat(resultado.direccion(), is(equalTo("Calle Gascona 5, Oviedo")));
        }

        @Test
        void testUpdateSinDireccionBorraLaDireccion() {
                SolicitudEntity solicitudExistente = crearSolicitudExistente();
                solicitudExistente.setDireccion("Calle Uría 10, Oviedo");
                when(repository.findById(1L)).thenReturn(Optional.of(solicitudExistente));
                when(repository.save(Mockito.any(SolicitudEntity.class))).thenReturn(solicitudExistente);

                SolicitudDTORequest dto = new SolicitudDTORequest("Acompañamiento", "Manuel", "Sin notas", 80,
                                LocalDate.of(2026, 9, 15), 1L, 1L);
                service.update(1L, dto);

                assertThat(solicitudExistente.getDireccion(), is(nullValue()));
        }
}