package org.bulbaseros.repository;

import org.bulbaseros.entity.RolUsuario;
import org.bulbaseros.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la gestión y persistencia de entidades {@link Usuario}.
 * <p>
 * Proporciona operaciones CRUD estándar heredadas de {@link JpaRepository} así como consultas
 * derivadas para verificar existencia, buscar usuarios por dirección de correo electrónico y
 * filtrar técnicos asociados a un departamento municipal específico.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Repository
public interface UserRepository extends JpaRepository<Usuario, Long> {

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
     * @return Un {@link Optional} que contiene el {@link Usuario} correspondiente si fue hallado,
     *         o un contenedor vacío en caso de no existir.
     */
    Optional<Usuario> findByEmail(String email);

    /**
     * Recupera la lista de usuarios que poseen un rol funcional determinado y pertenecen a un departamento específico.
     * <p>
     * Utilizado primordialmente en el flujo de triaje operativo para listar los técnicos adscritos a un departamento municipal.
     * </p>
     *
     * @param role         Rol funcional del usuario (típicamente {@link RolUsuario#TECHNICIAN}).
     * @param departmentId Identificador único del departamento municipal.
     * @return Lista de usuarios que cumplen con el rol y la adscripción departamental indicada.
     */
    @Query("SELECT u FROM Usuario u WHERE u.role = :role AND u.department.id = :departmentId")
    List<Usuario> findByRoleAndDepartmentId(@Param("role") RolUsuario role, @Param("departmentId") Long departmentId);

    /**
     * Recupera la lista de usuarios que poseen un rol funcional determinado.
     *
     * @param role Rol funcional del usuario.
     * @return Lista de usuarios con dicho rol.
     */
    List<Usuario> findByRole(RolUsuario role);
}
