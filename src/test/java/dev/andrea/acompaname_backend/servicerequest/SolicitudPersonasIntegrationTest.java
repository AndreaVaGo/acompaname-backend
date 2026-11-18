package dev.andrea.acompaname_backend.servicerequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.support.ContenedorMySqlRepositorios;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// Prueba contra un MySQL real que las personas a cuidar se guardan en su tabla, en orden, y se borran con la solicitud
@DataJpaTest
public class SolicitudPersonasIntegrationTest extends ContenedorMySqlRepositorios {

    @Autowired
    private SolicitudRepository repository;
    @Autowired
    private PerfilCuidadorRepository perfilCuidadorRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private EntityManager entityManager;

    private SolicitudEntity crearSolicitud() {
        UsuarioEntity familia = usuarioRepository.saveAndFlush(UsuarioEntity.builder().nombre("Ana")
                .email("ana@test.com").telefono("600111222").password("cifrada").build());
        UsuarioEntity usuarioCuidador = usuarioRepository.saveAndFlush(UsuarioEntity.builder().nombre("Pepe")
                .email("pepe@test.com").telefono("600111333").password("cifrada").build());
        PerfilCuidadorEntity cuidador = perfilCuidadorRepository.saveAndFlush(PerfilCuidadorEntity.builder()
                .especialidad("Geriatría").anosExperiencia(4).tarifaHora(new BigDecimal("18.00")).bio("Bio")
                .usuario(usuarioCuidador).build());
        SolicitudEntity solicitud = SolicitudEntity.builder()
                .tipoCuidado("DOMICILIO")
                .estado(EstadoSolicitud.PENDIENTE)
                .fechaCuidado(LocalDate.of(2026, 11, 10))
                .familia(familia)
                .cuidador(cuidador)
                .build();
        return solicitud;
    }

    private long contarPersonasEnLaTabla() {
        Number total = (Number) entityManager.createNativeQuery("select count(*) from solicitud_personas")
                .getSingleResult();
        return total.longValue();
    }

    @Test
    void testLasPersonasSeGuardanYSeLeenEnElMismoOrden() {
        SolicitudEntity solicitud = crearSolicitud();
        solicitud.getPersonas().add(new PersonaCuidada("Zoe", 5));
        solicitud.getPersonas().add(new PersonaCuidada("Ada", 9));
        solicitud.getPersonas().add(new PersonaCuidada("Leo", 2));
        Long id = repository.saveAndFlush(solicitud).getId();
        entityManager.clear();

        SolicitudEntity leida = repository.findById(id).get();

        assertThat(leida.getPersonas().size(), is(equalTo(3)));
        assertThat(leida.getPersonas().get(0).getNombre(), is(equalTo("Zoe")));
        assertThat(leida.getPersonas().get(1).getNombre(), is(equalTo("Ada")));
        assertThat(leida.getPersonas().get(2).getEdad(), is(equalTo(2)));
    }

    @Test
    void testUnaSolicitudSinPersonasSeLeeConListaVacia() {
        Long id = repository.saveAndFlush(crearSolicitud()).getId();
        entityManager.clear();

        SolicitudEntity leida = repository.findById(id).get();

        assertThat(leida.getPersonas().size(), is(equalTo(0)));
    }

    @Test
    void testAlCambiarLaListaSeBorranLasPersonasQueSobran() {
        SolicitudEntity solicitud = crearSolicitud();
        solicitud.getPersonas().add(new PersonaCuidada("Zoe", 5));
        solicitud.getPersonas().add(new PersonaCuidada("Ada", 9));
        Long id = repository.saveAndFlush(solicitud).getId();
        entityManager.clear();

        SolicitudEntity leida = repository.findById(id).get();
        leida.getPersonas().remove(1);
        repository.saveAndFlush(leida);
        entityManager.clear();

        assertThat(repository.findById(id).get().getPersonas().size(), is(equalTo(1)));
        assertThat(contarPersonasEnLaTabla(), is(equalTo(1L)));
    }

    @Test
    void testAlBorrarLaSolicitudSeBorranSusPersonas() {
        SolicitudEntity solicitud = crearSolicitud();
        solicitud.getPersonas().add(new PersonaCuidada("Zoe", 5));
        solicitud.getPersonas().add(new PersonaCuidada("Ada", 9));
        Long id = repository.saveAndFlush(solicitud).getId();
        entityManager.clear();

        repository.deleteById(id);
        repository.flush();

        assertThat(contarPersonasEnLaTabla(), is(equalTo(0L)));
    }

    @Test
    void testCadaPersonaGuardaSuGrupoDeEdad() {
        SolicitudEntity solicitud = crearSolicitud();
        solicitud.getPersonas().add(new PersonaCuidada("Zoe", 5, dev.andrea.acompaname_backend.globals.GrupoEdad.NINOS));
        solicitud.getPersonas().add(new PersonaCuidada("Abuelo", 80, dev.andrea.acompaname_backend.globals.GrupoEdad.MAYORES));
        solicitud.getPersonas().add(new PersonaCuidada("Sin grupo", 40));
        Long id = repository.saveAndFlush(solicitud).getId();
        entityManager.clear();

        SolicitudEntity leida = repository.findById(id).get();

        assertThat(leida.getPersonas().get(0).getGrupoEdad(),
                is(equalTo(dev.andrea.acompaname_backend.globals.GrupoEdad.NINOS)));
        assertThat(leida.getPersonas().get(1).getGrupoEdad(),
                is(equalTo(dev.andrea.acompaname_backend.globals.GrupoEdad.MAYORES)));
        assertThat(leida.getPersonas().get(2).getGrupoEdad(), is(org.hamcrest.Matchers.nullValue()));
    }
}
