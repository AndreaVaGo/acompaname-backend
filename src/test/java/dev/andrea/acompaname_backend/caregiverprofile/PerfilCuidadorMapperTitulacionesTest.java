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
import dev.andrea.acompaname_backend.globals.Titulacion;
import dev.andrea.acompaname_backend.user.UsuarioEntity;

public class PerfilCuidadorMapperTitulacionesTest {

    private UsuarioEntity crearUsuario() {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setId(2L);
        usuario.setNombre("Pepe");
        usuario.setEmail("pepe@test.com");
        return usuario;
    }

    // Crea una petición con las titulaciones y el texto de otras titulaciones que le pasemos
    private PerfilCuidadorDTORequest crearRequest(List<Titulacion> titulaciones, String otras) {
        return new PerfilCuidadorDTORequest("Geriatría", 4, new BigDecimal("18.00"), "Bio", true, true, 2L, null,
                null, null, null, null, null, titulaciones, otras);
    }

    @Test
    void testToEntityGuardaLasTitulacionesComoTexto() {
        PerfilCuidadorEntity perfil = PerfilCuidadorMapper.toEntity(
                crearRequest(List.of(Titulacion.ENFERMERIA, Titulacion.PRIMEROS_AUXILIOS), null), crearUsuario());

        assertThat(perfil.getTitulaciones(), is(equalTo("ENFERMERIA,PRIMEROS_AUXILIOS")));
    }

    @Test
    void testToEntityQuitaLasTitulacionesRepetidas() {
        PerfilCuidadorEntity perfil = PerfilCuidadorMapper.toEntity(
                crearRequest(List.of(Titulacion.ENFERMERIA, Titulacion.ENFERMERIA, Titulacion.AUXILIAR_ENFERMERIA),
                        null),
                crearUsuario());

        assertThat(perfil.getTitulaciones(), is(equalTo("ENFERMERIA,AUXILIAR_ENFERMERIA")));
    }

    @Test
    void testToEntitySinTitulacionesGuardaNull() {
        PerfilCuidadorEntity perfil = PerfilCuidadorMapper.toEntity(crearRequest(null, null), crearUsuario());

        assertThat(perfil.getTitulaciones(), is(nullValue()));
    }

    @Test
    void testToEntityConListaVaciaGuardaNull() {
        PerfilCuidadorEntity perfil = PerfilCuidadorMapper.toEntity(crearRequest(List.of(), null), crearUsuario());

        assertThat(perfil.getTitulaciones(), is(nullValue()));
    }

    @Test
    void testToEntityGuardaLasOtrasTitulacionesSinEspaciosSobrantes() {
        PerfilCuidadorEntity perfil = PerfilCuidadorMapper
                .toEntity(crearRequest(null, "  Curso de geriatría  "), crearUsuario());

        assertThat(perfil.getOtrasTitulaciones(), is(equalTo("Curso de geriatría")));
    }

    @Test
    void testToEntityConOtrasTitulacionesEnBlancoGuardaNull() {
        PerfilCuidadorEntity perfil = PerfilCuidadorMapper.toEntity(crearRequest(null, "   "), crearUsuario());

        assertThat(perfil.getOtrasTitulaciones(), is(nullValue()));
    }

    @Test
    void testToDtoDevuelveLasTitulacionesComoLista() {
        PerfilCuidadorEntity perfil = new PerfilCuidadorEntity(1L, "Geriatría", 4, new BigDecimal("18.00"), "Bio",
                true, true, crearUsuario());
        perfil.setTitulaciones("ATENCION_DEPENDENCIA,ATENCION_SOCIOSANITARIA");
        perfil.setOtrasTitulaciones("Curso de geriatría");

        PerfilCuidadorDTOResponse dto = PerfilCuidadorMapper.toDTO(perfil);

        assertThat(dto.titulaciones(),
                contains(Titulacion.ATENCION_DEPENDENCIA, Titulacion.ATENCION_SOCIOSANITARIA));
        assertThat(dto.otrasTitulaciones(), is(equalTo("Curso de geriatría")));
    }

    @Test
    void testToDtoSinTitulacionesDevuelveListaVaciaYOtrasNull() {
        PerfilCuidadorEntity perfil = new PerfilCuidadorEntity(1L, "Geriatría", 4, new BigDecimal("18.00"), "Bio",
                true, true, crearUsuario());

        PerfilCuidadorDTOResponse dto = PerfilCuidadorMapper.toDTO(perfil);

        assertThat(dto.titulaciones(), is(empty()));
        assertThat(dto.otrasTitulaciones(), is(nullValue()));
    }

    @Test
    void testTitulacionesATextoConNullDevuelveNull() {
        assertThat(PerfilCuidadorMapper.titulacionesATexto(null), is(nullValue()));
    }
}
