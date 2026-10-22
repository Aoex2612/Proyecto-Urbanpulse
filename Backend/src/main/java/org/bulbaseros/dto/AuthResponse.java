package org.bulbaseros.dto;

/**
 * Objeto de transferencia de datos (DTO) para la respuesta de autenticación exitosa (login).
 * <p>
 * Proporciona el token JWT emitido, el tipo de token (Bearer), el tiempo de expiración
 * en segundos según RFC 6749 (86.400 s = 24 horas), el rol del usuario autenticado y su nombre completo.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
public class AuthResponse {

    /**
     * Token de autenticación JSON Web Token (JWT) firmado.
     */
    private String token;

    /**
     * Esquema del token de autenticación (por defecto "Bearer").
     */
    private String tokenType = "Bearer";

    /**
     * Tiempo de vida útil del token en segundos (86.400 segundos = 24 horas).
     */
    private Long expiresIn;

    /**
     * Rol asignado al usuario autenticado (ej. "CITIZEN").
     */
    private String role;

    /**
     * Nombre completo del usuario autenticado para visualización en el cliente.
     */
    private String fullName;

    /**
     * Constructor por defecto para deserialización JSON.
     */
    public AuthResponse() {
        this.tokenType = "Bearer";
    }

    /**
     * Constructor con parámetros esenciales, asumiendo tipo "Bearer".
     *
     * @param token     Token JWT emitido.
     * @param expiresIn Tiempo de expiración en segundos.
     * @param role      Rol del usuario autenticado.
     * @param fullName  Nombre completo del usuario.
     */
    public AuthResponse(String token, Long expiresIn, String role, String fullName) {
        this(token, "Bearer", expiresIn, role, fullName);
    }

    /**
     * Constructor completo con todos los atributos de autenticación.
     *
     * @param token     Token JWT emitido.
     * @param tokenType Esquema de autorización (normalmente "Bearer").
     * @param expiresIn Tiempo de expiración en segundos.
     * @param role      Rol del usuario autenticado.
     * @param fullName  Nombre completo del usuario.
     */
    public AuthResponse(String token, String tokenType, Long expiresIn, String role, String fullName) {
        this.token = token;
        this.tokenType = (tokenType != null) ? tokenType : "Bearer";
        this.expiresIn = expiresIn;
        this.role = role;
        this.fullName = fullName;
    }

    /**
     * Obtiene el token JWT emitido.
     *
     * @return Cadena con el token JWT.
     */
    public String getToken() {
        return token;
    }

    /**
     * Establece el token JWT emitido.
     *
     * @param token Cadena con el token JWT.
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * Obtiene el esquema o tipo de token.
     *
     * @return Cadena con el tipo de token (ej. "Bearer").
     */
    public String getTokenType() {
        return tokenType;
    }

    /**
     * Establece el esquema o tipo de token.
     *
     * @param tokenType Cadena con el tipo de token.
     */
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    /**
     * Obtiene el tiempo de expiración del token en segundos.
     *
     * @return Segundos de validez del token.
     */
    public Long getExpiresIn() {
        return expiresIn;
    }

    /**
     * Establece el tiempo de expiración del token en segundos.
     *
     * @param expiresIn Segundos de validez del token.
     */
    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    /**
     * Obtiene el rol del usuario autenticado.
     *
     * @return Nombre del rol como {@link String}.
     */
    public String getRole() {
        return role;
    }

    /**
     * Establece el rol del usuario autenticado.
     *
     * @param role Nombre del rol como {@link String}.
     */
    public void setRole(String role) {
        this.role = role;
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
}
