package org.bulbaseros.security;

import io.jsonwebtoken.JwtException;
import org.bulbaseros.model.User;
import org.bulbaseros.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias para el servicio de gestión de tokens JWT {@link JwtService}.
 * <p>
 * Valida la correcta generación, extracción de claims (sujeto, rol, expiración),
 * verificación de validez y detección de tokens expirados o firmas manipuladas.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
class JwtServiceTest {

    private static final String SECRET_KEY =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B59703273357638792F423F4528482B4D6251655468576D5A7134743777217A25432A";
    private static final String DIFFERENT_SECRET_KEY =
            "999E635266556A586E3272357538782F413F4428472B4B6250645367566B59703273357638792F423F4528482B4D6251655468576D5A7134743777217A25432B";
    private static final long EXPIRATION_24H_MS = 86400000L;

    private JwtService jwtService;

    /**
     * Inicializa una instancia de {@link JwtService} antes de cada prueba.
     */
    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET_KEY, EXPIRATION_24H_MS);
    }

    /**
     * Verifica que se genere un token válido a partir de una entidad {@link User}
     * y que los claims de correo y rol sean extraídos con exactitud.
     */
    @Test
    @DisplayName("Debería generar un token desde una entidad User y extraer sus claims")
    void shouldGenerateTokenFromUserAndExtractClaims() {
        User user = new User("ciudadano@urbanpulse.es", "hashBcrypt", "Carlos Ruiz", UserRole.CITIZEN);

        String token = jwtService.generateToken(user);

        assertNotNull(token, "El token generado no debe ser nulo");
        assertFalse(token.isBlank(), "El token generado no debe estar vacío");

        String email = jwtService.extractEmail(token);
        String role = jwtService.extractRole(token);

        assertEquals("ciudadano@urbanpulse.es", email, "El email extraído debe coincidir con el del usuario");
        assertEquals("CITIZEN", role, "El rol extraído debe coincidir con el rol del usuario");
    }

    /**
     * Verifica que el payload del token JWT contenga explícitamente los claims 'sub' (subject) y 'role'.
     */
    @Test
    @DisplayName("Debería incluir y verificar los claims 'sub' y 'role' explícitos en el payload JWT")
    void shouldIncludeExplicitSubAndRoleClaims() {
        User user = new User("verificacion.claims@urbanpulse.es", "hash123", "Pedro Almodóvar", UserRole.CITIZEN);
        String token = jwtService.generateToken(user);

        io.jsonwebtoken.Claims claims = jwtService.extractAllClaims(token);
        assertNotNull(claims);
        assertEquals("verificacion.claims@urbanpulse.es", claims.getSubject(), "El claim 'sub' debe coincidir con el correo");
        assertEquals("CITIZEN", claims.get("role", String.class), "El claim 'role' debe coincidir con CITIZEN");
    }

    /**
     * Verifica que la sobrecarga con email y rol genere un token con los datos indicados.
     */
    @Test
    @DisplayName("Debería generar un token a partir de email y rol explícitos")
    void shouldGenerateTokenFromEmailAndRole() {
        String email = "operador@urbanpulse.es";
        String role = "OPERATOR";

        String token = jwtService.generateToken(email, role);

        assertNotNull(token);
        assertEquals(email, jwtService.extractEmail(token));
        assertEquals(role, jwtService.extractRole(token));
    }

    /**
     * Verifica la correcta validación de un token para el usuario propietario.
     */
    @Test
    @DisplayName("Debería validar correctamente un token legítimo para su usuario")
    void shouldValidateLegitimateToken() {
        String email = "test@urbanpulse.es";
        String token = jwtService.generateToken(email, "CITIZEN");

        assertTrue(jwtService.isTokenValid(token, email), "El token debe ser válido para el email con el que se creó");
        assertFalse(jwtService.isTokenValid(token, "otro@urbanpulse.es"), "El token no debe ser válido para un email distinto");
    }

    /**
     * Verifica que la fecha de expiración se calcule a 24 horas y el token no expire de inmediato.
     */
    @Test
    @DisplayName("Debería calcular la fecha de expiración aproximadamente a 24 horas")
    void shouldCalculateExpirationAround24Hours() {
        String token = jwtService.generateToken("user@urbanpulse.es", "CITIZEN");

        Date expiration = jwtService.extractExpiration(token);
        assertNotNull(expiration);

        long now = System.currentTimeMillis();
        long diff = expiration.getTime() - now;

        // Comprobamos que la diferencia esté en el rango esperado de 24 horas (~86.400.000 ms +/- 5 segundos)
        assertTrue(diff > (EXPIRATION_24H_MS - 5000), "La expiración debe ser mayor a 24h menos margen de ejecución");
        assertTrue(diff <= EXPIRATION_24H_MS, "La expiración no debe superar el tiempo máximo configurado");
        assertFalse(jwtService.isTokenExpired(token), "Un token recién generado no debe figurar como expirado");
    }

    /**
     * Verifica que tokens configurados con tiempo expirado sean detectados como caducados.
     */
    @Test
    @DisplayName("Debería detectar tokens expirados como caducados e inválidos")
    void shouldDetectExpiredTokens() {
        // Servicio con expiración negativa (-1 segundo)
        JwtService expiredJwtService = new JwtService(SECRET_KEY, -1000L);
        String expiredToken = expiredJwtService.generateToken("expired@urbanpulse.es", "CITIZEN");

        assertTrue(jwtService.isTokenExpired(expiredToken), "El token expirado debe detectarse como expirado");
        assertFalse(jwtService.isTokenValid(expiredToken, "expired@urbanpulse.es"), "El token expirado no debe ser válido");
    }

    /**
     * Verifica que un token firmado con una clave secreta diferente sea rechazado por discrepancia de firma.
     */
    @Test
    @DisplayName("Debería rechazar un token con firma manipulada o clave distinta")
    void shouldRejectTokenWithInvalidSignature() {
        JwtService foreignService = new JwtService(DIFFERENT_SECRET_KEY, EXPIRATION_24H_MS);
        String foreignToken = foreignService.generateToken("infiltrado@urbanpulse.es", "CITIZEN");

        assertFalse(jwtService.isTokenValid(foreignToken, "infiltrado@urbanpulse.es"),
                "El servicio debe invalidar tokens firmados con otra clave");

        assertThrows(JwtException.class, () -> jwtService.extractAllClaims(foreignToken),
                "Extraer claims de un token con firma ajena debe lanzar JwtException");
    }

    /**
     * Verifica que cadenas malformadas o corruptas no sean aceptadas como tokens válidos.
     */
    @Test
    @DisplayName("Debería rechazar tokens malformados")
    void shouldRejectMalformedTokens() {
        String malformedToken = "esto.no.es.un.token.jwt";

        assertFalse(jwtService.isTokenValid(malformedToken, "test@urbanpulse.es"));
        assertTrue(jwtService.isTokenExpired(malformedToken));
    }

    /**
     * Verifica que se lancen excepciones apropiadas ante argumentos nulos o vacíos.
     */
    @Test
    @DisplayName("Debería lanzar IllegalArgumentException si los argumentos son nulos o vacíos")
    void shouldThrowExceptionForInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> jwtService.generateToken((User) null));
        assertThrows(IllegalArgumentException.class, () -> jwtService.generateToken((String) null, "CITIZEN"));
        assertThrows(IllegalArgumentException.class, () -> jwtService.generateToken("   ", "CITIZEN"));
    }
}
