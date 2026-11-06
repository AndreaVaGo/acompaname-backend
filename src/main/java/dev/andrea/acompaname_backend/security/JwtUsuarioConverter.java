package dev.andrea.acompaname_backend.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;

import dev.andrea.acompaname_backend.user.UsuarioEntity;
import dev.andrea.acompaname_backend.user.UsuarioRepository;

// Se ejecuta en cada petición con token válido: busca a la persona en la base de datos
// por el id del token y la deja "logueada" con su email y sus roles actuales.
// No lleva @Component: se crea en SecurityConfiguration
public class JwtUsuarioConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UsuarioRepository usuarioRepository;

    public JwtUsuarioConverter(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) throws AuthenticationException {
        Long id = Long.valueOf(jwt.getSubject());

        // Si la persona ya no existe, el token no vale
        UsuarioEntity usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new BadCredentialsException("Usuario no encontrado"));

        SecurityUser securityUser = new SecurityUser(usuario);

        // Si el administrador la ha bloqueado, el token deja de valer al momento
        if (!securityUser.isEnabled()) {
            throw new DisabledException("Usuario bloqueado");
        }

        return new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
    }
}