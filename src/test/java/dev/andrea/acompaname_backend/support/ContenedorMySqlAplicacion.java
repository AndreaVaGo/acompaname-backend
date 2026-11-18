package dev.andrea.acompaname_backend.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;

// Base de los tests @SpringBootTest (la aplicación completa) que necesitan un MySQL de verdad.
// Es un contenedor DISTINTO al de los tests de repositorio a propósito: al arrancar, la aplicación
// crea los roles FAMILIA, CUIDADOR y ADMIN, y los tests de repositorio crean los suyos propios.
// Si compartieran base de datos se encontrarían roles repetidos.
// Se arranca una sola vez y se comparte entre las clases que extienden de esta.
public abstract class ContenedorMySqlAplicacion {

    @ServiceConnection
    protected static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    static {
        MYSQL.start();
    }
}
