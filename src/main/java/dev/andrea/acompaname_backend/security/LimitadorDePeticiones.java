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

    // Cuándo se borraron por última vez las claves que ya no tienen peticiones recientes
    private Instant ultimaLimpieza = Instant.now();

    public LimitadorDePeticiones(@Value("${rate-limit.max-requests}") int maximoDePeticiones,
            @Value("${rate-limit.window-seconds}") long segundosDeVentana) {
        this.maximoDePeticiones = maximoDePeticiones;
        this.segundosDeVentana = segundosDeVentana;
    }

    // Si la clave ya ha llegado al máximo general en el último tiempo, lanza el error.
    public void comprobar(String clave) {
        comprobar(clave, maximoDePeticiones);
    }

    // Igual, pero con un máximo propio para rutas que necesitan uno distinto al general
    // (por ejemplo, la búsqueda por cercanía). La ventana de tiempo es la misma.
    // synchronized: varias personas pueden llamar a la vez y la lista no se puede tocar a la vez
    public synchronized void comprobar(String clave, int maximo) {
        Instant ahora = Instant.now();
        Instant limite = ahora.minusSeconds(segundosDeVentana);

        limpiarClavesAntiguas(ahora, limite);

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

        if (recientes.size() >= maximo) {
            peticiones.put(clave, recientes);
            throw new DemasiadasPeticionesException(
                    "Demasiados intentos. Espera un minuto y vuelve a intentarlo.");
        }

        // Esta petición cuenta
        recientes.add(ahora);
        peticiones.put(clave, recientes);
    }

    // Borra las claves (direcciones IP) cuyas peticiones ya han caducado todas, para que el mapa
    // no crezca sin límite. Se hace como mucho una vez por ventana de tiempo, no en cada petición
    private void limpiarClavesAntiguas(Instant ahora, Instant limite) {
        if (ultimaLimpieza.isAfter(limite)) {
            return;
        }
        peticiones.values().removeIf(lista -> todasCaducadas(lista, limite));
        ultimaLimpieza = ahora;
    }

    private boolean todasCaducadas(List<Instant> lista, Instant limite) {
        for (Instant momento : lista) {
            if (momento.isAfter(limite)) {
                return false;
            }
        }
        return true;
    }

    // Cuántas claves hay guardadas ahora mismo (para los tests)
    synchronized int numeroDeClaves() {
        return peticiones.size();
    }
}
