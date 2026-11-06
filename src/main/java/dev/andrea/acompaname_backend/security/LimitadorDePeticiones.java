package dev.andrea.acompaname_backend.security;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Cuenta las peticiones de cada dirección IP y corta las que pasan del límite.
// El máximo y el tiempo salen de application.properties (rate-limit.*)
@Component
public class LimitadorDePeticiones {

    private final int maximoDePeticiones;
    private final long segundosDeVentana;

    // Para cada clave (por ejemplo "login:127.0.0.1") guarda cuándo hizo sus últimas peticiones
    private final Map<String, List<Instant>> peticiones = new HashMap<>();

    public LimitadorDePeticiones(@Value("${rate-limit.max-requests}") int maximoDePeticiones,
            @Value("${rate-limit.window-seconds}") long segundosDeVentana) {
        this.maximoDePeticiones = maximoDePeticiones;
        this.segundosDeVentana = segundosDeVentana;
    }

    // Si la clave ya ha llegado al máximo en el último tiempo, lanza el error.
    // synchronized: varias personas pueden llamar a la vez y la lista no se puede tocar a la vez
    public synchronized void comprobar(String clave) {
        Instant ahora = Instant.now();
        Instant limite = ahora.minusSeconds(segundosDeVentana);

        List<Instant> lista = peticiones.get(clave);
        if (lista == null) {
            lista = new ArrayList<>();
        }

        // Nos quedamos solo con las peticiones recientes: las antiguas ya no cuentan
        List<Instant> recientes = new ArrayList<>();
        for (Instant momento : lista) {
            if (momento.isAfter(limite)) {
                recientes.add(momento);
            }
        }

        if (recientes.size() >= maximoDePeticiones) {
            peticiones.put(clave, recientes);
            throw new DemasiadasPeticionesException(
                    "Demasiados intentos. Espera un minuto y vuelve a intentarlo.");
        }

        // Esta petición cuenta
        recientes.add(ahora);
        peticiones.put(clave, recientes);
    }
}