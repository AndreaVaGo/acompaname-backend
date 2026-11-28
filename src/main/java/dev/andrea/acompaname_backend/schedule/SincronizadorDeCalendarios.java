package dev.andrea.acompaname_backend.schedule;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Cada hora vuelve a leer los calendarios vinculados, para que los cambios de Google, Outlook o iCloud
// lleguen solos. (@EnableScheduling está en ConfiguracionDeTareas)
@Component
public class SincronizadorDeCalendarios {

    private final SuscripcionCalendarioService service;

    public SincronizadorDeCalendarios(SuscripcionCalendarioService service) {
        this.service = service;
    }

    // Por defecto, cada 15 minutos. Se puede cambiar con la variable CALENDARIO_SINCRONIZACION_CRON
    @Scheduled(cron = "${calendario.sincronizacion-cron}")
    public void sincronizarCadaHora() {
        service.sincronizarTodas();
    }
}
