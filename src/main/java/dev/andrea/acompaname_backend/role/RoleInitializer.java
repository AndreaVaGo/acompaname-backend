package dev.andrea.acompaname_backend.role;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class RoleInitializer implements CommandLineRunner {
    private static final List<String> ROLES = List.of("FAMILIA", "CUIDADOR");

    private final RoleRepository repository;

    public RoleInitializer(RoleRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        Set<String> existentes = repository.findAll().stream()
                .map(RoleEntity::getName)
                .collect(Collectors.toSet());

        for (String nombre : ROLES) {
            if (!existentes.contains(nombre)) {
                RoleEntity rol = new RoleEntity();
                rol.setName(nombre);
                repository.save(rol);
            }
        }
    }
}