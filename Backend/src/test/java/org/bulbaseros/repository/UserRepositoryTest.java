package org.bulbaseros.repository;

import org.bulbaseros.entity.Departamento;
import org.bulbaseros.model.User;
import org.bulbaseros.model.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de integración y persistencia para el repositorio {@link UserRepository}.
 * <p>
 * Emplea {@link DataJpaTest} para validar operaciones contra la base de datos en memoria H2,
 * verificando inserción, consulta por correo y comprobación de existencia.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    /**
     * Verifica que un usuario se persista correctamente y pueda ser recuperado por email.
     */
    @Test
    @DisplayName("Debería persistir un usuario y recuperarlo mediante findByEmail")
    void shouldPersistAndFindByEmail() {
        User user = new User("ciudadano.test@urbanpulse.es", "$2a$10$xyzHashedPassword", "Carlos López");
        entityManager.persistAndFlush(user);

        Optional<User> found = userRepository.findByEmail("ciudadano.test@urbanpulse.es");

        assertTrue(found.isPresent(), "El usuario debería encontrarse en el repositorio");
        assertEquals("ciudadano.test@urbanpulse.es", found.get().getEmail());
        assertEquals("Carlos López", found.get().getFullName());
        assertEquals(UserRole.CITIZEN, found.get().getRole());
        assertNotNull(found.get().getId(), "El identificador generado no debe ser nulo");
        assertNotNull(found.get().getCreatedAt(), "La fecha de creación no debe ser nula");
    }

    /**
     * Verifica que findByEmail retorne Optional vacío si el correo no existe.
     */
    @Test
    @DisplayName("Debería retornar Optional vacío si el correo no existe en la base de datos")
    void shouldReturnEmptyOptionalWhenEmailNotFound() {
        Optional<User> found = userRepository.findByEmail("inexistente@urbanpulse.es");

        assertFalse(found.isPresent(), "No debería encontrarse ningún usuario");
    }

    /**
     * Verifica que existsByEmail devuelva true cuando el correo existe.
     */
    @Test
    @DisplayName("Debería devolver true cuando el correo ya existe en existsByEmail")
    void shouldReturnTrueWhenEmailExists() {
        User user = new User("existente@urbanpulse.es", "$2a$10$hashed123", "Laura Martínez");
        entityManager.persistAndFlush(user);

        boolean exists = userRepository.existsByEmail("existente@urbanpulse.es");

        assertTrue(exists, "existsByEmail debería devolver true");
    }

    /**
     * Verifica que existsByEmail devuelva false cuando el correo no está registrado.
     */
    @Test
    @DisplayName("Debería devolver false cuando el correo no está registrado en existsByEmail")
    void shouldReturnFalseWhenEmailDoesNotExist() {
        boolean exists = userRepository.existsByEmail("no_registrado@urbanpulse.es");

        assertFalse(exists, "existsByEmail debería devolver false");
    }

    /**
     * Verifica que se persistan usuarios con distintos roles y departamento opcional.
     */
    @Test
    @DisplayName("Debería persistir usuario con rol TECHNICIAN y departamento asignado")
    void shouldPersistUserWithTechnicianRoleAndDepartment() {
        Departamento dept = new Departamento();
        dept.setName("Redes y Telecomunicaciones");
        dept = entityManager.persistAndFlush(dept);

        User technician = new User("tecnico.red@urbanpulse.es", "$2a$10$hashTecnico", "Marta V.", UserRole.TECHNICIAN, dept.getId());
        entityManager.persistAndFlush(technician);

        Optional<User> found = userRepository.findByEmail("tecnico.red@urbanpulse.es");

        assertTrue(found.isPresent());
        assertEquals(UserRole.TECHNICIAN, found.get().getRole());
        assertEquals(dept.getId(), found.get().getDepartmentId());
    }
}
