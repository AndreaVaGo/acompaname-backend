package dev.andrea.acompaname_backend.caregiverprofile;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import dev.andrea.acompaname_backend.review.ValoracionEntity;
import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.role.RoleRepository;
import dev.andrea.acompaname_backend.review.ValoracionRepository;
import dev.andrea.acompaname_backend.servicerequest.EstadoSolicitud;
import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;
import dev.andrea.acompaname_backend.servicerequest.SolicitudRepository;
import dev.andrea.acompaname_backend.support.ContenedorMySqlRepositorios;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;
import jakarta.persistence.EntityManager;

// Prueba contra un MySQL real (Testcontainers) que la lista de cuidadores no sufre el problema N+1:
// se cuentan las consultas que lanza Hibernate (generate_statistics).
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
public class PerfilCuidadorListaIntegrationTest extends ContenedorMySqlRepositorios {

    @Autowired
    private PerfilCuidadorRepository repository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private SolicitudRepository solicitudRepository;
    @Autowired
    private ValoracionRepository valoracionRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private EntityManager entityManager;

    // Los usuarios tienen roles (en la aplicación real siempre tienen), y los roles son EAGER:
    // sin cargarlos junto al usuario, saldría otra consulta por cada cuidador
    private RoleEntity crearRol() {
        RoleEntity rol = new RoleEntity();
        rol.setName("CUIDADOR");
        return roleRepository.saveAndFlush(rol);
    }

    private UsuarioEntity crearUsuario(String nombre, String email) {
        UsuarioEntity usuario = UsuarioEntity.builder()
                .nombre(nombre)
                .email(email)
                .telefono("600111222")
                .password("cifrada")
                .roles(Set.of(crearRolSiHaceFalta()))
                .build();
        return usuarioRepository.saveAndFlush(usuario);
    }

    // Todos los usuarios de un test comparten el mismo rol
    private RoleEntity rolDelTest;

    private RoleEntity crearRolSiHaceFalta() {
        if (rolDelTest == null) {
            rolDelTest = crearRol();
        }
        return rolDelTest;
    }

    private PerfilCuidadorEntity crearCuidador(String nombre, String email) {
        PerfilCuidadorEntity perfil = PerfilCuidadorEntity.builder()
                .especialidad("Geriatría")
                .anosExperiencia(4)
                .tarifaHora(new BigDecimal("18.00"))
                .bio("Bio")
                .usuario(crearUsuario(nombre, email))
                .build();
        return repository.saveAndFlush(perfil);
    }

    private void crearValoracion(UsuarioEntity familia, PerfilCuidadorEntity cuidador, int puntuacion) {
        SolicitudEntity solicitud = SolicitudEntity.builder()
                .tipoCuidado("domicilio")
                .estado(EstadoSolicitud.COMPLETADA)
                .fechaCuidado(LocalDate.of(2026, 11, 1))
                .familia(familia)
                .cuidador(cuidador)
                .build();
        solicitudRepository.saveAndFlush(solicitud);
        ValoracionEntity valoracion = ValoracionEntity.builder()
                .comentario("Comentario")
                .puntuacion(puntuacion)
                .fecha(LocalDate.of(2026, 11, 2))
                .solicitud(solicitud)
                .build();
        valoracionRepository.saveAndFlush(valoracion);
    }

    private Statistics estadisticas() {
        return entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
    }

    // Deja la base de datos y las estadísticas listas para medir solo lo que viene después
    private void empezarAMedir() {
        entityManager.flush();
        entityManager.clear();
        estadisticas().clear();
    }

    @Test
    void testFindAllConUsuarioHaceUnaSolaConsultaAunqueHayaVariosCuidadores() {
        crearCuidador("Pepe", "pepe@test.com");
        crearCuidador("Marta", "marta@test.com");
        crearCuidador("Luis", "luis@test.com");
        crearCuidador("Ana", "ana@test.com");
        empezarAMedir();

        List<PerfilCuidadorEntity> perfiles = repository.findAllConUsuario();
        // Leemos el nombre y los roles de cada usuario: si no vinieran cargados, aquí saldrían consultas extra
        for (PerfilCuidadorEntity perfil : perfiles) {
            perfil.getUsuario().getNombre();
            perfil.getUsuario().getRoles().size();
        }

        // Un cuidador por perfil (sin repetidos) y una sola consulta en total
        assertThat(perfiles.size(), is(equalTo(4)));
        assertThat(perfiles.get(0).getUsuario().getRoles().size(), is(equalTo(1)));
        assertThat(estadisticas().getPrepareStatementCount(), is(equalTo(1L)));
    }

    @Test
    void testFindAllNormalSiHaceUnaConsultaExtraPorCadaCuidador() {
        crearCuidador("Pepe", "pepe@test.com");
        crearCuidador("Marta", "marta@test.com");
        crearCuidador("Luis", "luis@test.com");
        empezarAMedir();

        List<PerfilCuidadorEntity> perfiles = repository.findAll();
        for (PerfilCuidadorEntity perfil : perfiles) {
            perfil.getUsuario().getNombre();
        }

        // Es el problema que arreglamos: 1 consulta de la lista + 3 de los usuarios
        assertThat(estadisticas().getPrepareStatementCount(), is(greaterThan(1L)));
    }

    @Test
    void testResumenPorCuidadorCalculaMediaYNumeroDeTodosEnUnaConsulta() {
        UsuarioEntity familia = crearUsuario("Familia", "familia@test.com");
        PerfilCuidadorEntity pepe = crearCuidador("Pepe", "pepe@test.com");
        PerfilCuidadorEntity marta = crearCuidador("Marta", "marta@test.com");
        crearCuidador("Luis", "luis@test.com");
        crearValoracion(familia, pepe, 5);
        crearValoracion(familia, pepe, 3);
        crearValoracion(familia, marta, 4);
        empezarAMedir();

        List<Object[]> resumen = valoracionRepository.resumenPorCuidador();

        // Luis no tiene valoraciones, así que no sale
        assertThat(resumen.size(), is(equalTo(2)));
        assertThat(estadisticas().getPrepareStatementCount(), is(equalTo(1L)));
        for (Object[] fila : resumen) {
            Long cuidadorId = (Long) fila[0];
            Double media = (Double) fila[1];
            Long numero = (Long) fila[2];
            if (cuidadorId.equals(pepe.getId())) {
                assertThat(media, is(equalTo(4.0)));
                assertThat(numero, is(equalTo(2L)));
            } else {
                assertThat(cuidadorId, is(equalTo(marta.getId())));
                assertThat(media, is(equalTo(4.0)));
                assertThat(numero, is(equalTo(1L)));
            }
        }
    }

    @Test
    void testResumenPorCuidadorSinValoracionesDevuelveListaVacia() {
        crearCuidador("Luis", "luis@test.com");
        empezarAMedir();

        List<Object[]> resumen = valoracionRepository.resumenPorCuidador();

        assertThat(resumen.isEmpty(), is(true));
    }
}
