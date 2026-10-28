package dev.andrea.acompaname_backend.user.validation;

// Imports "static": permiten escribir FIELD en vez de ElementType.FIELD (más corto y legible)
import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Pattern;

/**
 * Regla de contraseña segura, escrita una sola vez y reutilizable en cualquier
 * DTO que reciba una contraseña.
 *
 * Exige entre 8 y 72 caracteres (72 es el límite de BCrypt), con al menos una
 * mayúscula, una minúscula, un número y un carácter especial.
 *
 * Una contraseña nula pasa esta regla; para exigirla se combina con @NotBlank.
 */

// @Documented: la anotación aparece en el Javadoc de las clases donde se use.
@Documented

// @Constraint: le dice a Jakarta "esto es una regla de validación".
// validatedBy = {} significa que no tiene una clase propia que valide: la
// validación la hacen las anotaciones que lleva encima (aquí, @Pattern).
@Constraint(validatedBy = {})

// @Target: en qué sitios se puede poner la anotación.
// FIELD = encima de un campo (los componentes de un record cuentan como campo).
// PARAMETER = en un parámetro de método.
// ANNOTATION_TYPE = encima de otra anotación (permite componer reglas).
@Target({ FIELD, PARAMETER, ANNOTATION_TYPE })

// @Retention(RUNTIME): la anotación sigue existiendo cuando la app está en marcha.
// Sin esto, Jakarta no podría leerla para validar.
@Retention(RUNTIME)

// @ReportAsSingleViolation: si la contraseña falla, devuelve UN solo error con
// el mensaje de esta anotación, en vez del mensaje genérico de @Pattern.
@ReportAsSingleViolation

// @Pattern: la regla real. La expresión regular se lee así:
//   ^                    inicio del texto
//   (?=.*[a-z])          hay al menos una minúscula
//   (?=.*[A-Z])          hay al menos una mayúscula
//   (?=.*\\d)            hay al menos un número (\\ porque en Java hay que duplicar la barra)
//   (?=.*[^A-Za-z0-9])   hay al menos un carácter que no es letra ni número
//   .{8,72}              el texto completo tiene entre 8 y 72 caracteres
//   $                    fin del texto
@Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,72}$")
public @interface PasswordSegura {

    // Mensaje de error por defecto. Quien use la anotación puede cambiarlo:
    // @PasswordSegura(message = "otro mensaje")
    String message() default "La contraseña debe tener entre 8 y 72 caracteres, con mayúscula, minúscula, número y carácter especial";

    // Miembros obligatorios de toda anotación de validación de Jakarta.
    // groups: permite validar solo en ciertas situaciones (no lo usamos).
    Class<?>[] groups() default {};

    // payload: permite adjuntar información extra al error (no lo usamos).
    Class<? extends Payload>[] payload() default {};
}