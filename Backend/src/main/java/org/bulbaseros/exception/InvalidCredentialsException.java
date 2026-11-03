package org.bulbaseros.exception;

/**
 * Excepción de autenticación lanzada cuando las credenciales suministradas (correo o contraseña)
 * no coinciden con ningún usuario registrado o son erróneas.
 * <p>
 * Se mapea típicamente al código de respuesta HTTP 401 Unauthorized sin revelar si falló el correo o la clave.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
public class InvalidCredentialsException extends RuntimeException {

    /**
     * Construye una nueva excepción con un mensaje genérico de credenciales inválidas.
     */
    public InvalidCredentialsException() {
        super("Credenciales inválidas");
    }

    /**
     * Construye una nueva excepción con el mensaje descriptivo especificado.
     *
     * @param message Mensaje descriptivo del error de autenticación.
     */
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
