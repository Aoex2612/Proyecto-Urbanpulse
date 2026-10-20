package org.bulbaseros.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias para la entidad de dominio {@link User}.
 * <p>
 * Verifica los constructores, valores por defecto (rol {@link UserRole#CITIZEN}),
 * validaciones defensivas de longitud, comportamiento de igualdad y exclusión
 * de datos sensibles en el método {@code toString()}.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
class UserTest {

    /**
     * Verifica que el constructor por defecto asigne el rol CITIZEN y deje campos vacíos.
     */
    @Test
    @DisplayName("Debería inicializar con rol CITIZEN por defecto en constructor sin argumentos")
    void shouldInitializeWithCitizenRoleByDefault() {
        User user = new User();

        assertEquals(UserRole.CITIZEN, user.getRole(), "El rol por defecto debe ser CITIZEN");
        assertNull(user.getId());
        assertNull(user.getEmail());
        assertNull(user.getPasswordHash());
        assertNull(user.getFullName());
        assertNull(user.getDepartmentId());
        assertNull(user.getCreatedAt());
    }

    /**
     * Verifica la instanciación mediante constructor básico para registro de ciudadano.
     */
    @Test
    @DisplayName("Debería crear usuario con constructor básico asignando rol CITIZEN")
    void shouldCreateUserWithBasicConstructor() {
        User user = new User("ciudadano@urbanpulse.es", "$2a$10$hashEjemplo", "Juan Pérez");

        assertEquals("ciudadano@urbanpulse.es", user.getEmail());
        assertEquals("$2a$10$hashEjemplo", user.getPasswordHash());
        assertEquals("Juan Pérez", user.getFullName());
        assertEquals(UserRole.CITIZEN, user.getRole());
        assertNull(user.getDepartmentId());
    }

    /**
     * Verifica la instanciación con rol específico y con departamento.
     */
    @Test
    @DisplayName("Debería crear usuario con rol específico y departamento")
    void shouldCreateUserWithRoleAndDepartment() {
        User user = new User("tecnico@urbanpulse.es", "$2a$10$hash", "Ana Gómez", UserRole.TECHNICIAN, 5L);

        assertEquals("tecnico@urbanpulse.es", user.getEmail());
        assertEquals("$2a$10$hash", user.getPasswordHash());
        assertEquals("Ana Gómez", user.getFullName());
        assertEquals(UserRole.TECHNICIAN, user.getRole());
        assertEquals(5L, user.getDepartmentId());
    }

    /**
     * Verifica que si se pasa rol nulo al constructor o setter, se asigne CITIZEN por defecto.
     */
    @Test
    @DisplayName("Debería asignar rol CITIZEN si se provee null como rol")
    void shouldFallbackToCitizenWhenRoleIsNull() {
        User user = new User("test@urbanpulse.es", "$2a$10$hash", "Carlos", null);
        assertEquals(UserRole.CITIZEN, user.getRole());

        user.setRole(null);
        assertEquals(UserRole.CITIZEN, user.getRole());
    }

    /**
     * Verifica getters y setters de todos los atributos.
     */
    @Test
    @DisplayName("Debería permitir modificar y obtener todos los atributos mediante getters y setters")
    void shouldGetAndSetAllProperties() {
        User user = new User();
        OffsetDateTime now = OffsetDateTime.now();

        user.setId(10L);
        user.setEmail("operador@urbanpulse.es");
        user.setPasswordHash("$2a$10$hashedPass");
        user.setFullName("María Ruiz");
        user.setRole(UserRole.OPERATOR);
        user.setDepartmentId(2L);
        user.setCreatedAt(now);

        assertEquals(10L, user.getId());
        assertEquals("operador@urbanpulse.es", user.getEmail());
        assertEquals("$2a$10$hashedPass", user.getPasswordHash());
        assertEquals("María Ruiz", user.getFullName());
        assertEquals(UserRole.OPERATOR, user.getRole());
        assertEquals(2L, user.getDepartmentId());
        assertEquals(now, user.getCreatedAt());
    }

    /**
     * Verifica que se lance IllegalArgumentException si el correo supera 150 caracteres.
     */
    @Test
    @DisplayName("Debería rechazar correos que excedan 150 caracteres")
    void shouldRejectEmailExceeding150Characters() {
        User user = new User();
        String longEmail = "a".repeat(140) + "@urbanpulse.es"; // 154 caracteres

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> user.setEmail(longEmail)
        );
        assertTrue(exception.getMessage().contains("150 caracteres"));
    }

    /**
     * Verifica que se lance IllegalArgumentException si el nombre completo supera 150 caracteres.
     */
    @Test
    @DisplayName("Debería rechazar nombres completos que excedan 150 caracteres")
    void shouldRejectFullNameExceeding150Characters() {
        User user = new User();
        String longName = "A".repeat(151);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> user.setFullName(longName)
        );
        assertTrue(exception.getMessage().contains("150 caracteres"));
    }

    /**
     * Verifica que se lance IllegalArgumentException si el hash supera 255 caracteres.
     */
    @Test
    @DisplayName("Debería rechazar hashes de contraseña que excedan 255 caracteres")
    void shouldRejectPasswordHashExceeding255Characters() {
        User user = new User();
        String longHash = "h".repeat(256);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> user.setPasswordHash(longHash)
        );
        assertTrue(exception.getMessage().contains("255 caracteres"));
    }

    /**
     * Verifica que el método toString() no exponga el hash de la contraseña por seguridad.
     */
    @Test
    @DisplayName("Debería excluir el hash de contraseña en el método toString")
    void shouldNotIncludePasswordHashInToString() {
        String secretHash = "$2a$10$SuperSecretHashValue";
        User user = new User("seguridad@urbanpulse.es", secretHash, "Seguridad");

        String toStringResult = user.toString();

        assertNotNull(toStringResult);
        assertFalse(toStringResult.contains(secretHash), "El toString jamás debe exponer la contraseña hasheada");
        assertTrue(toStringResult.contains("seguridad@urbanpulse.es"));
        assertTrue(toStringResult.contains("Seguridad"));
    }

    /**
     * Verifica la lógica de igualdad y código hash basada en id o email.
     */
    @Test
    @DisplayName("Debería evaluar igualdad por id o por email")
    void shouldEvaluateEqualityCorrectly() {
        User user1 = new User("user1@urbanpulse.es", "hash1", "User One");
        User user2 = new User("user1@urbanpulse.es", "hash2", "User Dos");

        // Sin id persistido, compara por email
        assertEquals(user1, user2);
        assertEquals(user1.hashCode(), user2.hashCode());

        user1.setId(1L);
        user2.setId(1L);
        assertEquals(user1, user2);

        user2.setId(2L);
        assertNotEquals(user1, user2);
    }
}
