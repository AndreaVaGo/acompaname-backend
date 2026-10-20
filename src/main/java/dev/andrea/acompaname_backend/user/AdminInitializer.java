package dev.andrea.acompaname_backend.user;

import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import dev.andrea.acompaname_backend.role.RoleEntity;
import dev.andrea.acompaname_backend.role.RoleRepository;

// Se ejecuta el segundo (Order 2), cuando RoleInitializer ya ha creado el rol ADMIN
@Component
@Order(2)
public class AdminInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminInitializer(UsuarioRepository usuarioRepository, RoleRepository roleRepository,
            PasswordEncoder passwordEncoder, @Value("${admin.email}") String adminEmail,
            @Value("${admin.password}") String adminPassword) {
        this.usuarioRepository = usuarioRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        // Si no se han definido el email y la contraseña, no se crea ningún administrador
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            return;
        }

        // Si el administrador ya existe, no se vuelve a crear
        if (usuarioRepository.findByEmail(adminEmail).isPresent()) {
            return;
        }

        Optional<RoleEntity> rol = roleRepository.findByName("ADMIN");
        if (rol.isEmpty()) {
            return;
        }

        UsuarioEntity admin = new UsuarioEntity(null, "Administrador", adminEmail, "000000000",
                passwordEncoder.encode(adminPassword), Set.of(rol.get()));
        usuarioRepository.save(admin);
    }
}