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
// Los dos usan la misma clave secreta (HS256).
@Configuration
public class JwtConfiguration {

    // Una clave HS256 necesita al menos 32 bytes (256 bits)
    static final int LONGITUD_MINIMA_CLAVE = 32;

    private final SecretKey clave;

    public JwtConfiguration(@Value("${jwt.secret}") String secreto) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < LONGITUD_MINIMA_CLAVE) {
            throw new IllegalStateException(
                    "jwt.secret (JWT_SECRET) debe tener al menos " + LONGITUD_MINIMA_CLAVE + " caracteres");
        }
        this.clave = new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(clave));
    }

    @Bean
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(clave)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // Además de la firma y la caducidad, el token tiene que venir de esta aplicación
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(JwtService.EMISOR));
        return decoder;
    }
}