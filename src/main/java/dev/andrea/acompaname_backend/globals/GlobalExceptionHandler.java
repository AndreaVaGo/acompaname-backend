package dev.andrea.acompaname_backend.globals;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import dev.andrea.acompaname_backend.perfilcuidador.exceptions.PerfilCuidadorExceptionNotFound;
import dev.andrea.acompaname_backend.solicitud.exceptions.SolicitudExceptionNotFound;
import dev.andrea.acompaname_backend.usuario.exceptions.UsuarioExceptionEmailDuplicado;
import dev.andrea.acompaname_backend.usuario.exceptions.UsuarioExceptionNotFound;
import dev.andrea.acompaname_backend.valoracion.exceptions.ValoracionExceptionNotFound;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UsuarioExceptionNotFound.class)
    public ResponseEntity<String> handleUsuarioNotFoundException(UsuarioExceptionNotFound exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handlerGenericException(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno del servidor");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(PerfilCuidadorExceptionNotFound.class)
    public ResponseEntity<String> handlePerfilCuidadorNotFoundException(PerfilCuidadorExceptionNotFound exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @ExceptionHandler(SolicitudExceptionNotFound.class)
    public ResponseEntity<String> handlerSolicitudNotFoundException(SolicitudExceptionNotFound exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @ExceptionHandler(ValoracionExceptionNotFound.class)
    public ResponseEntity<String> handleValoracionNotFoundException(ValoracionExceptionNotFound exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @ExceptionHandler(UsuarioExceptionEmailDuplicado.class)
    public ResponseEntity<String> handleUsuarioEmailDuplicadoException(UsuarioExceptionEmailDuplicado exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(exception.getMessage());
    }
}
