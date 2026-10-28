package dev.andrea.acompaname_backend.caregiverprofile;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.andrea.acompaname_backend.caregiverprofile.dtos.PerfilCuidadorDTORequest;
import dev.andrea.acompaname_backend.caregiverprofile.dtos.PerfilCuidadorDTOResponse;
import dev.andrea.acompaname_backend.caregiverprofile.mappers.PerfilCuidadorMapper;
import dev.andrea.acompaname_backend.globals.GrupoEdad;
import dev.andrea.acompaname_backend.user.UsuarioEntity;

public class PerfilCuidadorMapperTest {

    private UsuarioEntity crearUsuario() {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setId(2L);
        usuario.setNombre("Pepe");
        usuario.setEmail("pepe@test.com");
        return usuario;
    }

    // Crea una petición con los grupos de edad que le pasemos
    private PerfilCuidadorDTORequest crearRequest(List<GrupoEdad> grupos) {
        return new PerfilCuidadorDTORequest("Geriatría", 4, new BigDecimal("18.00"), "Bio", true, true, 2L, null,
                null, null, null, null, grupos);
    }

    @Test
    void testToEntityGuardaLosGruposDeEdadComoTexto() {
        PerfilCuidadorEntity perfil = PerfilCuidadorMapper
                .toEntity(crearRequest(List.of(GrupoEdad.BEBES, GrupoEdad.NINOS)), crearUsuario());

        assertThat(perfil.getGruposEdad(), is(equalTo("BEBES,NINOS")));
    }

    @Test
    void testToEntityQuitaLosGruposRepetidos() {
        PerfilCuidadorEntity perfil = PerfilCuidadorMapper
                .toEntity(crearRequest(List.of(GrupoEdad.NINOS, GrupoEdad.NINOS, GrupoEdad.BEBES)), crearUsuario());

        assertThat(perfil.getGruposEdad(), is(equalTo("NINOS,BEBES")));
    }

    @Test
    void testToEntitySinGruposGuardaNull() {
        PerfilCuidadorEntity perfil = PerfilCuidadorMapper.toEntity(crearRequest(null), crearUsuario());

        assertThat(perfil.getGruposEdad(), is(nullValue()));
    }

    @Test
    void testToEntityConListaVaciaGuardaNull() {
        PerfilCuidadorEntity perfil = PerfilCuidadorMapper.toEntity(crearRequest(List.of()), crearUsuario());

        assertThat(perfil.getGruposEdad(), is(nullValue()));
    }

    @Test
    void testToDtoDevuelveLosGruposComoLista() {
        PerfilCuidadorEntity perfil = new PerfilCuidadorEntity(1L, "Geriatría", 4, new BigDecimal("18.00"), "Bio",
                true, true, crearUsuario());
        perfil.setGruposEdad("MAYORES,ADULTOS");

        PerfilCuidadorDTOResponse dto = PerfilCuidadorMapper.toDTO(perfil);

        assertThat(dto.gruposEdad(), contains(GrupoEdad.MAYORES, GrupoEdad.ADULTOS));
    }

    @Test
    void testToDtoSinGruposDevuelveListaVacia() {
        PerfilCuidadorEntity perfil = new PerfilCuidadorEntity(1L, "Geriatría", 4, new BigDecimal("18.00"), "Bio",
                true, true, crearUsuario());

        PerfilCuidadorDTOResponse dto = PerfilCuidadorMapper.toDTO(perfil);

        assertThat(dto.gruposEdad(), is(empty()));
    }

    @Test
    void testGruposEdadATextoConNullDevuelveNull() {
        assertThat(PerfilCuidadorMapper.gruposEdadATexto(null), is(nullValue()));
    }
}