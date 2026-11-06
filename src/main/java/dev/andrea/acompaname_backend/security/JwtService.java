package dev.andrea.acompaname_backend.security;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import dev.andrea.acompaname_backend.user.UsuarioEntity;

@Service
public class JwtService {

    // Quién emite los tokens; al comprobar un token se rechazan los de otro emisor
    static final String EMISOR = "acompaname";

    private final JwtEncoder encoder;
    private final long minutosDeValidez;

    public JwtService(JwtEncoder encoder, @Value("${jwt.expiration-minutes}") long minutosDeValidez) {
        this.encoder = encoder;
        this.minutosDeValidez = minutosDeValidez;
    }

    // Crea el token de una persona. Solo guarda su id: el email y los roles se leen
    // de la base de datos en cada petición, así que si cambian se nota al momento
    public String crearToken(UsuarioEntity usuario) {
        Instant ahora = Instant.now();
        Instant caduca = ahora.plusSeconds(minutosDeValidez * 60);

        JwtClaimsSet datos = JwtClaimsSet.builder()
                .issuer(EMISOR)
                .issuedAt(ahora)
                .expiresAt(caduca)
                .subject(String.valueOf(usuario.getId()))
                .build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();

        return encoder.encode(JwtEncoderParameters.from(cabecera, datos)).getTokenValue();
    }
}