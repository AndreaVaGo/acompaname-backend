package dev.andrea.acompaname_backend.caregiverprofile;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import dev.andrea.acompaname_backend.user.UsuarioEntity;

public class PerfilCuidadorEntityTest {

    private UsuarioEntity crearUsuario() {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setId(2L);
        usuario.setNombre("Pepe");
        usuario.setEmail("pepe@test.com");
        return usuario;
    }

    @Test
    void testPerfilNuevoNoTieneDatos() {
        PerfilCuidadorEntity perfil = new PerfilCuidadorEntity();

        assertThat(perfil.getId(), is(nullValue()));
        assertThat(perfil.getEspecialidad(), is(nullValue()));
        assertThat(perfil.getAnosExperiencia(), is(nullValue()));
        assertThat(perfil.getTarifaHora(), is(nullValue()));
        assertThat(perfil.getUsuario(), is(nullValue()));
        assertThat(perfil.isTieneVehiculo(), is(false));
        assertThat(perfil.isDisponibleAhora(), is(false));
    }

    @Test
    void testConstructorConTodosLosDatos() {
        UsuarioEntity usuario = crearUsuario();

        PerfilCuidadorEntity perfil = new PerfilCuidadorEntity(1L, "Geriatría", 4, new BigDecimal("18.00"),
                "Bio", true, true, usuario, "foto", "ambos", "movilidad,medicacion", "MUJER", "Oviedo");

        assertThat(perfil.getId(), is(equalTo(1L)));
        assertThat(perfil.getEspecialidad(), is(equalTo("Geriatría")));
        assertThat(perfil.getAnosExperiencia(), is(equalTo(4)));
        assertThat(perfil.getTarifaHora(), is(equalTo(new BigDecimal("18.00"))));
        assertThat(perfil.getBio(), is(equalTo("Bio")));
        assertThat(perfil.isTieneVehiculo(), is(true));
        assertThat(perfil.isDisponibleAhora(), is(true));
        assertThat(perfil.getUsuario(), is(equalTo(usuario)));
        assertThat(perfil.getFoto(), is(equalTo("foto")));
        assertThat(perfil.getTipoCuidado(), is(equalTo("ambos")));
        assertThat(perfil.getServicios(), is(equalTo("movilidad,medicacion")));
        assertThat(perfil.getSexo(), is(equalTo("MUJER")));
        assertThat(perfil.getCiudad(), is(equalTo("Oviedo")));
    }

    @Test
    void testConstructorSinCiudad() {
        PerfilCuidadorEntity perfil = new PerfilCuidadorEntity(1L, "Geriatría", 4, new BigDecimal("18.00"),
                "Bio", true, true, crearUsuario(), "foto", "ambos", "movilidad", "MUJER");

        assertThat(perfil.getSexo(), is(equalTo("MUJER")));
        assertThat(perfil.getCiudad(), is(nullValue()));
    }

    @Test
    void testConstructorSinSexo() {
        PerfilCuidadorEntity perfil = new PerfilCuidadorEntity(1L, "Geriatría", 4, new BigDecimal("18.00"),
                "Bio", true, true, crearUsuario(), "foto", "ambos", "movilidad");

        assertThat(perfil.getServicios(), is(equalTo("movilidad")));
        assertThat(perfil.getSexo(), is(nullValue()));
        assertThat(perfil.getCiudad(), is(nullValue()));
    }

    @Test
    void testConstructorSinServicios() {
        PerfilCuidadorEntity perfil = new PerfilCuidadorEntity(1L, "Geriatría", 4, new BigDecimal("18.00"),
                "Bio", true, true, crearUsuario(), "foto", "ambos");

        assertThat(perfil.getTipoCuidado(), is(equalTo("ambos")));
        assertThat(perfil.getServicios(), is(nullValue()));
    }

    @Test
    void testConstructorSinTipoDeCuidado() {
        PerfilCuidadorEntity perfil = new PerfilCuidadorEntity(1L, "Geriatría", 4, new BigDecimal("18.00"),
                "Bio", true, true, crearUsuario(), "foto");

        assertThat(perfil.getFoto(), is(equalTo("foto")));
        assertThat(perfil.getTipoCuidado(), is(nullValue()));
    }

    @Test
    void testConstructorSinFoto() {
        PerfilCuidadorEntity perfil = new PerfilCuidadorEntity(1L, "Geriatría", 4, new BigDecimal("18.00"),
                "Bio", true, true, crearUsuario());

        assertThat(perfil.getBio(), is(equalTo("Bio")));
        assertThat(perfil.getFoto(), is(nullValue()));
        assertThat(perfil.getTipoCuidado(), is(nullValue()));
    }

    @Test
    void testSettersYGetters() {
        PerfilCuidadorEntity perfil = new PerfilCuidadorEntity();
        perfil.setId(5L);
        perfil.setEspecialidad("Demencias");
        perfil.setAnosExperiencia(10);
        perfil.setTarifaHora(new BigDecimal("20.50"));
        perfil.setBio("Experiencia en domicilio");
        perfil.setTieneVehiculo(true);
        perfil.setDisponibleAhora(true);
        perfil.setFoto("foto2");
        perfil.setTipoCuidado("domicilio");
        perfil.setServicios("aseo");
        perfil.setSexo("HOMBRE");
        perfil.setCiudad("Gijón");

        assertThat(perfil.getId(), is(equalTo(5L)));
        assertThat(perfil.getEspecialidad(), is(equalTo("Demencias")));
        assertThat(perfil.getAnosExperiencia(), is(equalTo(10)));
        assertThat(perfil.getTarifaHora(), is(equalTo(new BigDecimal("20.50"))));
        assertThat(perfil.getBio(), is(equalTo("Experiencia en domicilio")));
        assertThat(perfil.isTieneVehiculo(), is(true));
        assertThat(perfil.isDisponibleAhora(), is(true));
        assertThat(perfil.getFoto(), is(equalTo("foto2")));
        assertThat(perfil.getTipoCuidado(), is(equalTo("domicilio")));
        assertThat(perfil.getServicios(), is(equalTo("aseo")));
        assertThat(perfil.getSexo(), is(equalTo("HOMBRE")));
        assertThat(perfil.getCiudad(), is(equalTo("Gijón")));
    }

    @Test
    void testBuilder() {
        UsuarioEntity usuario = crearUsuario();

        PerfilCuidadorEntity perfil = PerfilCuidadorEntity.builder()
                .id(7L)
                .especialidad("Geriatría")
                .anosExperiencia(3)
                .tarifaHora(new BigDecimal("15.00"))
                .bio("Bio del builder")
                .tieneVehiculo(false)
                .disponibleAhora(true)
                .usuario(usuario)
                .ciudad("Avilés")
                .build();

        assertThat(perfil.getId(), is(equalTo(7L)));
        assertThat(perfil.getEspecialidad(), is(equalTo("Geriatría")));
        assertThat(perfil.getAnosExperiencia(), is(equalTo(3)));
        assertThat(perfil.getBio(), is(equalTo("Bio del builder")));
        assertThat(perfil.isTieneVehiculo(), is(false));
        assertThat(perfil.isDisponibleAhora(), is(true));
        assertThat(perfil.getUsuario(), is(equalTo(usuario)));
        assertThat(perfil.getCiudad(), is(equalTo("Avilés")));
        assertThat(perfil.getFoto(), is(nullValue()));
    }
}