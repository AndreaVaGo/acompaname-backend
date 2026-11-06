package dev.andrea.acompaname_backend.security;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Value("${api-endpoint}")
    String endpoint;

    // Orígenes del frontend permitidos; salen de la propiedad cors.allowed-origins
    @Value("${cors.allowed-origins}")
    String[] origenesPermitidos;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtUsuarioConverter jwtUsuarioConverter)
            throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/" + endpoint + "/usuarios").permitAll()
                        .requestMatchers(HttpMethod.GET, "/" + endpoint + "/roles").permitAll()
                        // El login es público: aquí es donde la persona consigue su token
                        .requestMatchers(HttpMethod.POST, "/" + endpoint + "/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/" + endpoint + "/cuidadores/mi-perfil").authenticated()
                        .requestMatchers(HttpMethod.GET, "/" + endpoint + "/cuidadores").permitAll()
                        .requestMatchers(HttpMethod.GET, "/" + endpoint + "/cuidadores/*").permitAll()
                        // Las valoraciones de un cuidador se ven sin iniciar sesión (solo puntuación, comentario y fecha)
                        .requestMatchers(HttpMethod.GET, "/" + endpoint + "/valoraciones/cuidador/*").permitAll()
                        // Para filtrar la búsqueda por fechas sin iniciar sesión: solo devuelve ids de cuidadores
                        .requestMatchers(HttpMethod.GET, "/" + endpoint + "/solicitudes/cuidadores-ocupados").permitAll()
                        .requestMatchers(HttpMethod.POST, "/" + endpoint + "/cuidadores").hasAuthority("CUIDADOR")
                        .requestMatchers(HttpMethod.POST, "/" + endpoint + "/solicitudes").hasAuthority("FAMILIA")
                        .requestMatchers(HttpMethod.PUT, "/" + endpoint + "/cuidadores/**").hasAuthority("CUIDADOR")
                        .requestMatchers(HttpMethod.DELETE, "/" + endpoint + "/cuidadores/**").hasAuthority("CUIDADOR")
                        .requestMatchers(HttpMethod.PUT, "/" + endpoint + "/solicitudes/**").hasAuthority("FAMILIA")
                        .requestMatchers(HttpMethod.DELETE, "/" + endpoint + "/solicitudes/**").hasAuthority("FAMILIA")
                        // Todo lo que cuelga de /admin es solo para el administrador
                        .requestMatchers("/" + endpoint + "/admin/**").hasAuthority("ADMIN")
                        .anyRequest().authenticated())
                // Lee el token de la cabecera "Authorization: Bearer ..." y carga a la persona de la base de datos
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtUsuarioConverter)))
                // Sin sesiones: cada petición lleva su token
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.headers(header -> header.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    // Lo usa el login para comprobar el email y la contraseña
    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(origenesPermitidos));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}