package dev.andrea.acompaname_backend.servicerequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.support.ContenedorMySqlRepositorios;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;
import jakarta.persistence.EntityManager;

// Prueba contra un MySQL real (Testcontainers) que cargar la lista de solicitudes no sufre el problema N+1:
// las personas de cada solicitud se cargan todas juntas, así que las consultas no crecen con el número
// de solicitudes (se cuentan con generate_statistics).
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
public class SolicitudListaIntegrationTest extends ContenedorMySqlRepositorios {

    @Autowired
    private SolicitudRepository repository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PerfilCuidadorRepository perfilCuidadorRepository;
    @Autowired
    private EntityManager entityManager;

    private UsuarioEntity crearUsuario(String nombre, String email) {
        UsuarioEntity usuario = UsuarioEntity.builder()
                .nombre(nombre)
                .email(email)
                .telefono("600111222")
                .password("cifrada")
                .build();
        return usuarioRepository.saveAndFlush(usuario);
    }

    private PerfilCuidadorEntity crearCuidador(UsuarioEntity usuario) {
        PerfilCuidadorEntity perfil = PerfilCuidadorEntity.builder()
                .especialidad("Geriatría")
                .anosExperiencia(4)
                .tarifaHora(new BigDecimal("18.00"))
                .bio("Bio")
                .usuario(usuario)
                .build();
        return perfilCuidadorRepository.saveAndFlush(perfil);
    }

    // Cada solicitud tiene dos personas, que se guardan en otra tabla
    private void crearSolicitud(UsuarioEntity familia, PerfilCuidadorEntity cuidador, int diasMas) {
        SolicitudEntity solicitud = SolicitudEntity.builder()
                .tipoCuidado("domicilio")
                .estado(EstadoSolicitud.PENDIENTE)
                .fechaCuidado(LocalDate.of(2027, 1, 1).plusDays(diasMas))
                .horaInicio(LocalTime.of(10, 0))
                .horaFin(LocalTime.of(11, 0))
                .familia(familia)
                .cuidador(cuidador)
                .personas(List.of(new PersonaCuidada("Ana", 80), new PersonaCuidada("Luis", 82)))
                .build();
        repository.saveAndFlush(solicitud);
    }

    private Statistics estadisticas() {
        return entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
    }

    private void empezarAMedir() {
        entityManager.flush();
        entityManager.clear();
        estadisticas().clear();
    }

    // Pide la lista como lo hace el servicio y lee las personas de cada solicitud.
    // Devuelve cuántas consultas ha lanzado Hibernate
    private long consultasAlPedirLaLista(String email, int solicitudesEsperadas) {
        empezarAMedir();
        List<SolicitudEntity> lista = repository.findByParticipante(email);
        for (SolicitudEntity solicitud : lista) {
            assertThat(solicitud.getPersonas().size(), is(equalTo(2)));
        }
        assertThat(lista.size(), is(equalTo(solicitudesEsperadas)));
        return estadisticas().getPrepareStatementCount();
    }

    @Test
    void testLasConsultasNoCrecenConElNumeroDeSolicitudes() {
        UsuarioEntity familia = crearUsuario("Ana", "ana@test.com");
        PerfilCuidadorEntity cuidador = crearCuidador(crearUsuario("Pepe", "pepe@test.com"));
        for (int dia = 0; dia < 3; dia++) {
            crearSolicitud(familia, cuidador, dia);
        }
        long consultasConTres = consultasAlPedirLaLista("ana@test.com", 3);

        for (int dia = 3; dia < 15; dia++) {
            crearSolicitud(familia, cuidador, dia);
        }
        long consultasConQuince = consultasAlPedirLaLista("ana@test.com", 15);

        // Antes salía una consulta más por cada solicitud nueva (12 más). Ahora, las mismas
        assertThat(consultasConQuince, is(equalTo(consultasConTres)));
    }

    @Test
    void testLaListaDelCuidadorTambienTieneLasMismasConsultasConMasSolicitudes() {
        UsuarioEntity familia = crearUsuario("Ana", "ana@test.com");
        PerfilCuidadorEntity cuidador = crearCuidador(crearUsuario("Pepe", "pepe@test.com"));
        crearSolicitud(familia, cuidador, 0);
        crearSolicitud(familia, cuidador, 1);
        long consultasConDos = consultasAlPedirLaLista("pepe@test.com", 2);

        for (int dia = 2; dia < 10; dia++) {
            crearSolicitud(familia, cuidador, dia);
        }
        long consultasConDiez = consultasAlPedirLaLista("pepe@test.com", 10);

        assertThat(consultasConDiez, is(equalTo(consultasConDos)));
    }
}
