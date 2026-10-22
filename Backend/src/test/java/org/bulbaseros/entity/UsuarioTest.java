package org.bulbaseros.entity;

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
 * Pruebas unitarias para la entidad de dominio {@link Usuario}.
 * <p>
 * Verifica los constructores, valores por defecto (rol {@link RolUsuario#CITIZEN}),
 * validaciones defensivas de longitud, comportamiento de igualdad y exclusión
 * de datos sensibles en el método {@code toString()}.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
class UsuarioTest {

    /**
     * Verifica que el constructor por defecto asigne el rol CITIZEN y deje campos vacíos.
     */
    @Test
    @DisplayName("Debería inicializar con rol CITIZEN por defecto en constructor sin argumentos")
    void shouldInitializeWithCitizenRoleByDefault() {
        Usuario usuario = new Usuario();

        assertEquals(RolUsuario.CITIZEN, usuario.getRole(), "El rol por defecto debe ser CITIZEN");
        assertNull(usuario.getId());
        assertNull(usuario.getEmail());
        assertNull(usuario.getPasswordHash());
        assertNull(usuario.getFullName());
        assertNull(usuario.getDepartment());
        assertNull(usuario.getCreatedAt());
    }

    /**
     * Verifica la instanciación mediante constructor básico para registro de ciudadano.
     */
    @Test
    @DisplayName("Debería crear usuario con constructor básico asignando rol CITIZEN")
    void shouldCreateUserWithBasicConstructor() {
        Usuario usuario = new Usuario("ciudadano@urbanpulse.es", "$2a$10$hashEjemplo", "Juan Pérez");

        assertEquals("ciudadano@urbanpulse.es", usuario.getEmail());
        assertEquals("$2a$10$hashEjemplo", usuario.getPasswordHash());
        assertEquals("Juan Pérez", usuario.getFullName());
        assertEquals(RolUsuario.CITIZEN, usuario.getRole());
        assertNull(usuario.getDepartment());
    }

    /**
     * Verifica la instanciación con rol específico y con departamento.
     */
    @Test
    @DisplayName("Debería crear usuario con rol específico y departamento")
    void shouldCreateUserWithRoleAndDepartment() {
        Departamento depto = new Departamento();
        depto.setId(5L);
        depto.setName("Movilidad");

        Usuario usuario = new Usuario("tecnico@urbanpulse.es", "$2a$10$hash", "Ana Gómez", RolUsuario.TECHNICIAN, depto);

        assertEquals("tecnico@urbanpulse.es", usuario.getEmail());
        assertEquals("$2a$10$hash", usuario.getPasswordHash());
        assertEquals("Ana Gómez", usuario.getFullName());
        assertEquals(RolUsuario.TECHNICIAN, usuario.getRole());
        assertEquals(depto, usuario.getDepartment());
    }

    /**
     * Verifica que si se pasa rol nulo al constructor o setter, se asigne CITIZEN por defecto.
     */
    @Test
    @DisplayName("Debería asignar rol CITIZEN si se provee null como rol")
    void shouldFallbackToCitizenWhenRoleIsNull() {
        Usuario usuario = new Usuario("test@urbanpulse.es", "$2a$10$hash", "Carlos", null);
        assertEquals(RolUsuario.CITIZEN, usuario.getRole());

        usuario.setRole(null);
        assertEquals(RolUsuario.CITIZEN, usuario.getRole());
    }

    /**
     * Verifica getters y setters de todos los atributos.
     */
    @Test
    @DisplayName("Debería permitir modificar y obtener todos los atributos mediante getters y setters")
    void shouldGetAndSetAllProperties() {
        Usuario usuario = new Usuario();
        Departamento depto = new Departamento();
        depto.setId(2L);
        OffsetDateTime now = OffsetDateTime.now();

        usuario.setId(10L);
        usuario.setEmail("operador@urbanpulse.es");
        usuario.setPasswordHash("$2a$10$hashedPass");
        usuario.setFullName("María Ruiz");
        usuario.setRole(RolUsuario.OPERATOR);
        usuario.setDepartment(depto);
        usuario.setCreatedAt(now);

        assertEquals(10L, usuario.getId());
        assertEquals("operador@urbanpulse.es", usuario.getEmail());
        assertEquals("$2a$10$hashedPass", usuario.getPasswordHash());
        assertEquals("María Ruiz", usuario.getFullName());
        assertEquals(RolUsuario.OPERATOR, usuario.getRole());
        assertEquals(depto, usuario.getDepartment());
        assertEquals(now, usuario.getCreatedAt());
    }

    /**
     * Verifica que se lance IllegalArgumentException si el correo supera 150 caracteres.
     */
    @Test
    @DisplayName("Debería rechazar correos que excedan 150 caracteres")
    void shouldRejectEmailExceeding150Characters() {
        Usuario usuario = new Usuario();
        String longEmail = "a".repeat(140) + "@urbanpulse.es"; // 154 caracteres

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> usuario.setEmail(longEmail)
        );
        assertTrue(exception.getMessage().contains("150 caracteres"));
    }

    /**
     * Verifica que se lance IllegalArgumentException si el nombre completo supera 150 caracteres.
     */
    @Test
    @DisplayName("Debería rechazar nombres completos que excedan 150 caracteres")
    void shouldRejectFullNameExceeding150Characters() {
        Usuario usuario = new Usuario();
        String longName = "A".repeat(151);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> usuario.setFullName(longName)
        );
        assertTrue(exception.getMessage().contains("150 caracteres"));
    }

    /**
     * Verifica que se lance IllegalArgumentException si el hash supera 255 caracteres.
     */
    @Test
    @DisplayName("Debería rechazar hashes de contraseña que excedan 255 caracteres")
    void shouldRejectPasswordHashExceeding255Characters() {
        Usuario usuario = new Usuario();
        String longHash = "h".repeat(256);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> usuario.setPasswordHash(longHash)
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
        Usuario usuario = new Usuario("seguridad@urbanpulse.es", secretHash, "Seguridad");

        String toStringResult = usuario.toString();

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
        Usuario usuario1 = new Usuario("user1@urbanpulse.es", "hash1", "User One");
        Usuario usuario2 = new Usuario("user1@urbanpulse.es", "hash2", "User Dos");

        // Sin id persistido, compara por email
        assertEquals(usuario1, usuario2);
        assertEquals(usuario1.hashCode(), usuario2.hashCode());

        usuario1.setId(1L);
        usuario2.setId(1L);
        assertEquals(usuario1, usuario2);

        usuario2.setId(2L);
        assertNotEquals(usuario1, usuario2);
    }
}
