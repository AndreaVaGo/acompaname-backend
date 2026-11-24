package dev.andrea.acompaname_backend.caregiverprofile;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import dev.andrea.acompaname_backend.caregiverprofile.dtos.PerfilCuidadorDTOResponse;
import dev.andrea.acompaname_backend.caregiverprofile.mappers.PerfilCuidadorMapper;
import dev.andrea.acompaname_backend.user.UsuarioEntity;

// En las listas y en los perfiles públicos la foto va como dirección, no como imagen en Base64 (ACOM-158)
public class PerfilCuidadorMapperFotoTest {

    private static final String FOTO = "data:image/png;base64,AAAA";

    private PerfilCuidadorEntity perfilConFoto(Long id, String foto) {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setId(id + 100);
        usuario.setNombre("Pepe");
        PerfilCuidadorEntity perfil = new PerfilCuidadorEntity(id, "Geriatría", 4, new BigDecimal("18.00"), "Bio",
                false, false, usuario);
        perfil.setFoto(foto);
        return perfil;
    }

    @Test
    void testToDTOMantieneLaFotoTalComoEstaGuardada() {
        PerfilCuidadorDTOResponse dto = PerfilCuidadorMapper.toDTO(perfilConFoto(5L, FOTO), null, 0L);

        assertThat(dto.foto(), is(equalTo(FOTO)));
    }

    @Test
    void testToDTOPublicoPoneLaDireccionDeLaFotoEnVezDeLaImagen() {
        PerfilCuidadorDTOResponse dto = PerfilCuidadorMapper.toDTOPublico(perfilConFoto(5L, FOTO), null, 0L);

        assertThat(dto.foto(), startsWith("/cuidadores/5/foto?v="));
        assertThat(dto.foto(), not(containsString("base64")));
    }

    @Test
    void testSinFotoNoHayDireccion() {
        assertThat(PerfilCuidadorMapper.toDTOPublico(perfilConFoto(5L, null), null, 0L).foto(), is(nullValue()));
        assertThat(PerfilCuidadorMapper.toDTOPublico(perfilConFoto(5L, " "), null, 0L).foto(), is(nullValue()));
    }

    @Test
    void testLaDireccionCambiaCuandoCambiaLaFoto() {
        String antes = PerfilCuidadorMapper.direccionDeLaFoto(perfilConFoto(5L, FOTO));
        String despues = PerfilCuidadorMapper.direccionDeLaFoto(perfilConFoto(5L, "data:image/png;base64,BBBB"));

        assertThat(antes, is(not(equalTo(despues))));
    }

    @Test
    void testLaMismaFotoDaSiempreLaMismaDireccion() {
        assertThat(PerfilCuidadorMapper.direccionDeLaFoto(perfilConFoto(5L, FOTO)),
                is(equalTo(PerfilCuidadorMapper.direccionDeLaFoto(perfilConFoto(5L, FOTO)))));
    }

    @Test
    void testUnaFotoQueYaEraUnaDireccionExternaSeDejaComoEstaba() {
        PerfilCuidadorDTOResponse dto = PerfilCuidadorMapper
                .toDTOPublico(perfilConFoto(5L, "https://ejemplo.com/foto.jpg"), null, 0L);

        assertThat(dto.foto(), is(equalTo("https://ejemplo.com/foto.jpg")));
    }
}
