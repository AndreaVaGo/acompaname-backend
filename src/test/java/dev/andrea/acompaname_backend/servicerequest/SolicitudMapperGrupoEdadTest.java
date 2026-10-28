package dev.andrea.acompaname_backend.servicerequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import dev.andrea.acompaname_backend.caregiverprofile.PerfilCuidadorEntity;
import dev.andrea.acompaname_backend.globals.GrupoEdad;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTORequest;
import dev.andrea.acompaname_backend.servicerequest.dtos.SolicitudDTOResponse;
import dev.andrea.acompaname_backend.servicerequest.mappers.SolicitudMapper;
import dev.andrea.acompaname_backend.user.UsuarioEntity;

public class SolicitudMapperGrupoEdadTest {

    private UsuarioEntity crearFamilia() {
        UsuarioEntity familia = new UsuarioEntity();
        familia.setId(1L);
        familia.setNombre("Ana");
        familia.setEmail("ana@test.com");
        return familia;
    }

    private PerfilCuidadorEntity crearCuidador() {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setId(2L);
        usuario.setNombre("Pepe");
        usuario.setEmail("pepe@test.com");
        return new PerfilCuidadorEntity(1L, "Geriatría", 4, new BigDecimal("18.00"), "Bio", true, true, usuario);
    }

    // Petición con el grupo de edad que le pasemos (puede ser null)
    private SolicitudDTORequest crearRequest(GrupoEdad grupo) {
        return new SolicitudDTORequest("domicilio", "Manuel", "Sin notas", 12, LocalDate.of(2026, 11, 10), null, 1L,
                1L, null, null, grupo);
    }

    @Test
    void testToEntityGuardaElGrupoDeEdad() {
        SolicitudEntity solicitud = SolicitudMapper.toEntity(crearRequest(GrupoEdad.NINOS), crearFamilia(),
                crearCuidador());

        assertThat(solicitud.getGrupoEdad(), is(equalTo(GrupoEdad.NINOS)));
    }

    @Test
    void testToEntitySinGrupoDeEdadGuardaNull() {
        SolicitudEntity solicitud = SolicitudMapper.toEntity(crearRequest(null), crearFamilia(), crearCuidador());

        assertThat(solicitud.getGrupoEdad(), is(nullValue()));
    }

    @Test
    void testToDtoDevuelveElGrupoDeEdad() {
        SolicitudEntity solicitud = SolicitudMapper.toEntity(crearRequest(GrupoEdad.MAYORES), crearFamilia(),
                crearCuidador());

        SolicitudDTOResponse dto = SolicitudMapper.toDTO(solicitud);

        assertThat(dto.grupoEdad(), is(equalTo(GrupoEdad.MAYORES)));
    }

    @Test
    void testToDtoSinGrupoDeEdadDevuelveNull() {
        SolicitudEntity solicitud = SolicitudMapper.toEntity(crearRequest(null), crearFamilia(), crearCuidador());

        SolicitudDTOResponse dto = SolicitudMapper.toDTO(solicitud);

        assertThat(dto.grupoEdad(), is(nullValue()));
    }
}