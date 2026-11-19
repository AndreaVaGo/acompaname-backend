package dev.andrea.acompaname_backend.schedule;

import java.util.List;

import dev.andrea.acompaname_backend.schedule.dtos.BloqueOcupadoDTORequest;
import dev.andrea.acompaname_backend.schedule.dtos.BloqueOcupadoDTOResponse;
import dev.andrea.acompaname_backend.schedule.dtos.ImportacionCalendarioDTOResponse;

public interface BloqueOcupadoService {

    // Guarda un bloque ocupado para la persona cuidadora que tiene la sesión iniciada
    BloqueOcupadoDTOResponse crear(BloqueOcupadoDTORequest dto);

    // Los bloques de la persona cuidadora logueada, por orden de fecha y hora
    List<BloqueOcupadoDTOResponse> listarMios();

    // Borra un bloque; solo puede hacerlo su dueña o dueño
    void borrar(Long id);

    // Lee el texto de un archivo .ics y crea los bloques ocupados de la persona cuidadora logueada.
    // No crea los que ya existen ni los que están en el pasado, y devuelve un resumen
    ImportacionCalendarioDTOResponse importar(String contenido);
}
