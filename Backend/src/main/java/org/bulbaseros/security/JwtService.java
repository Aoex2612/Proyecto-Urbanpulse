package org.bulbaseros.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.bulbaseros.entity.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Servicio encargado de la generación, extracción y validación de tokens JWT (JSON Web Tokens).
 * <p>
 * Emplea el algoritmo HMAC-SHA256 para la firma criptográfica de los tokens y gestiona claims
 * estándar como el sujeto ({@code sub}) correspondiente al correo del usuario y claims
 * personalizados como el rol funcional ({@code role}).
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    /**
     * Constructor por defecto requerido por Spring Boot para la inyección de dependencias.
     */
    public JwtService() {
    }

    /**
     * Constructor con parámetros para facilitar pruebas unitarias independientes sin levantar el contexto de Spring.
     *
     * @param secretKey     Clave secreta utilizada para firmar y verificar tokens HMAC-SHA256.
     * @param jwtExpiration Tiempo de expiración del token en milisegundos.
     */
    public JwtService(String secretKey, long jwtExpiration) {
        this.secretKey = secretKey;
        this.jwtExpiration = jwtExpiration;
    }

    /**
     * Genera un token JWT a partir de la entidad de dominio {@link Usuario}.
     *
     * @param user Entidad del usuario para el cual se emite el token.
     * @return Cadena que representa el token JWT compacto y firmado.
     * @throws IllegalArgumentException si el usuario o su correo electrónico son nulos.
     */
    public String generateToken(Usuario user) {
        if (user == null || user.getEmail() == null) {
            throw new IllegalArgumentException("El usuario y su correo electrónico no pueden ser nulos");
        }
        String roleName = user.getRole() != null ? user.getRole().name() : "CITIZEN";
        return generateToken(user.getEmail(), roleName);
    }

    /**
     * Sobrecarga para generar un token JWT a partir de una dirección de correo y un rol textual.
     *
     * @param email Dirección de correo electrónico del usuario (claim {@code sub}).
     * @param role  Nombre del rol del usuario a incluir en el claim personalizado {@code role}.
     * @return Cadena que representa el token JWT compacto y firmado.
     * @throws IllegalArgumentException si el correo electrónico es nulo o está en blanco.
     */
    public String generateToken(String email, String role) {
        Map<String, Object> extraClaims = new HashMap<>();
        if (role != null) {
            extraClaims.put("role", role);
        }
        return generateToken(extraClaims, email);
    }

    /**
     * Genera un token JWT firmado incluyendo un conjunto de claims adicionales y el sujeto indicado.
     *
     * @param extraClaims Mapa de claims adicionales para incrustar en el payload del token.
     * @param email       Correo electrónico del usuario como sujeto principal del token.
     * @return Cadena que representa el token JWT compacto y firmado con HMAC-SHA256.
     * @throws IllegalArgumentException si el correo electrónico es nulo o está en blanco.
     */
    public String generateToken(Map<String, Object> extraClaims, String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El correo electrónico del sujeto no puede ser nulo ni estar vacío");
        }
        long currentTimeMillis = System.currentTimeMillis();
        Date issuedAt = new Date(currentTimeMillis);
        Date expiration = new Date(currentTimeMillis + jwtExpiration);

        return Jwts.builder()
                .claims(extraClaims != null ? extraClaims : new HashMap<>())
                .subject(email)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extrae el correo electrónico (sujeto {@code sub}) alojado en el token JWT.
     *
     * @param token Cadena con el token JWT a inspeccionar.
     * @return Correo electrónico del usuario.
     * @throws JwtException si el token es inválido, manipulado o su firma no coincide.
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrae el rol funcional asignado dentro del claim {@code role} del token JWT.
     *
     * @param token Cadena con el token JWT a inspeccionar.
     * @return Nombre del rol del usuario, o {@code null} si el claim no está presente.
     * @throws JwtException si el token es inválido, manipulado o su firma no coincide.
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    /**
     * Extrae la fecha y hora de expiración registrada en el token JWT.
     *
     * @param token Cadena con el token JWT a inspeccionar.
     * @return Instancia de {@link Date} con el instante de expiración.
     * @throws JwtException si el token es inválido, manipulado o su firma no coincide.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extrae un claim genérico aplicando una función transformadora sobre los {@link Claims} del token.
     *
     * @param <T>            Tipo de dato del claim esperado.
     * @param token          Cadena con el token JWT.
     * @param claimsResolver Función resolutora que extrae el atributo deseado a partir de los claims.
     * @return Valor del claim resuelto.
     * @throws JwtException si el token es inválido, manipulado o ha expirado.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parsea y valida el token JWT suministrado, devolviendo el conjunto completo de claims.
     *
     * @param token Cadena con el token JWT.
     * @return Objeto {@link Claims} con los claims del payload del token.
     * @throws JwtException si el token es inválido, manipulado, expirado o malformado.
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Comprueba si el token JWT ha superado su tiempo de vida útil.
     *
     * @param token Cadena con el token JWT a verificar.
     * @return {@code true} si el token ha expirado o no es válido; {@code false} si aún se encuentra vigente.
     */
    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return true;
        }
    }

    /**
     * Valida de manera integral si un token JWT es válido, su firma es auténtica, no ha expirado
     * y su sujeto coincide con la dirección de correo suministrada.
     *
     * @param token Cadena con el token JWT.
     * @param email Correo electrónico contra el que se verifica el sujeto del token.
     * @return {@code true} si el token es íntegro, vigente y pertenece al email; {@code false} en caso contrario.
     */
    public boolean isTokenValid(String token, String email) {
        try {
            final String extractedEmail = extractEmail(token);
            return (extractedEmail != null && extractedEmail.equals(email) && !isTokenExpired(token));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Obtiene el tiempo de expiración configurado para los tokens en milisegundos.
     *
     * @return Duración de vida del token en milisegundos.
     */
    public long getJwtExpiration() {
        return jwtExpiration;
    }

    /**
     * Obtiene la clave criptográfica {@link SecretKey} para firma y verificación HMAC-SHA256.
     * <p>
     * Se intenta decodificar la clave desde Base64. Si la clave no es Base64 válida o su longitud
     * resultante es inferior a 256 bits (32 bytes), se utilizan directamente sus bytes en codificación UTF-8.
     * </p>
     *
     * @return Instancia de {@link SecretKey} válida para HMAC-SHA256.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secretKey);
            if (keyBytes.length < 32) {
                keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
