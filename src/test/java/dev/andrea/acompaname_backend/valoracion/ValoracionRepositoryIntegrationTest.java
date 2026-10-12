package dev.andrea.acompaname_backend.valoracion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import dev.andrea.acompaname_backend.perfilcuidador.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.perfilcuidador.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.solicitud.EstadoSolicitud;
import dev.andrea.acompaname_backend.solicitud.SolicitudEntity;
import dev.andrea.acompaname_backend.solicitud.SolicitudRepository;

@DataJpaTest
@Testcontainers
public class ValoracionRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0");

    @Autowired
    private ValoracionRepository repository;
    @Autowired
    private SolicitudRepository solicitudRepository;
    @Autowired
    private PerfilCuidadorRepository perfilCuidadorRepository;

    private PerfilCuidadorEntity crearCuidador() {
        PerfilCuidadorEntity perfil = PerfilCuidadorEntity.builder()
                .especialidad("Geriatría")
                .anosExperiencia(4)
                .tarifaHora(new BigDecimal("18.00"))
                .bio("Bio")
                .build();
        return perfilCuidadorRepository.saveAndFlush(perfil);
    }

    // Crea una solicitud completada del cuidador y le añade una valoración
    private void valorar(PerfilCuidadorEntity cuidador, int puntuacion) {
        SolicitudEntity solicitud = SolicitudEntity.builder()
                .tipoCuidado("DOMICILIO")
                .estado(EstadoSolicitud.COMPLETADA)
                .cuidador(cuidador)
                .build();
        solicitudRepository.saveAndFlush(solicitud);

        ValoracionEntity valoracion = ValoracionEntity.builder()
                .comentario("Comentario")
                .puntuacion(puntuacion)
                .fecha(LocalDate.now())
                .solicitud(solicitud)
                .build();
        repository.saveAndFlush(valoracion);
    }

    @Test
    void testMediaYNumeroDeValoracionesDeUnCuidador() {
        PerfilCuidadorEntity cuidador = crearCuidador();
        valorar(cuidador, 5);
        valorar(cuidador, 4);

        Double media = repository.mediaPorCuidador(cuidador.getId());
        long total = repository.countBySolicitudCuidadorId(cuidador.getId());

        assertThat(media, is(equalTo(4.5)));
        assertThat(total, is(equalTo(2L)));
    }

    @Test
    void testCuidadorSinValoracionesNoTieneMedia() {
        PerfilCuidadorEntity cuidador = crearCuidador();

        Double media = repository.mediaPorCuidador(cuidador.getId());
        long total = repository.countBySolicitudCuidadorId(cuidador.getId());

        assertThat(media, is(nullValue()));
        assertThat(total, is(equalTo(0L)));
    }

    @Test
    void testLaMediaNoMezclaLasValoracionesDeOtrosCuidadores() {
        PerfilCuidadorEntity cuidador = crearCuidador();
        PerfilCuidadorEntity otroCuidador = crearCuidador();
        valorar(cuidador, 5);
        valorar(otroCuidador, 1);

        Double media = repository.mediaPorCuidador(cuidador.getId());
        long total = repository.countBySolicitudCuidadorId(cuidador.getId());

        assertThat(media, is(equalTo(5.0)));
        assertThat(total, is(equalTo(1L)));
    }
}