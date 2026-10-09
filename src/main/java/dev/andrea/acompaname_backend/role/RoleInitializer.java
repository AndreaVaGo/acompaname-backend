package dev.andrea.acompaname_backend.role;

import java.util.List;

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
        List<RoleEntity> existentes = repository.findAll();

        for (String nombre : ROLES) {
            boolean yaExiste = false;
            for (RoleEntity rol : existentes) {
                if (rol.getName().equals(nombre)) {
                    yaExiste = true;
                }
            }

            if (!yaExiste) {
                RoleEntity nuevo = new RoleEntity();
                nuevo.setName(nombre);
                repository.save(nuevo);
            }
        }
    }
}