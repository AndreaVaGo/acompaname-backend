package dev.andrea.acompaname_backend.security;

import static org.springframework.security.config.Customizer.withDefaults;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

    private JpaUserDetailsService jpaUserDetailsService;

    public SecurityConfiguration(JpaUserDetailsService jpaUserDetailsService) {
        this.jpaUserDetailsService = jpaUserDetailsService;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/" + endpoint + "/usuarios").permitAll()
                        .requestMatchers("/" + endpoint + "/roles").permitAll()
                        .requestMatchers("/" + endpoint + "/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/" + endpoint + "/cuidadores/mi-perfil").authenticated()
                        .requestMatchers(HttpMethod.GET, "/" + endpoint + "/cuidadores").permitAll()
                        .requestMatchers(HttpMethod.GET, "/" + endpoint + "/cuidadores/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/" + endpoint + "/cuidadores").hasAuthority("CUIDADOR")
                        .requestMatchers(HttpMethod.POST, "/" + endpoint + "/solicitudes").hasAuthority("FAMILIA")
                        .requestMatchers(HttpMethod.PUT, "/" + endpoint + "/cuidadores/**").hasAuthority("CUIDADOR")
                        .requestMatchers(HttpMethod.DELETE, "/" + endpoint + "/cuidadores/**").hasAuthority("CUIDADOR")
                        .requestMatchers(HttpMethod.PUT, "/" + endpoint + "/solicitudes/**").hasAuthority("FAMILIA")
                        .requestMatchers(HttpMethod.DELETE, "/" + endpoint + "/solicitudes/**").hasAuthority("FAMILIA")
                        .anyRequest().authenticated())
                .userDetailsService(jpaUserDetailsService)
                .httpBasic(withDefaults())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));

        http.headers(header -> header.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}