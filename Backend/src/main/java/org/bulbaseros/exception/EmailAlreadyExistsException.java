package org.bulbaseros.exception;

/**
 * Excepción de negocio lanzada cuando se intenta registrar un usuario con un correo electrónico
 * que ya se encuentra registrado en el sistema.
 * <p>
 * Se mapea típicamente al código de respuesta HTTP 409 Conflict.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
public class EmailAlreadyExistsException extends RuntimeException {

    /**
     * Construye una nueva excepción con el mensaje descriptivo especificado.
     *
     * @param message Mensaje que describe el conflicto de correo duplicado.
     */
    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}
