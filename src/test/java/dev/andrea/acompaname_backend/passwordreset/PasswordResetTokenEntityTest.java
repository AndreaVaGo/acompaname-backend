package dev.andrea.acompaname_backend.passwordreset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import dev.andrea.acompaname_backend.user.UsuarioEntity;

public class PasswordResetTokenEntityTest {

    private PasswordResetTokenEntity tokenQueCaducaEn(LocalDateTime momento) {
        return PasswordResetTokenEntity.builder()
                .tokenHash("a".repeat(64))
                .usuario(new UsuarioEntity())
                .expiraEn(momento)
                .build();
    }

    @Test
    void testUnTokenConCaducidadFuturaNoEstaCaducado() {
        LocalDateTime ahora = LocalDateTime.of(2026, 12, 1, 10, 0);

        assertThat(tokenQueCaducaEn(ahora.plusMinutes(1)).estaCaducado(ahora), is(false));
    }

    @Test
    void testUnTokenConCaducidadPasadaEstaCaducado() {
        LocalDateTime ahora = LocalDateTime.of(2026, 12, 1, 10, 0);

        assertThat(tokenQueCaducaEn(ahora.minusMinutes(1)).estaCaducado(ahora), is(true));
    }

    @Test
    void testJustoEnElMomentoDeLaCaducidadTodaviaVale() {
        LocalDateTime ahora = LocalDateTime.of(2026, 12, 1, 10, 0);

        assertThat(tokenQueCaducaEn(ahora).estaCaducado(ahora), is(false));
    }

    @Test
    void testElBuilderGuardaLosDatos() {
        UsuarioEntity usuario = new UsuarioEntity();
        LocalDateTime caducidad = LocalDateTime.of(2026, 12, 1, 11, 0);

        PasswordResetTokenEntity token = PasswordResetTokenEntity.builder()
                .tokenHash("b".repeat(64))
                .usuario(usuario)
                .expiraEn(caducidad)
                .build();

        assertThat(token.getTokenHash(), is(equalTo("b".repeat(64))));
        assertThat(token.getUsuario(), is(equalTo(usuario)));
        assertThat(token.getExpiraEn(), is(equalTo(caducidad)));
    }
}
