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

    @Test
    void testUnMensajeNuevoQuedaSinLeer() {
        UsuarioEntity ana = crearUsuario("Ana", "ana@test.com");
        UsuarioEntity pepe = crearUsuario("Pepe", "pepe@test.com");
        SolicitudEntity solicitud = crearSolicitud(ana, pepe);

        MensajeEntity mensaje = escribir(solicitud, ana, "Hola");

        assertThat(repository.findById(mensaje.getId()).get().isLeido(), is(false));
    }

    @Test
    void testSinLeerDevuelveSoloLosMensajesDeLaOtraPersona() {
        UsuarioEntity ana = crearUsuario("Ana", "ana@test.com");
        UsuarioEntity pepe = crearUsuario("Pepe", "pepe@test.com");
        SolicitudEntity solicitud = crearSolicitud(ana, pepe);
        escribir(solicitud, ana, "De Ana");
        escribir(solicitud, pepe, "De Pepe");

        // Ana solo tiene sin leer lo que escribió Pepe
        List<MensajeEntity> sinLeerAna = repository
                .findBySolicitudIdAndEmisorIdNotAndLeidoFalse(solicitud.getId(), ana.getId());

        assertThat(sinLeerAna.stream().map(MensajeEntity::getTexto).toList(), contains("De Pepe"));
    }

    @Test
    void testSinLeerNoIncluyeLosMensajesYaLeidos() {
        UsuarioEntity ana = crearUsuario("Ana", "ana@test.com");
        UsuarioEntity pepe = crearUsuario("Pepe", "pepe@test.com");
        SolicitudEntity solicitud = crearSolicitud(ana, pepe);
        MensajeEntity leido = escribir(solicitud, pepe, "Ya leído");
        leido.setLeido(true);
        repository.saveAndFlush(leido);
        escribir(solicitud, pepe, "Sin leer");

        List<MensajeEntity> sinLeerAna = repository
                .findBySolicitudIdAndEmisorIdNotAndLeidoFalse(solicitud.getId(), ana.getId());

        assertThat(sinLeerAna.stream().map(MensajeEntity::getTexto).toList(), contains("Sin leer"));
    }

    @Test
    void testNoLeidosDeUnaPersonaIncluyeSolicitudesComoFamiliaYComoCuidadora() {
        UsuarioEntity ana = crearUsuario("Ana", "ana@test.com");
        UsuarioEntity pepe = crearUsuario("Pepe", "pepe@test.com");
        UsuarioEntity luis = crearUsuario("Luis", "luis@test.com");
        // Pepe es cuidador en la solicitud de Ana y Luis es familia en otra
        SolicitudEntity deAna = crearSolicitud(ana, pepe);
        UsuarioEntity marta = crearUsuario("Marta", "marta@test.com");
        SolicitudEntity deLuis = crearSolicitud(luis, marta);
        escribir(deAna, ana, "Ana a Pepe");
        escribir(deLuis, marta, "Marta a Luis");

        List<MensajeEntity> deLaFamilia = repository.findNoLeidosDe("luis@test.com");
        List<MensajeEntity> deLaCuidadora = repository.findNoLeidosDe("pepe@test.com");

        assertThat(deLaFamilia.stream().map(MensajeEntity::getTexto).toList(), contains("Marta a Luis"));
        assertThat(deLaCuidadora.stream().map(MensajeEntity::getTexto).toList(), contains("Ana a Pepe"));
    }

    @Test
    void testNoLeidosNoIncluyeLosMensajesDeSolicitudesAjenas() {
        UsuarioEntity ana = crearUsuario("Ana", "ana@test.com");
        UsuarioEntity pepe = crearUsuario("Pepe", "pepe@test.com");
        UsuarioEntity luis = crearUsuario("Luis", "luis@test.com");
        UsuarioEntity marta = crearUsuario("Marta", "marta@test.com");
        crearSolicitud(ana, pepe);
        SolicitudEntity deLuis = crearSolicitud(luis, marta);
        escribir(deLuis, marta, "Marta a Luis");

        assertThat(repository.findNoLeidosDe("ana@test.com"), is(empty()));
    }

    @Test
    void testExisteMensajeSinLeerDeUnaPersonaSoloSiLoEscribioElla() {
        UsuarioEntity ana = crearUsuario("Ana", "ana@test.com");
        UsuarioEntity pepe = crearUsuario("Pepe", "pepe@test.com");
        SolicitudEntity solicitud = crearSolicitud(ana, pepe);
        escribir(solicitud, ana, "Hola");

        assertThat(repository.existsBySolicitudIdAndEmisorIdAndLeidoFalse(solicitud.getId(), ana.getId()), is(true));
        assertThat(repository.existsBySolicitudIdAndEmisorIdAndLeidoFalse(solicitud.getId(), pepe.getId()), is(false));
    }

    @Test
    void testNoExisteMensajeSinLeerSiYaSeLeyo() {
        UsuarioEntity ana = crearUsuario("Ana", "ana@test.com");
        UsuarioEntity pepe = crearUsuario("Pepe", "pepe@test.com");
        SolicitudEntity solicitud = crearSolicitud(ana, pepe);
        MensajeEntity mensaje = escribir(solicitud, ana, "Hola");
        mensaje.setLeido(true);
        repository.saveAndFlush(mensaje);

        assertThat(repository.existsBySolicitudIdAndEmisorIdAndLeidoFalse(solicitud.getId(), ana.getId()), is(false));
    }
}
