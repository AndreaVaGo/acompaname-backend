package dev.andrea.acompaname_backend.chat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorRepository;
import dev.andrea.acompaname_backend.servicerequest.EstadoSolicitud;
import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;
import dev.andrea.acompaname_backend.servicerequest.SolicitudRepository;
import dev.andrea.acompaname_backend.support.ContenedorMySqlRepositorios;
import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

@DataJpaTest
public class MensajeRepositoryIntegrationTest extends ContenedorMySqlRepositorios {

    @Autowired
    private MensajeRepository repository;
    @Autowired
    private SolicitudRepository solicitudRepository;
    @Autowired
    private PerfilCuidadorRepository perfilCuidadorRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    private UsuarioEntity crearUsuario(String nombre, String email) {
        return usuarioRepository.saveAndFlush(UsuarioEntity.builder()
                .nombre(nombre).email(email).telefono("600111222").password("cifrada").build());
    }

    private SolicitudEntity crearSolicitud(UsuarioEntity familia, UsuarioEntity cuidador) {
        PerfilCuidadorEntity perfil = perfilCuidadorRepository.saveAndFlush(PerfilCuidadorEntity.builder()
                .especialidad("Geriatría").anosExperiencia(4).tarifaHora(new BigDecimal("18.00"))
                .bio("Bio").usuario(cuidador).build());
        return solicitudRepository.saveAndFlush(SolicitudEntity.builder()
                .tipoCuidado("DOMICILIO").estado(EstadoSolicitud.ACEPTADA).familia(familia).cuidador(perfil)
                .build());
    }

    private MensajeEntity escribir(SolicitudEntity solicitud, UsuarioEntity emisor, String texto) {
        return repository.saveAndFlush(MensajeEntity.builder()
                .solicitud(solicitud).emisor(emisor).texto(texto).fecha(LocalDateTime.now()).build());
    }

    @Test
    void testDevuelveLosMensajesDeLaSolicitudEnOrden() {
        UsuarioEntity ana = crearUsuario("Ana", "ana@test.com");
        UsuarioEntity pepe = crearUsuario("Pepe", "pepe@test.com");
        SolicitudEntity solicitud = crearSolicitud(ana, pepe);
        escribir(solicitud, ana, "Primero");
        escribir(solicitud, pepe, "Segundo");

        List<MensajeEntity> mensajes = repository.findBySolicitudIdOrderByIdAsc(solicitud.getId());

        assertThat(mensajes.stream().map(MensajeEntity::getTexto).toList(), contains("Primero", "Segundo"));
    }

    @Test
    void testNoMezclaLosMensajesDeOtraSolicitud() {
        UsuarioEntity ana = crearUsuario("Ana", "ana@test.com");
        UsuarioEntity pepe = crearUsuario("Pepe", "pepe@test.com");
        UsuarioEntity luis = crearUsuario("Luis", "luis@test.com");
        // Cada persona cuidadora tiene un solo perfil, así que la otra solicitud es de otra persona
        UsuarioEntity marta = crearUsuario("Marta", "marta@test.com");
        SolicitudEntity solicitud = crearSolicitud(ana, pepe);
        SolicitudEntity otra = crearSolicitud(luis, marta);
        escribir(otra, luis, "De otra conversación");

        assertThat(repository.findBySolicitudIdOrderByIdAsc(solicitud.getId()), is(empty()));
    }

    @Test
    void testSoloDevuelveLosMensajesMasNuevosQueElId() {
        UsuarioEntity ana = crearUsuario("Ana", "ana@test.com");
        UsuarioEntity pepe = crearUsuario("Pepe", "pepe@test.com");
        SolicitudEntity solicitud = crearSolicitud(ana, pepe);
        MensajeEntity primero = escribir(solicitud, ana, "Primero");
        escribir(solicitud, pepe, "Segundo");
        escribir(solicitud, ana, "Tercero");

        List<MensajeEntity> nuevos = repository
                .findBySolicitudIdAndIdGreaterThanOrderByIdAsc(solicitud.getId(), primero.getId());

        assertThat(nuevos.stream().map(MensajeEntity::getTexto).toList(), contains("Segundo", "Tercero"));
    }
}
