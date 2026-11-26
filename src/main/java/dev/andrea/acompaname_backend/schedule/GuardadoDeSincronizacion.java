package dev.andrea.acompaname_backend.schedule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// La parte de la sincronización que toca la base de datos, en una transacción.
// Está separada del servicio para que la descarga (que tarda) NO se haga con la transacción abierta.
@Component
public class GuardadoDeSincronizacion {

    // Lo que se escribe como motivo en los bloques sincronizados: nunca el título de la cita
    static final String MOTIVO_SINCRONIZADO = "Calendario sincronizado";

    private final BloqueOcupadoRepository bloqueRepository;
    private final SuscripcionCalendarioRepository suscripcionRepository;

    public GuardadoDeSincronizacion(BloqueOcupadoRepository bloqueRepository,
            SuscripcionCalendarioRepository suscripcionRepository) {
        this.bloqueRepository = bloqueRepository;
        this.suscripcionRepository = suscripcionRepository;
    }

    // Cambia los bloques FUTUROS que venían del calendario por los de la lectura nueva.
    // Los creados a mano o importados no se tocan, y no se duplica ninguno que ya exista
    @Transactional
    public void aplicar(SuscripcionCalendarioEntity suscripcion, LecturaDeCalendario lectura) {
        Long cuidadorId = suscripcion.getCuidador().getId();
        LocalDate hoy = LocalDate.now();

        bloqueRepository.deleteByCuidadorIdAndMotivoAndFechaGreaterThanEqual(cuidadorId, MOTIVO_SINCRONIZADO, hoy);
        // Sin esto, el borrado podría ejecutarse después de los guardados
        bloqueRepository.flush();

        List<BloqueOcupadoEntity> existentes = new ArrayList<>(
                bloqueRepository.findByCuidadorIdOrderByFechaAscHoraInicioAsc(cuidadorId));
        for (TramoOcupado tramo : lectura.tramos()) {
            if (tramo.fecha().isBefore(hoy) || yaExiste(existentes, tramo)) {
                continue;
            }
            BloqueOcupadoEntity nuevo = BloqueOcupadoEntity.builder()
                    .cuidador(suscripcion.getCuidador())
                    .fecha(tramo.fecha())
                    .horaInicio(tramo.horaInicio())
                    .horaFin(tramo.horaFin())
                    .motivo(MOTIVO_SINCRONIZADO)
                    .build();
            existentes.add(bloqueRepository.save(nuevo));
        }

        suscripcion.setUltimaSincronizacion(LocalDateTime.now());
        suscripcion.setUltimoError(null);
        suscripcionRepository.save(suscripcion);
    }

    // Guarda por qué ha fallado la lectura (sin tocar los bloques que ya había)
    @Transactional
    public void registrarError(SuscripcionCalendarioEntity suscripcion, String mensaje) {
        String texto = mensaje == null ? "No se pudo leer el calendario" : mensaje;
        if (texto.length() > 255) {
            texto = texto.substring(0, 255);
        }
        suscripcion.setUltimoError(texto);
        suscripcionRepository.save(suscripcion);
    }

    // Quita la vinculación y todos los bloques que venían de ella
    @Transactional
    public void desvincular(SuscripcionCalendarioEntity suscripcion) {
        bloqueRepository.deleteByCuidadorIdAndMotivo(suscripcion.getCuidador().getId(), MOTIVO_SINCRONIZADO);
        suscripcionRepository.delete(suscripcion);
    }

    private boolean yaExiste(List<BloqueOcupadoEntity> existentes, TramoOcupado tramo) {
        for (BloqueOcupadoEntity bloque : existentes) {
            if (bloque.getFecha().equals(tramo.fecha()) && bloque.getHoraInicio().equals(tramo.horaInicio())
                    && bloque.getHoraFin().equals(tramo.horaFin())) {
                return true;
            }
        }
        return false;
    }
}
