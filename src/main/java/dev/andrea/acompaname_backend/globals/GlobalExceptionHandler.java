package dev.andrea.acompaname_backend.globals;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import dev.andrea.acompaname_backend.payment.exceptions.PagoExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.payment.exceptions.PagoExceptionConflicto;
import dev.andrea.acompaname_backend.payment.exceptions.PagoExceptionNotFound;
import dev.andrea.acompaname_backend.caregiverprofile.exceptions.PerfilCuidadorExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.caregiverprofile.exceptions.PerfilCuidadorExceptionNotFound;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionFechaInvalida;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionNotFound;
import dev.andrea.acompaname_backend.servicerequest.exceptions.SolicitudExceptionSolapamiento;
import dev.andrea.acompaname_backend.user.exceptions.UsuarioExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.user.exceptions.UsuarioExceptionEmailDuplicado;
import dev.andrea.acompaname_backend.user.exceptions.UsuarioExceptionNotFound;
import dev.andrea.acompaname_backend.review.exceptions.ValoracionExceptionAccesoDenegado;
import dev.andrea.acompaname_backend.review.exceptions.ValoracionExceptionConflicto;
import dev.andrea.acompaname_backend.review.exceptions.ValoracionExceptionNotFound;
import org.springframework.web.HttpRequestMethodNotSupportedException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UsuarioExceptionNotFound.class)
    public ResponseEntity<String> handleUsuarioNotFoundException(UsuarioExceptionNotFound exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handlerGenericException(Exception exception) {
        log.error("Error no controlado", exception);
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

    @ExceptionHandler(SolicitudExceptionFechaInvalida.class)
    public ResponseEntity<String> handleSolicitudFechaInvalidaException(SolicitudExceptionFechaInvalida exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
    }

    @ExceptionHandler(SolicitudExceptionSolapamiento.class)
    public ResponseEntity<String> handleSolicitudSolapamientoException(SolicitudExceptionSolapamiento exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(exception.getMessage());
    }

    @ExceptionHandler(ValoracionExceptionNotFound.class)
    public ResponseEntity<String> handleValoracionNotFoundException(ValoracionExceptionNotFound exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @ExceptionHandler(PagoExceptionNotFound.class)
    public ResponseEntity<String> handlePagoNotFoundException(PagoExceptionNotFound exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @ExceptionHandler(UsuarioExceptionEmailDuplicado.class)
    public ResponseEntity<String> handleUsuarioEmailDuplicadoException(UsuarioExceptionEmailDuplicado exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(exception.getMessage());
    }

    @ExceptionHandler(SolicitudExceptionAccesoDenegado.class)
    public ResponseEntity<String> handleSolicitudAccesoDenegadoException(SolicitudExceptionAccesoDenegado exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(exception.getMessage());
    }

    @ExceptionHandler(PerfilCuidadorExceptionAccesoDenegado.class)
    public ResponseEntity<String> handlePerfilCuidadorAccesoDenegadoException(
            PerfilCuidadorExceptionAccesoDenegado exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(exception.getMessage());
    }

    @ExceptionHandler(UsuarioExceptionAccesoDenegado.class)
    public ResponseEntity<String> handleUsuarioAccesoDenegadoException(UsuarioExceptionAccesoDenegado exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(exception.getMessage());
    }

    @ExceptionHandler(ValoracionExceptionAccesoDenegado.class)
    public ResponseEntity<String> handleValoracionAccesoDenegadoException(ValoracionExceptionAccesoDenegado exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(exception.getMessage());
    }

    @ExceptionHandler(PagoExceptionAccesoDenegado.class)
    public ResponseEntity<String> handlePagoAccesoDenegadoException(PagoExceptionAccesoDenegado exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(exception.getMessage());
    }

    @ExceptionHandler(ValoracionExceptionConflicto.class)
    public ResponseEntity<String> handleValoracionConflictoException(ValoracionExceptionConflicto exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(exception.getMessage());
    }

    @ExceptionHandler(PagoExceptionConflicto.class)
    public ResponseEntity<String> handlePagoConflictoException(PagoExceptionConflicto exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(exception.getMessage());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<String> handleMetodoNoPermitido(HttpRequestMethodNotSupportedException exception) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body("Método no permitido");
    }

    // Un parámetro de la URL con formato incorrecto (por ejemplo, /cuidadores/abc) es un fallo del cliente
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<String> handleParametroConFormatoIncorrecto(MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("El parámetro '" + exception.getName() + "' no tiene un formato válido");
    }

    // Falta un parámetro obligatorio de la URL (por ejemplo, ?inicio=...)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<String> handleParametroQueFalta(MissingServletRequestParameterException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Falta el parámetro '" + exception.getParameterName() + "'");
    }

    // El JSON del cuerpo está mal escrito o tiene un tipo de dato que no encaja
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleCuerpoMalFormado(HttpMessageNotReadableException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("El cuerpo de la petición no es válido");
    }
}