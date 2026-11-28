package dev.andrea.acompaname_backend.user;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.Test;

import dev.andrea.acompaname_backend.role.RoleEntity;

public class UsuarioEntityTest {

    private RoleEntity crearRol(Long id, String nombre) {
        RoleEntity rol = new RoleEntity();
        rol.setId(id);
        rol.setName(nombre);
        return rol;
    }

    @Test
    void testUsuarioNuevoNoTieneDatos() {
        UsuarioEntity usuario = new UsuarioEntity();

        assertThat(usuario.getId(), is(nullValue()));
        assertThat(usuario.getNombre(), is(nullValue()));
        assertThat(usuario.getEmail(), is(nullValue()));
        assertThat(usuario.getTelefono(), is(nullValue()));
        assertThat(usuario.getPassword(), is(nullValue()));
        assertThat(usuario.getRoles(), is(nullValue()));
    }

    @Test
    void testConstructorConTodosLosDatos() {
        RoleEntity rol = crearRol(1L, "FAMILIA");

        UsuarioEntity usuario = new UsuarioEntity(1L, "Ana", "ana@test.com", "600111222", "1234", Set.of(rol));

        assertThat(usuario.getId(), is(equalTo(1L)));
        assertThat(usuario.getNombre(), is(equalTo("Ana")));
        assertThat(usuario.getEmail(), is(equalTo("ana@test.com")));
        assertThat(usuario.getTelefono(), is(equalTo("600111222")));
        assertThat(usuario.getPassword(), is(equalTo("1234")));
        assertThat(usuario.getRoles().size(), is(equalTo(1)));
    }

    @Test
    void testSettersYGetters() {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setId(2L);
        usuario.setNombre("Pepe");
        usuario.setEmail("pepe@test.com");
        usuario.setTelefono("600333444");
        usuario.setPassword("5678");
        usuario.setRoles(Set.of(crearRol(2L, "CUIDADOR")));

        assertThat(usuario.getId(), is(equalTo(2L)));
        assertThat(usuario.getNombre(), is(equalTo("Pepe")));
        assertThat(usuario.getEmail(), is(equalTo("pepe@test.com")));
        assertThat(usuario.getTelefono(), is(equalTo("600333444")));
        assertThat(usuario.getPassword(), is(equalTo("5678")));
        assertThat(usuario.getRoles().size(), is(equalTo(1)));
    }

    @Test
    void testBuilder() {
        UsuarioEntity usuario = UsuarioEntity.builder()
                .id(3L)
                .nombre("Marta")
                .email("marta@test.com")
                .telefono("600555666")
                .password("abcd")
                .roles(Set.of(crearRol(1L, "FAMILIA")))
                .build();

        assertThat(usuario.getId(), is(equalTo(3L)));
        assertThat(usuario.getNombre(), is(equalTo("Marta")));
        assertThat(usuario.getEmail(), is(equalTo("marta@test.com")));
        assertThat(usuario.getTelefono(), is(equalTo("600555666")));
        assertThat(usuario.getPassword(), is(equalTo("abcd")));
        assertThat(usuario.getRoles().size(), is(equalTo(1)));
    }

    @Test
    void testBuilderSinDatosDejaLosCamposVacios() {
        UsuarioEntity usuario = UsuarioEntity.builder().build();

        assertThat(usuario.getId(), is(nullValue()));
        assertThat(usuario.getEmail(), is(nullValue()));
        assertThat(usuario.getRoles(), is(nullValue()));
    }

    @Test
    void testUsuarioConDosRoles() {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setRoles(Set.of(crearRol(1L, "FAMILIA"), crearRol(2L, "CUIDADOR")));

        assertThat(usuario.getRoles().size(), is(equalTo(2)));
    }

    @Test
    void testUsuarioNuevoNoTieneElSegundoPasoActivado() {
        assertThat(new UsuarioEntity().isDosPasos(), is(false));
        assertThat(new UsuarioEntity(1L, "Ana", "ana@test.com", "600111222", "1234", Set.of()).isDosPasos(),
                is(false));
    }

    @Test
    void testSeVePuedeActivarElSegundoPaso() {
        UsuarioEntity usuario = new UsuarioEntity();

        usuario.setDosPasos(true);

        assertThat(usuario.isDosPasos(), is(true));
    }

    @Test
    void testElConstructorConActivoDejaElSegundoPasoDesactivado() {
        UsuarioEntity usuario = new UsuarioEntity(1L, "Ana", "ana@test.com", "600111222", "1234", Set.of(), false);

        assertThat(usuario.isActivo(), is(false));
        assertThat(usuario.isDosPasos(), is(false));
    }

    @Test
    void testElBuilderDejaElSegundoPasoDesactivadoPorDefecto() {
        assertThat(UsuarioEntity.builder().build().isDosPasos(), is(false));
        assertThat(UsuarioEntity.builder().dosPasos(true).build().isDosPasos(), is(true));
    }

    @Test
    void testFechaAltaEmpiezaVaciaYSeRellenaAlGuardar() {
        UsuarioEntity usuario = new UsuarioEntity();
        assertThat(usuario.getFechaAlta(), is(nullValue()));

        LocalDateTime antes = LocalDateTime.now().minusSeconds(1);
        usuario.asignarFechaAlta();

        assertThat(usuario.getFechaAlta().isAfter(antes), is(true));
    }

    @Test
    void testAsignarFechaAltaNoPisaUnaFechaYaGuardada() {
        LocalDateTime fecha = LocalDateTime.of(2026, 1, 15, 10, 30);
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setFechaAlta(fecha);

        usuario.asignarFechaAlta();

        assertThat(usuario.getFechaAlta(), is(equalTo(fecha)));
    }

    @Test
    void testLosConstructoresAntiguosDejanLaFechaAltaVacia() {
        assertThat(new UsuarioEntity(1L, "Ana", "ana@test.com", "600111222", "1234", Set.of()).getFechaAlta(),
                is(nullValue()));
        assertThat(new UsuarioEntity(1L, "Ana", "ana@test.com", "600111222", "1234", Set.of(), true, false)
                .getFechaAlta(), is(nullValue()));
        assertThat(UsuarioEntity.builder().build().getFechaAlta(), is(nullValue()));
    }

    // ACOM-260
    @Test
    void testUnUsuarioNuevoNoTieneSesionesInvalidadas() {
        assertThat(new UsuarioEntity().getSesionesValidasDesde(), is(nullValue()));
        assertThat(UsuarioEntity.builder().build().getSesionesValidasDesde(), is(nullValue()));
        assertThat(new UsuarioEntity(1L, "Ana", "ana@test.com", "600111222", "1234", Set.of(), true, false)
                .getSesionesValidasDesde(), is(nullValue()));
    }

    @Test
    void testCerrarSesionesApuntaElMomentoActualEnSegundos() {
        UsuarioEntity usuario = new UsuarioEntity();
        Instant antes = Instant.now().minusSeconds(1);

        usuario.cerrarSesiones();

        Instant momento = usuario.getSesionesValidasDesde();
        assertThat(momento.isAfter(antes), is(true));
        assertThat(momento.getNano(), is(0));
        assertThat(momento.isAfter(Instant.now()), is(false));
    }
}
