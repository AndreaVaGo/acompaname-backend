package dev.andrea.acompaname_backend.chat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;

import dev.andrea.acompaname_backend.chat.dtos.MensajeDTORequest;
import dev.andrea.acompaname_backend.chat.dtos.MensajeDTOResponse;
import dev.andrea.acompaname_backend.chat.mappers.MensajeMapper;
import dev.andrea.acompaname_backend.servicerequest.SolicitudEntity;
import dev.andrea.acompaname_backend.user.UsuarioEntity;

public class MensajeMapperTest {

    private UsuarioEntity usuario(Long id, String nombre, String email) {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setId(id);
        usuario.setNombre(nombre);
        usuario.setEmail(email);
        return usuario;
    }

    private SolicitudEntity solicitud() {
        return SolicitudEntity.builder().id(7L).build();
    }

    @Test
    void testToEntityQuitaLosEspaciosDeLosLados() {
        MensajeEntity mensaje = MensajeMapper.toEntity(new MensajeDTORequest("  Hola, buenas  "), solicitud(),
                usuario(1L, "Ana", "ana@test.com"));

        assertThat(mensaje.getTexto(), is(equalTo("Hola, buenas")));
    }

    @Test
    void testToEntityPoneLaFechaElServidorYGuardaLosDatos() {
        UsuarioEntity ana = usuario(1L, "Ana", "ana@test.com");

        MensajeEntity mensaje = MensajeMapper.toEntity(new MensajeDTORequest("Hola"), solicitud(), ana);

        assertThat(mensaje.getFecha(), is(notNullValue()));
        assertThat(mensaje.getEmisor(), is(equalTo(ana)));
        assertThat(mensaje.getSolicitud().getId(), is(equalTo(7L)));
    }

    @Test
    void testToDTOMarcaComoMioElMensajeDeQuienConsulta() {
        MensajeEntity mensaje = MensajeMapper.toEntity(new MensajeDTORequest("Hola"), solicitud(),
                usuario(1L, "Ana", "ana@test.com"));
        mensaje.setId(3L);

        MensajeDTOResponse suyo = MensajeMapper.toDTO(mensaje, "ana@test.com");
        MensajeDTOResponse ajeno = MensajeMapper.toDTO(mensaje, "pepe@test.com");

        assertThat(suyo.mio(), is(equalTo(true)));
        assertThat(ajeno.mio(), is(equalTo(false)));
        assertThat(suyo.id(), is(equalTo(3L)));
        assertThat(suyo.solicitudId(), is(equalTo(7L)));
        assertThat(suyo.emisorNombre(), is(equalTo("Ana")));
        assertThat(suyo.texto(), is(equalTo("Hola")));
    }
}
