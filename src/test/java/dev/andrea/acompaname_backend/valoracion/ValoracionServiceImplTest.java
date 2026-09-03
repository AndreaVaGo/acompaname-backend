package dev.andrea.acompaname_backend.valoracion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.andrea.acompaname_backend.perfilcuidador.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.solicitud.EstadoSolicitud;
import dev.andrea.acompaname_backend.solicitud.SolicitudEntity;
import dev.andrea.acompaname_backend.solicitud.SolicitudRepository;
import dev.andrea.acompaname_backend.usuario.Rol;
import dev.andrea.acompaname_backend.usuario.UsuarioEntity;
import dev.andrea.acompaname_backend.valoracion.dtos.ValoracionDTORequest;
import dev.andrea.acompaname_backend.valoracion.dtos.ValoracionDTOResponse;

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

    private SolicitudEntity crearSolicitudMock() {
        UsuarioEntity familia = new UsuarioEntity(1L, "Ana", "ana@test.com", "600111222", "1234", Rol.FAMILIA);
        PerfilCuidadorEntity cuidador = new PerfilCuidadorEntity(1L, "Geriatría", 4, new java.math.BigDecimal("18.00"),
                "Bio", true, true, new UsuarioEntity(2L, "Pepe", "pepe@test.com", "600333444", "5678", Rol.CUIDADOR));
        return new SolicitudEntity(1L, "Acompañamiento", "Manuel", "Sin notas", 80, LocalDate.of(2026, 9, 10),
                EstadoSolicitud.COMPLETADA, familia, cuidador);
    }

    @Test
    void testGetEntities() {
        SolicitudEntity solicitud = crearSolicitudMock();
        List<ValoracionEntity> valoracionesMock = List.of(
                new ValoracionEntity(1L, "Muy buena atención", 5, LocalDate.of(2026, 9, 11), solicitud));
        when(repository.findAll()).thenReturn(valoracionesMock);

        List<ValoracionEntity> valoraciones = service.getEntities();

        assertThat(valoraciones.size(), is(equalTo(1)));
        assertThat(valoraciones.get(0).getComentario(), is(equalTo("Muy buena atención")));
    }

    @Test
    void testGetById() {
        SolicitudEntity solicitud = crearSolicitudMock();
        ValoracionEntity valoracionMock = new ValoracionEntity(1L, "Muy buena atención", 5,
                LocalDate.of(2026, 9, 11), solicitud);
        when(repository.findById(1L)).thenReturn(Optional.of(valoracionMock));

        ValoracionEntity valoracion = service.getById(1L);

        assertThat(valoracion.getComentario(), is(equalTo("Muy buena atención")));
        assertThat(valoracion.getPuntuacion(), is(equalTo(5)));
    }

    @Test
    void testStoreEntity() {
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
        SolicitudEntity solicitud = crearSolicitudMock();
        ValoracionEntity valoracionMock = new ValoracionEntity(1L, "Muy buena atención", 5,
                LocalDate.of(2026, 9, 11), solicitud);
        when(repository.findById(1L)).thenReturn(Optional.of(valoracionMock));

        service.deleteById(1L);

        Mockito.verify(repository).deleteById(1L);
    }

    @Test
    void testUpdate() {
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