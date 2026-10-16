package dev.andrea.acompaname_backend.servicerequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.user.UsuarioEntity;

public class SolicitudEntityTest {

    private UsuarioEntity crearFamilia() {
        UsuarioEntity familia = new UsuarioEntity();
        familia.setId(1L);
        familia.setEmail("ana@test.com");
        return familia;
    }

    private PerfilCuidadorEntity crearCuidador() {
        PerfilCuidadorEntity cuidador = new PerfilCuidadorEntity();
        cuidador.setId(1L);
        cuidador.setEspecialidad("Geriatría");
        return cuidador;
    }

    @Test
    void testSolicitudNuevaNoTieneDatos() {
        SolicitudEntity solicitud = new SolicitudEntity();

        assertThat(solicitud.getId(), is(nullValue()));
        assertThat(solicitud.getTipoCuidado(), is(nullValue()));
        assertThat(solicitud.getNombrePaciente(), is(nullValue()));
        assertThat(solicitud.getEstado(), is(nullValue()));
        assertThat(solicitud.getFamilia(), is(nullValue()));
        assertThat(solicitud.getCuidador(), is(nullValue()));
        assertThat(solicitud.getDireccion(), is(nullValue()));
    }

    @Test
    void testConstructorConTodosLosDatos() {
        UsuarioEntity familia = crearFamilia();
        PerfilCuidadorEntity cuidador = crearCuidador();

        SolicitudEntity solicitud = new SolicitudEntity(1L, "domicilio", "Manuel", "Sin notas", 80,
                LocalDate.of(2026, 10, 21), LocalDate.of(2026, 10, 23), EstadoSolicitud.PENDIENTE, familia, cuidador,
                "movilidad,higiene", "Calle Mayor 1");

        assertThat(solicitud.getId(), is(equalTo(1L)));
        assertThat(solicitud.getTipoCuidado(), is(equalTo("domicilio")));
        assertThat(solicitud.getNombrePaciente(), is(equalTo("Manuel")));
        assertThat(solicitud.getNotas(), is(equalTo("Sin notas")));
        assertThat(solicitud.getEdadPaciente(), is(equalTo(80)));
        assertThat(solicitud.getFechaCuidado(), is(equalTo(LocalDate.of(2026, 10, 21))));
        assertThat(solicitud.getFechaFin(), is(equalTo(LocalDate.of(2026, 10, 23))));
        assertThat(solicitud.getEstado(), is(equalTo(EstadoSolicitud.PENDIENTE)));
        assertThat(solicitud.getFamilia(), is(equalTo(familia)));
        assertThat(solicitud.getCuidador(), is(equalTo(cuidador)));
        assertThat(solicitud.getNecesidades(), is(equalTo("movilidad,higiene")));
        assertThat(solicitud.getDireccion(), is(equalTo("Calle Mayor 1")));
    }

    @Test
    void testConstructorSinDireccion() {
        SolicitudEntity solicitud = new SolicitudEntity(1L, "domicilio", "Manuel", "Sin notas", 80,
                LocalDate.of(2026, 10, 21), LocalDate.of(2026, 10, 23), EstadoSolicitud.PENDIENTE, crearFamilia(),
                crearCuidador(), "movilidad");

        assertThat(solicitud.getNecesidades(), is(equalTo("movilidad")));
        assertThat(solicitud.getDireccion(), is(nullValue()));
    }

    @Test
    void testConstructorSinNecesidades() {
        SolicitudEntity solicitud = new SolicitudEntity(1L, "domicilio", "Manuel", "Sin notas", 80,
                LocalDate.of(2026, 10, 21), LocalDate.of(2026, 10, 23), EstadoSolicitud.ACEPTADA, crearFamilia(),
                crearCuidador());

        assertThat(solicitud.getEstado(), is(equalTo(EstadoSolicitud.ACEPTADA)));
        assertThat(solicitud.getNecesidades(), is(nullValue()));
        assertThat(solicitud.getDireccion(), is(nullValue()));
    }

    @Test
    void testConstructorSinFechaDeFin() {
        SolicitudEntity solicitud = new SolicitudEntity(1L, "hospital", "Manuel", "Sin notas", 80,
                LocalDate.of(2026, 10, 21), EstadoSolicitud.COMPLETADA, crearFamilia(), crearCuidador());

        assertThat(solicitud.getFechaCuidado(), is(equalTo(LocalDate.of(2026, 10, 21))));
        assertThat(solicitud.getFechaFin(), is(nullValue()));
        assertThat(solicitud.getEstado(), is(equalTo(EstadoSolicitud.COMPLETADA)));
    }

    @Test
    void testSettersYGetters() {
        UsuarioEntity familia = crearFamilia();
        PerfilCuidadorEntity cuidador = crearCuidador();
        SolicitudEntity solicitud = new SolicitudEntity();
        solicitud.setId(4L);
        solicitud.setTipoCuidado("hospital");
        solicitud.setNombrePaciente("Rosa");
        solicitud.setNotas("Necesita ayuda para comer");
        solicitud.setEdadPaciente(85);
        solicitud.setFechaCuidado(LocalDate.of(2026, 11, 2));
        solicitud.setFechaFin(LocalDate.of(2026, 11, 4));
        solicitud.setEstado(EstadoSolicitud.RECHAZADA);
        solicitud.setFamilia(familia);
        solicitud.setCuidador(cuidador);
        solicitud.setNecesidades("alimentacion");
        solicitud.setDireccion("Hospital, planta 3");

        assertThat(solicitud.getId(), is(equalTo(4L)));
        assertThat(solicitud.getTipoCuidado(), is(equalTo("hospital")));
        assertThat(solicitud.getNombrePaciente(), is(equalTo("Rosa")));
        assertThat(solicitud.getNotas(), is(equalTo("Necesita ayuda para comer")));
        assertThat(solicitud.getEdadPaciente(), is(equalTo(85)));
        assertThat(solicitud.getFechaCuidado(), is(equalTo(LocalDate.of(2026, 11, 2))));
        assertThat(solicitud.getFechaFin(), is(equalTo(LocalDate.of(2026, 11, 4))));
        assertThat(solicitud.getEstado(), is(equalTo(EstadoSolicitud.RECHAZADA)));
        assertThat(solicitud.getFamilia(), is(equalTo(familia)));
        assertThat(solicitud.getCuidador(), is(equalTo(cuidador)));
        assertThat(solicitud.getNecesidades(), is(equalTo("alimentacion")));
        assertThat(solicitud.getDireccion(), is(equalTo("Hospital, planta 3")));
    }

    @Test
    void testBuilder() {
        UsuarioEntity familia = crearFamilia();
        PerfilCuidadorEntity cuidador = crearCuidador();

        SolicitudEntity solicitud = SolicitudEntity.builder()
                .id(9L)
                .tipoCuidado("domicilio")
                .nombrePaciente("Luis")
                .edadPaciente(78)
                .fechaCuidado(LocalDate.of(2026, 12, 1))
                .estado(EstadoSolicitud.PENDIENTE)
                .familia(familia)
                .cuidador(cuidador)
                .build();

        assertThat(solicitud.getId(), is(equalTo(9L)));
        assertThat(solicitud.getNombrePaciente(), is(equalTo("Luis")));
        assertThat(solicitud.getEdadPaciente(), is(equalTo(78)));
        assertThat(solicitud.getEstado(), is(equalTo(EstadoSolicitud.PENDIENTE)));
        assertThat(solicitud.getFamilia(), is(equalTo(familia)));
        assertThat(solicitud.getCuidador(), is(equalTo(cuidador)));
        assertThat(solicitud.getNotas(), is(nullValue()));
    }

    @Test
    void testCambioDeEstado() {
        SolicitudEntity solicitud = new SolicitudEntity();
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);

        solicitud.setEstado(EstadoSolicitud.ACEPTADA);

        assertThat(solicitud.getEstado(), is(equalTo(EstadoSolicitud.ACEPTADA)));
    }
}