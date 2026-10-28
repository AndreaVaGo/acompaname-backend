package dev.andrea.acompaname_backend.globals;

import java.util.ArrayList;
import java.util.List;

// Pasa una lista a un texto con comas (para guardarla en la base de datos) y al revés.
// Se escribe una sola vez aquí para no repetir este código en cada mapper.
public class ListaDeTexto {

    private ListaDeTexto() {
    }

    // Si no hay nada que guardar, devuelve null
    public static String aTexto(List<String> lista) {
        if (lista == null || lista.isEmpty()) {
            return null;
        }
        return String.join(",", lista);
    }

    // Si no hay nada guardado, devuelve una lista vacía (nunca null)
    public static List<String> aLista(String texto) {
        List<String> lista = new ArrayList<>();
        if (texto == null || texto.isBlank()) {
            return lista;
        }
        for (String elemento : texto.split(",")) {
            lista.add(elemento);
        }
        return lista;
    }
}