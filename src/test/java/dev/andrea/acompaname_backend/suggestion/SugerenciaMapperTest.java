package dev.andrea.acompaname_backend.suggestion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.Test;

import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.suggestion.dtos.AdminSugerenciaDTOResponse;
import dev.andrea.acompaname_backend.suggestion.dtos.SugerenciaDTORequest;
import dev.andrea.acompaname_backend.suggestion.dtos.SugerenciaDTOResponse;
import dev.andrea.acompaname_backend.suggestion.mappers.SugerenciaMapper;
import dev.andrea.acompaname_backend.user.UsuarioEntity;

public class SugerenciaMapperTest {

    private UsuarioEntity crearUsuario(Set<RoleEntity> roles) {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setId(3L);
        usuario.setNombre("Ana");
        usuario.setEmail("ana@test.com");
        usuario.setRoles(roles);
        return usuario;
    }

    private RoleEntity rol(String nombre) {
        RoleEntity rol = new RoleEntity();
        rol.setId(1L);
        rol.setName(nombre);
        return rol;
    }

    @Test
    void testToEntityQuitaLosEspaciosPoneLaFechaYGuardaAQuienLaEscribe() {
        UsuarioEntity usuario = crearUsuario(Set.of(rol("FAMILIA")));

        SugerenciaEntity entity = SugerenciaMapper.toEntity(new SugerenciaDTORequest("  Más colores  "), usuario);

        assertThat(entity.getTexto(), is(equalTo("Más colores")));
        assertThat(entity.getFecha(), is(notNullValue()));
        assertThat(entity.getUsuario(), is(equalTo(usuario)));
        assertThat(entity.getId(), is(equalTo(null)));
    }

    @Test
    void testToDTONoEnsenaQuienLaEscribio() {
        LocalDateTime fecha = LocalDateTime.of(2026, 11, 17, 22, 0);
        SugerenciaEntity entity = new SugerenciaEntity(7L, "Más colores", fecha, crearUsuario(Set.of(rol("FAMILIA"))));

        SugerenciaDTOResponse dto = SugerenciaMapper.toDTO(entity);

        assertThat(dto.id(), is(equalTo(7L)));
        assertThat(dto.texto(), is(equalTo("Más colores")));
        assertThat(dto.fecha(), is(equalTo(fecha)));
    }

    @Test
    void testToAdminDTOIncluyeNombreEmailYRol() {
        LocalDateTime fecha = LocalDateTime.of(2026, 11, 17, 22, 0);
        SugerenciaEntity entity = new SugerenciaEntity(7L, "Más colores", fecha, crearUsuario(Set.of(rol("CUIDADOR"))));

        AdminSugerenciaDTOResponse dto = SugerenciaMapper.toAdminDTO(entity);

        assertThat(dto.id(), is(equalTo(7L)));
        assertThat(dto.texto(), is(equalTo("Más colores")));
        assertThat(dto.nombreUsuario(), is(equalTo("Ana")));
        assertThat(dto.emailUsuario(), is(equalTo("ana@test.com")));
        assertThat(dto.rol(), is(equalTo("CUIDADOR")));
    }

    @Test
    void testToAdminDTOSinRolDejaElRolVacio() {
        SugerenciaEntity sinRoles = new SugerenciaEntity(1L, "Hola", LocalDateTime.now(), crearUsuario(null));
        SugerenciaEntity rolesVacios = new SugerenciaEntity(2L, "Hola", LocalDateTime.now(), crearUsuario(Set.of()));

        assertThat(SugerenciaMapper.toAdminDTO(sinRoles).rol(), is(equalTo("")));
        assertThat(SugerenciaMapper.toAdminDTO(rolesVacios).rol(), is(equalTo("")));
    }
}
