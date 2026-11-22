package dev.andrea.acompaname_backend.verificacion;

// En qué punto está una verificación. Si la persona nunca ha enviado nada, no existe fila: está "sin verificar"
public enum EstadoVerificacion {
    PENDIENTE,      // La ha enviado y el administrador aún no la ha revisado
    VERIFICADO,     // El administrador la ha comprobado y está bien
    RECHAZADO       // El administrador no ha podido comprobarla (se guarda el motivo)
}
