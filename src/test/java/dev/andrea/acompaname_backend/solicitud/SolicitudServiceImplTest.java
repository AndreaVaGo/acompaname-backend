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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.andrea.acompaname_backend.perfilcuidador.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.perfilcuidador.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.solicitud.dtos.SolicitudDTORequest;
import dev.andrea.acompaname_backend.solicitud.dtos.SolicitudDTOResponse;
import dev.andrea.acompaname_backend.usuario.UsuarioEntity;
import dev.andrea.acompaname_backend.usuario.UsuarioRepository;

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

    @BeforeEach
    void setup() {
        service = new SolicitudServiceImpl(repository, usuarioRepository, perfilCuidadorRepository);
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
        UsuarioEntity familia = new UsuarioEntity(1L, "Ana", "ana@test.com", "600111222", "1234",
                Set.of(rolFamilia()));
        PerfilCuidadorEntity cuidador = new PerfilCuidadorEntity(1L, "Geriatría", 4, new java.math.BigDecimal("18.00"),
                "Bio", true, true, new UsuarioEntity(2L, "Pepe", "pepe@test.com", "600333444", "5678",
                        Set.of(rolCuidador())));
        List<SolicitudEntity> solicitudesMock = List.of(
                new SolicitudEntity(1L, "Acompañamiento", "Manuel", "Sin notas", 80, LocalDate.of(2026, 9, 10),
                        EstadoSolicitud.PENDIENTE, familia, cuidador));
        when(repository.findAll()).thenReturn(solicitudesMock);

        List<SolicitudDTOResponse> solicitudes = service.getEntities();

        assertThat(solicitudes.size(), is(equalTo(1)));
        assertThat(solicitudes.get(0).tipoCuidado(), is(equalTo("Acompañamiento")));
    }

    @Test
    void testGetById() {
        UsuarioEntity familia = new UsuarioEntity(1L, "Ana", "ana@test.com", "600111222", "1234",
                Set.of(rolFamilia()));
        PerfilCuidadorEntity cuidador = new PerfilCuidadorEntity(1L, "Geriatría", 4, new java.math.BigDecimal("18.00"),
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
        UsuarioEntity familia = new UsuarioEntity(1L, "Ana", "ana@test.com", "600111222", "1234",
                Set.of(rolFamilia()));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(familia));

        PerfilCuidadorEntity cuidador = new PerfilCuidadorEntity(1L, "Geriatría", 4, new java.math.BigDecimal("18.00"),
                "Bio", true, true, new UsuarioEntity(2L, "Pepe", "pepe@test.com", "600333444", "5678",
                        Set.of(rolCuidador())));
        when(perfilCuidadorRepository.findById(1L)).thenReturn(Optional.of(cuidador));

        SolicitudDTORequest dto = new SolicitudDTORequest("Acompañamiento", "Manuel", "Sin notas", 80,
                LocalDate.of(2026, 9, 10), 1L, 1L);
        when(repository.save(Mockito.any(SolicitudEntity.class))).thenReturn(
                new SolicitudEntity(1L, dto.tipoCuidado(), dto.nombrePaciente(), dto.notas(), dto.edadPaciente(),
                        dto.fechaCuidado(), EstadoSolicitud.PENDIENTE, familia, cuidador));

        SolicitudDTOResponse entity = service.storeEntity(dto);

        assertThat(entity.tipoCuidado(), is(equalTo("Acompañamiento")));
        assertThat(entity.estado(), is(equalTo(EstadoSolicitud.PENDIENTE)));
    }

    @Test
    void testDeleteById() {
        UsuarioEntity familia = new UsuarioEntity(1L, "Ana", "ana@test.com", "600111222", "1234",
                Set.of(rolFamilia()));
        PerfilCuidadorEntity cuidador = new PerfilCuidadorEntity(1L, "Geriatría", 4, new java.math.BigDecimal("18.00"),
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
        PerfilCuidadorEntity cuidador = new PerfilCuidadorEntity(1L, "Geriatría", 4, new java.math.BigDecimal("18.00"),
                "Bio", true, true, new UsuarioEntity(2L, "Pepe", "pepe@test.com", "600333444", "5678",
                        Set.of(rolCuidador())));
        SolicitudEntity solicitudExistente = new SolicitudEntity(1L, "Acompañamiento", "Manuel", "Sin notas", 80,
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
        UsuarioEntity familia = new UsuarioEntity(1L, "Ana", "ana@test.com", "600111222", "1234",
                Set.of(rolFamilia()));
        PerfilCuidadorEntity cuidador = new PerfilCuidadorEntity(1L, "Geriatría", 4, new java.math.BigDecimal("18.00"),
                "Bio", true, true, new UsuarioEntity(2L, "Pepe", "pepe@test.com", "600333444", "5678",
                        Set.of(rolCuidador())));
        SolicitudEntity solicitudExistente = new SolicitudEntity(1L, "Acompañamiento", "Manuel", "Sin notas", 80,
                LocalDate.of(2026, 9, 10), EstadoSolicitud.PENDIENTE, familia, cuidador);
        when(repository.findById(1L)).thenReturn(Optional.of(solicitudExistente));
        when(repository.save(Mockito.any(SolicitudEntity.class))).thenReturn(solicitudExistente);

        SolicitudDTOResponse resultado = service.cambiarEstado(1L, EstadoSolicitud.ACEPTADA);

        assertThat(resultado.estado(), is(equalTo(EstadoSolicitud.ACEPTADA)));
    }
}