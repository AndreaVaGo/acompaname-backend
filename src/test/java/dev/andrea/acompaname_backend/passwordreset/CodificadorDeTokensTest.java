package dev.andrea.acompaname_backend.passwordreset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class CodificadorDeTokensTest {

    @Test
    void testElTokenTiene43CaracteresValidosParaUnEnlace() {
        String token = CodificadorDeTokens.crear();

        // 32 bytes en Base64 sin relleno son 43 caracteres: letras, números, "-" y "_"
        assertThat(token.length(), is(equalTo(43)));
        assertThat(token.matches("[A-Za-z0-9_-]+"), is(true));
    }

    @Test
    void testDosTokensNuncaSonIguales() {
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            tokens.add(CodificadorDeTokens.crear());
        }

        assertThat(tokens.size(), is(equalTo(1000)));
    }

    @Test
    void testElHashDeUnTextoConocidoEsElDeSha256() {
        // Valor de referencia de SHA-256 para "abc"
        assertThat(CodificadorDeTokens.hash("abc"),
                is(equalTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")));
    }

    @Test
    void testElMismoTokenDaSiempreElMismoHash() {
        assertThat(CodificadorDeTokens.hash("mi-token"), is(equalTo(CodificadorDeTokens.hash("mi-token"))));
    }

    @Test
    void testTokensDistintosDanHashDistinto() {
        assertThat(CodificadorDeTokens.hash("token-a"), is(not(equalTo(CodificadorDeTokens.hash("token-b")))));
    }

    @Test
    void testElHashTiene64CaracteresHexadecimales() {
        String hash = CodificadorDeTokens.hash(CodificadorDeTokens.crear());

        assertThat(hash.length(), is(equalTo(64)));
        assertThat(hash.matches("[0-9a-f]+"), is(true));
    }

    @Test
    void testElHashNoContieneElToken() {
        String token = CodificadorDeTokens.crear();

        assertThat(CodificadorDeTokens.hash(token).contains(token), is(false));
    }
}
