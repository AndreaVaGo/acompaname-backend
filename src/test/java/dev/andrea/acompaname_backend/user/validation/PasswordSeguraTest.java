package dev.andrea.acompaname_backend.user.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

/**
 * Tests unitarios de la anotación @PasswordSegura.
 * No arrancan Spring: usan directamente el validador de Jakarta, así que son rápidos.
 */
class PasswordSeguraTest {

    // Clase mínima solo para los tests: un DTO con un único campo anotado.
    // Es un record, igual que los DTO reales del proyecto.
    private record DtoDePrueba(@PasswordSegura String password) {
    }

    private static Validator validador;

    // Se ejecuta una sola vez antes de todos los tests: crea el validador de Jakarta.
    @BeforeAll
    static void crearValidador() {
        validador = Validation.buildDefaultValidatorFactory().getValidator();
    }

    // Método auxiliar: valida una contraseña y devuelve los errores encontrados.
    // Si la lista está vacía, la contraseña es válida.
    private Set<ConstraintViolation<DtoDePrueba>> validar(String password) {
        return validador.validate(new DtoDePrueba(password));
    }

    // @ParameterizedTest + @ValueSource: el mismo test se ejecuta una vez por cada valor.
    @ParameterizedTest
    @ValueSource(strings = {
            "Abcdef1!", // justo 8 caracteres, cumple todo
            "Contraseña1!", // con ñ (cuenta como letra minúscula)
            "Secreta#2026", // símbolo distinto
            "aB3$aB3$aB3$" // caracteres repetidos
    })
    void aceptaContraseñasQueCumplenTodosLosRequisitos(String password) {
        assertTrue(validar(password).isEmpty(), "Debería ser válida: " + password);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Ab1!", // demasiado corta (menos de 8)
            "abcdefg1!", // sin mayúscula
            "ABCDEFG1!", // sin minúscula
            "Abcdefgh!", // sin número
            "Abcdefg12", // sin carácter especial
            "        ", // solo espacios
            "" // vacía
    })
    void rechazaContraseñasQueNoCumplenAlgúnRequisito(String password) {
        assertEquals(1, validar(password).size(), "Debería ser inválida: '" + password + "'");
    }

    @Test
    void aceptaUnaContraseñaDeExactamente72Caracteres() {
        // 4 caracteres que cumplen las reglas + 68 de relleno = 72 (el límite de BCrypt)
        String password = "Aa1!" + "a".repeat(68);
        assertEquals(72, password.length());
        assertTrue(validar(password).isEmpty());
    }

    @Test
    void rechazaUnaContraseñaDe73Caracteres() {
        // Un carácter por encima del límite de BCrypt
        String password = "Aa1!" + "a".repeat(69);
        assertEquals(73, password.length());
        assertEquals(1, validar(password).size());
    }

    @Test
    void elMensajeDeErrorIndicaLosRequisitos() {
        String mensaje = validar("abc").iterator().next().getMessage();
        assertTrue(mensaje.contains("mayúscula") && mensaje.contains("carácter especial"));
    }

    @Test
    void laContraseñaNulaPasaEstaReglaPorqueLaExigeNotBlank() {
        // @PasswordSegura no se ocupa de los nulos; de eso se encarga @NotBlank en los DTO
        assertTrue(validar(null).isEmpty());
    }
}