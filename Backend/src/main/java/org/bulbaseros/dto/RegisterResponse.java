package org.bulbaseros.dto;

/**
 * Objeto de transferencia de datos (DTO) para la respuesta de confirmación de registro de usuario.
 * <p>
 * Contiene el mensaje descriptivo de éxito y la dirección de correo registrada.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
public class RegisterResponse {

    /**
     * Mensaje de confirmación del resultado del registro.
     */
    private String message;

    /**
     * Dirección de correo electrónico registrada.
     */
    private String email;

    /**
     * Constructor por defecto para serialización y deserialización.
     */
    public RegisterResponse() {
    }

    /**
     * Constructor parametrizado con mensaje y correo.
     *
     * @param message Mensaje descriptivo de éxito.
     * @param email   Correo electrónico registrado.
     */
    public RegisterResponse(String message, String email) {
        this.message = message;
        this.email = email;
    }

    /**
     * Obtiene el mensaje descriptivo del resultado.
     *
     * @return Mensaje como {@link String}.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Establece el mensaje descriptivo del resultado.
     *
     * @param message Mensaje como {@link String}.
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Obtiene la dirección de correo registrada.
     *
     * @return Correo electrónico como {@link String}.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Establece la dirección de correo registrada.
     *
     * @param email Correo electrónico como {@link String}.
     */
    public void setEmail(String email) {
        this.email = email;
    }
}
