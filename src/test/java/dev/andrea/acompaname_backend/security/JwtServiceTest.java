package dev.andrea.acompaname_backend.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;

import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.user.UsuarioEntity;

// Tests de los tokens del login con la configuración de verdad (sin Spring): qué llevan
// y que se rechazan los caducados, los de otra clave, los de otro emisor y los manipulados.
public class JwtServiceTest {

    private static final String CLAVE = "clave-de-pruebas-con-mas-de-32-caracteres";
    private static final String OTRA_CLAVE = "otra-clave-distinta-con-mas-de-32-caracteres";

    private final JwtConfiguration configuracion = new JwtConfiguration(CLAVE);
    private final JwtDecoder decoder = configuracion.jwtDecoder();
    private final JwtService service = new JwtService(configuracion.jwtEncoder(), 120);

    private UsuarioEntity ana() {
        RoleEntity rol = new RoleEntity();
        rol.setId(1L);
        rol.setName("FAMILIA");
        return new UsuarioEntity(7L, "Ana", "ana@test.com", "600111222", "1234", Set.of(rol));
    }

    @Test
    void testElTokenLlevaElIdDeLaPersonaComoSujeto() {
        Jwt jwt = decoder.decode(service.crearToken(ana()));

        assertThat(jwt.getSubject(), is(equalTo("7")));
    }

    @Test
    void testElTokenLlevaElEmisorDeLaAplicacion() {
        Jwt jwt = decoder.decode(service.crearToken(ana()));

        assertThat(jwt.getIssuer().toString(), is(equalTo("acompaname")));
    }

    @Test
    void testElTokenCaducaALosMinutosConfigurados() {
        Jwt jwt = decoder.decode(service.crearToken(ana()));

        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()).toMinutes(), is(equalTo(120L)));
    }

    // Los datos de la persona y sus roles se leen de la base de datos en cada petición, no del token
    @Test
    void testElTokenNoLlevaEmailNiContrasenaNiRoles() {
        Jwt jwt = decoder.decode(service.crearToken(ana()));

        assertThat(jwt.getClaims().containsKey("email"), is(equalTo(false)));
        assertThat(jwt.getClaims().containsKey("password"), is(equalTo(false)));
        assertThat(jwt.getClaims().containsKey("roles"), is(equalTo(false)));
        assertThat(jwt.getClaims().containsKey("authorities"), is(equalTo(false)));
    }

    @Test
    void testUnTokenCaducadoSeRechaza() {
        // Spring admite 60 segundos de margen, por eso caduca hace 5 minutos
        JwtService caducado = new JwtService(configuracion.jwtEncoder(), -5);

        assertThrows(JwtException.class, () -> decoder.decode(caducado.crearToken(ana())));
    }

    @Test
    void testUnTokenFirmadoConOtraClaveSeRechaza() {
        JwtConfiguration otra = new JwtConfiguration(OTRA_CLAVE);
        String tokenAjeno = new JwtService(otra.jwtEncoder(), 120).crearToken(ana());

        assertThrows(JwtException.class, () -> decoder.decode(tokenAjeno));
    }

    @Test
    void testUnTokenDeOtroEmisorSeRechaza() {
        Instant ahora = Instant.now();
        JwtClaimsSet datos = JwtClaimsSet.builder()
                .issuer("otra-aplicacion")
                .issuedAt(ahora)
                .expiresAt(ahora.plusSeconds(3600))
                .subject("7")
                .build();
        String tokenDeOtroEmisor = configuracion.jwtEncoder()
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), datos))
                .getTokenValue();

        assertThrows(JwtException.class, () -> decoder.decode(tokenDeOtroEmisor));
    }

    @Test
    void testUnTokenManipuladoSeRechaza() {
        String token = service.crearToken(ana());
        // Se cambian los últimos caracteres de la firma
        String manipulado = token.substring(0, token.length() - 8) + "AAAAAAAA";

        assertThrows(JwtException.class, () -> decoder.decode(manipulado));
    }

    @Test
    void testUnTextoCualquieraNoEsUnToken() {
        assertThrows(JwtException.class, () -> decoder.decode("esto-no-es-un-token"));
    }
}