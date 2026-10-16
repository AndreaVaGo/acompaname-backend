package dev.andrea.acompaname_backend.role;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import dev.andrea.acompaname_backend.user.UsuarioEntity;

public class RoleEntityTest {

    @Test
    void testRolNuevoNoTieneDatos() {
        RoleEntity role = new RoleEntity();

        assertThat(role.getId(), is(nullValue()));
        assertThat(role.getName(), is(nullValue()));
        assertThat(role.getUsers(), is(nullValue()));
    }

    @Test
    void testSettersYGetters() {
        RoleEntity role = new RoleEntity();
        role.setId(1L);
        role.setName("FAMILIA");

        assertThat(role.getId(), is(equalTo(1L)));
        assertThat(role.getName(), is(equalTo("FAMILIA")));
    }

    @Test
    void testUsuariosDelRol() {
        UsuarioEntity usuario = UsuarioEntity.builder().email("ana@test.com").build();
        RoleEntity role = new RoleEntity();
        role.setUsers(Set.of(usuario));

        assertThat(role.getUsers().size(), is(equalTo(1)));
    }

    @Test
    void testDosRolesConLosMismosDatosSonIguales() {
        RoleEntity role1 = new RoleEntity();
        role1.setId(1L);
        role1.setName("FAMILIA");
        RoleEntity role2 = new RoleEntity();
        role2.setId(1L);
        role2.setName("FAMILIA");

        assertThat(role1, is(equalTo(role2)));
        assertThat(role1.hashCode(), is(equalTo(role2.hashCode())));
    }

    @Test
    void testDosRolesDistintosNoSonIguales() {
        RoleEntity role1 = new RoleEntity();
        role1.setId(1L);
        role1.setName("FAMILIA");
        RoleEntity role2 = new RoleEntity();
        role2.setId(2L);
        role2.setName("CUIDADOR");

        assertThat(role1, is(not(equalTo(role2))));
    }

    @Test
    void testToStringContieneElNombre() {
        RoleEntity role = new RoleEntity();
        role.setName("FAMILIA");

        assertThat(role.toString(), containsString("FAMILIA"));
    }
}