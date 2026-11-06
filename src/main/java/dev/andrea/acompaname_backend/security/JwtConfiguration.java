package dev.andrea.acompaname_backend.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

// Configura cómo se firman (JwtEncoder) y se comprueban (JwtDecoder) los tokens del login.
// Los dos usan la misma clave secreta.
@Configuration
public class JwtConfiguration {

    private final SecretKey clave;

    public JwtConfiguration(@Value("${jwt.secret}") String secreto) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        // La clave tiene que ser larga (32 caracteres como mínimo). Si no, la aplicación no arranca
        if (bytes.length < 32) {
            throw new IllegalStateException("jwt.secret (JWT_SECRET) debe tener al menos 32 caracteres");
        }
        this.clave = new SecretKeySpec(bytes, "HmacSHA256");
    }

    // Firma los tokens al hacer login
    @Bean
    JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(clave));
    }

    // Comprueba los tokens que llegan en cada petición: firma, caducidad y emisor
    @Bean
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(clave)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(JwtService.EMISOR));
        return decoder;
    }
}