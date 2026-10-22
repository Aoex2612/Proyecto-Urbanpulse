package org.bulbaseros.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Objeto de transferencia de datos (DTO) para la solicitud de registro de nuevos usuarios en UrbanPulse.
 * <p>
 * Aplica validaciones declarativas para garantizar que el nombre completo, el correo electrónico y la
 * contraseña cumplan con las reglas de negocio y los límites definidos en la especificación.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
public class RegisterRequest {

    /**
     * Nombre completo del usuario.
     * Obligatorio y con una longitud máxima de 150 caracteres.
     */
    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 150, message = "El nombre completo no puede superar los 150 caracteres")
    private String fullName;

    /**
     * Correo electrónico del usuario.
     * Obligatorio, con formato de email válido y longitud máxima de 150 caracteres.
     */
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato del correo electrónico no es válido")
    @Size(max = 150, message = "El correo electrónico no puede superar los 150 caracteres")
    private String email;

    /**
     * Contraseña en texto plano para el registro.
     * Obligatoria, con un mínimo de 8 caracteres y al menos una letra y un dígito.
     */
    @NotBlank(message = "La contraseña es obligatoria")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
            message = "La contraseña debe tener al menos 8 caracteres y contener al menos una letra y un número"
    )
    private String password;

    /**
     * Constructor por defecto requerido para deserialización JSON.
     */
    public RegisterRequest() {
    }

    /**
     * Constructor con todos los campos para instanciación en pruebas o lógica interna.
     *
     * @param fullName Nombre completo del usuario.
     * @param email    Correo electrónico del usuario.
     * @param password Contraseña elegida por el usuario.
     */
    public RegisterRequest(String fullName, String email, String password) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
    }

    /**
     * Obtiene el nombre completo del usuario.
     *
     * @return Nombre completo como {@link String}.
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Establece el nombre completo del usuario.
     *
     * @param fullName Nombre completo como {@link String}.
     */
    public void setFullName(String fullName) {
        this.fullName = fullName;
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
     * @return Contraseña en texto plano como {@link String}.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Establece la contraseña del usuario.
     *
     * @param password Contraseña en texto plano como {@link String}.
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
