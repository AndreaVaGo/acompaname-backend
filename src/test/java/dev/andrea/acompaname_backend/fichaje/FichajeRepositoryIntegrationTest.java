package dev.andrea.acompaname_backend.fichaje;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.servicerequest.EstadoSolicitud;
import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;
import dev.andrea.acompaname_backend.servicerequest.SolicitudRepository;
import dev.andrea.acompaname_backend.support.ContenedorMySqlRepositorios;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// Prueba contra un MySQL real que los fichajes se guardan por solicitud, en orden y sin repetirse
@DataJpaTest
public class FichajeRepositoryIntegrationTest extends ContenedorMySqlRepositorios {

    @Autowired
    private FichajeRepository repository;
    @Autowired
    private SolicitudRepository solicitudRepository;
    @Autowired
    private PerfilCuidadorRepository perfilRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    private UsuarioEntity crearUsuario(String nombre, String email) {
        return usuarioRepository.saveAndFlush(UsuarioEntity.builder()
                .nombre(nombre)
                .email(email)
                .telefono("600111222")
                .password("cifrada")
                .build());
    }

    private SolicitudEntity crearSolicitud(String sufijo) {
        UsuarioEntity familia = crearUsuario("Ana", "ana." + sufijo + "@test.com");
        PerfilCuidadorEntity cuidador = perfilRepository.saveAndFlush(PerfilCuidadorEntity.builder()
                .especialidad("Geriatría")
                .anosExperiencia(4)
                .tarifaHora(new BigDecimal("18.00"))
                .bio("Bio")
                .usuario(crearUsuario("Pepe", "pepe." + sufijo + "@test.com"))
                .build());
        return solicitudRepository.saveAndFlush(SolicitudEntity.builder()
                .tipoCuidado("domicilio")
                .estado(EstadoSolicitud.ACEPTADA)
                .fechaCuidado(LocalDate.of(2026, 11, 24))
                .direccion("Calle Uría 1, Oviedo")
                .familia(familia)
                .cuidador(cuidador)
                .build());
    }

    private FichajeEntity fichar(SolicitudEntity solicitud, TipoFichaje tipo, LocalDateTime momento) {
        return repository.saveAndFlush(FichajeEntity.builder()
                .solicitud(solicitud)
                .tipo(tipo)
                .momento(momento)
                .ubicacion(ResultadoUbicacion.EN_EL_LUGAR)
                .build());
    }

    @Test
    void testDevuelveLosFichajesDeLaSolicitudEnOrdenDeHora() {
        SolicitudEntity solicitud = crearSolicitud("orden");
        // Se guardan al revés para comprobar que la base de datos los ordena por hora
        fichar(solicitud, TipoFichaje.FIN, LocalDateTime.of(2026, 11, 24, 14, 0));
        fichar(solicitud, TipoFichaje.INICIO, LocalDateTime.of(2026, 11, 24, 10, 0));

        List<FichajeEntity> fichajes = repository.findBySolicitudIdOrderByMomentoAsc(solicitud.getId());

        assertThat(fichajes.size(), is(equalTo(2)));
        assertThat(fichajes.get(0).getTipo(), is(equalTo(TipoFichaje.INICIO)));
        assertThat(fichajes.get(1).getTipo(), is(equalTo(TipoFichaje.FIN)));
    }

    @Test
    void testCadaSolicitudSoloVeSusFichajes() {
        SolicitudEntity una = crearSolicitud("una");
        SolicitudEntity otra = crearSolicitud("otra");
        fichar(una, TipoFichaje.INICIO, LocalDateTime.of(2026, 11, 24, 10, 0));

        assertThat(repository.findBySolicitudIdOrderByMomentoAsc(una.getId()).size(), is(equalTo(1)));
        assertThat(repository.findBySolicitudIdOrderByMomentoAsc(otra.getId()).size(), is(equalTo(0)));
    }

    @Test
    void testSabeSiYaSeHaFichadoCadaTipo() {
        SolicitudEntity solicitud = crearSolicitud("existe");
        fichar(solicitud, TipoFichaje.INICIO, LocalDateTime.of(2026, 11, 24, 10, 0));

        assertThat(repository.existsBySolicitudIdAndTipo(solicitud.getId(), TipoFichaje.INICIO), is(true));
        assertThat(repository.existsBySolicitudIdAndTipo(solicitud.getId(), TipoFichaje.FIN), is(false));
    }

    @Test
    void testLaBaseDeDatosNoDejaFicharDosVecesElMismoTipo() {
        SolicitudEntity solicitud = crearSolicitud("duplicado");
        fichar(solicitud, TipoFichaje.INICIO, LocalDateTime.of(2026, 11, 24, 10, 0));

        assertThrows(DataIntegrityViolationException.class,
                () -> fichar(solicitud, TipoFichaje.INICIO, LocalDateTime.of(2026, 11, 24, 11, 0)));
    }

    @Test
    void testGuardaElResultadoDeLaUbicacionSinCoordenadas() {
        SolicitudEntity solicitud = crearSolicitud("resultado");
        FichajeEntity guardado = repository.saveAndFlush(FichajeEntity.builder()
                .solicitud(solicitud)
                .tipo(TipoFichaje.INICIO)
                .momento(LocalDateTime.of(2026, 11, 24, 10, 0))
                .ubicacion(ResultadoUbicacion.FUERA_DEL_LUGAR)
                .build());

        FichajeEntity leido = repository.findById(guardado.getId()).orElseThrow();

        assertThat(leido.getUbicacion(), is(equalTo(ResultadoUbicacion.FUERA_DEL_LUGAR)));
        // La entidad no tiene ningún campo de latitud ni longitud: no hay coordenadas que guardar
        for (java.lang.reflect.Field campo : FichajeEntity.class.getDeclaredFields()) {
            assertThat(campo.getName().toLowerCase().contains("lat"), is(false));
            assertThat(campo.getName().toLowerCase().contains("long"), is(false));
        }
    }
}
