package org.bulbaseros.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Objeto de transferencia de datos (DTO) para la solicitud de inicio de sesión de usuarios.
 * <p>
 * Valida la presencia del correo electrónico (con formato válido) y de la contraseña.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
public class LoginRequest {

    /**
     * Correo electrónico del usuario para autenticación.
     */
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato del correo electrónico no es válido")
    private String email;

    /**
     * Contraseña en texto plano para verificar credenciales.
     */
    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    /**
     * Constructor por defecto requerido para deserialización JSON.
     */
    public LoginRequest() {
    }

    /**
     * Constructor parametrizado para instanciación en pruebas o lógica interna.
     *
     * @param email    Correo electrónico del usuario.
     * @param password Contraseña del usuario.
     */
    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    /**
     * Obtiene el correo electrónico del usuario.
     *
     * @return Correo electrónico como {@link String}.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Establece el correo electrónico del usuario.
     *
     * @param email Correo electrónico como {@link String}.
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Obtiene la contraseña del usuario.
     *
     * @return Contraseña como {@link String}.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Establece la contraseña del usuario.
     *
     * @param password Contraseña como {@link String}.
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
