package org.bulbaseros.repository;

import org.bulbaseros.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la gestión y persistencia de entidades {@link User}.
 * <p>
 * Proporciona operaciones CRUD estándar heredadas de {@link JpaRepository} así como consultas
 * derivadas para verificar existencia y buscar usuarios por dirección de correo electrónico.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Comprueba si ya existe un usuario registrado en el sistema con el correo electrónico especificado.
     *
     * @param email Correo electrónico a verificar.
     * @return {@code true} si existe un registro con dicho correo, {@code false} en caso contrario.
     */
    boolean existsByEmail(String email);

    /**
     * Busca un usuario en el sistema a partir de su dirección de correo electrónico.
     *
     * @param email Correo electrónico del usuario a buscar.
     * @return Un {@link Optional} que contiene el {@link User} correspondiente si fue hallado,
     *         o un contenedor vacío en caso de no existir.
     */
    Optional<User> findByEmail(String email);
}
