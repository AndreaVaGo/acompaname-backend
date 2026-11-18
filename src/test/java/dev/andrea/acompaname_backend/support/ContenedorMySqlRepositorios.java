package dev.andrea.acompaname_backend.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;

// Base de los tests @DataJpaTest que necesitan un MySQL de verdad.
// Antes cada clase arrancaba su propio contenedor (unos 15 segundos cada una).
// Ahora el contenedor se arranca UNA sola vez y lo comparten todas las clases que extienden de esta.
// No se para a mano: Testcontainers lo apaga solo cuando acaba la ejecución de los tests.
// Estos tests dejan la base limpia porque cada test se deshace al terminar (rollback).
public abstract class ContenedorMySqlRepositorios {

    @ServiceConnection
    protected static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    static {
        MYSQL.start();
    }
}
