package dev.andrea.acompaname_backend.servicerequest;

public enum EstadoSolicitud {
    PENDIENTE, ACEPTADA, RECHAZADA, COMPLETADA;

    // Las reglas de los cambios de estado están aquí, en un solo sitio:
    // una pendiente se acepta o se rechaza, y una aceptada se completa.
    // Una rechazada o una completada ya no cambia más.
    public boolean puedePasarA(EstadoSolicitud nuevoEstado) {
        return switch (this) {
            case PENDIENTE -> nuevoEstado == ACEPTADA || nuevoEstado == RECHAZADA;
            case ACEPTADA -> nuevoEstado == COMPLETADA;
            case RECHAZADA, COMPLETADA -> false;
        };
    }

    // Los datos de la solicitud (fechas, tipo, notas...) solo se pueden cambiar mientras está pendiente
    public boolean admiteCambios() {
        return this == PENDIENTE;
    }
}