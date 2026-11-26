package dev.andrea.acompaname_backend.schedule;

import dev.andrea.acompaname_backend.schedule.dtos.SuscripcionCalendarioDTOResponse;

public interface SuscripcionCalendarioService {

    // Cómo está la vinculación de la persona cuidadora logueada (vinculado o no, última lectura, último error)
    SuscripcionCalendarioDTOResponse estado();

    // Vincula (o cambia) el calendario externo. Lo lee al momento: si no se puede leer, da error y no guarda nada
    SuscripcionCalendarioDTOResponse vincular(String url);

    // Vuelve a leer el calendario ahora mismo. Si falla, el error queda en el estado
    SuscripcionCalendarioDTOResponse sincronizarAhora();

    // Quita la vinculación y los bloques que vinieron de ella (los creados a mano se quedan)
    void desvincular();

    // Lo que hace la tarea programada: vuelve a leer todos los calendarios vinculados
    void sincronizarTodas();
}
