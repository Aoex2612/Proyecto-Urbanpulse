package org.bulbaseros.repository;

import org.bulbaseros.entity.Departamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para la persistencia y consulta de departamentos municipales ({@link Departamento}).
 * <p>
 * Proporciona métodos de acceso a datos para listar las áreas de especialidad técnica operativas en el municipio.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Repository
public interface DepartamentoRepository extends JpaRepository<Departamento, Long> {

    /**
     * Recupera la totalidad de departamentos municipales registrados, ordenados alfabéticamente por su nombre.
     *
     * @return Lista ordenada alfabéticamente de departamentos municipales.
     */
    List<Departamento> findAllByOrderByNameAsc();

    /**
     * Recupera un departamento a partir de su identificador publico universal (UUID v4).
     *
     * @param publicId Identificador publico del departamento.
     * @return {@link Optional} con el departamento encontrado o vacio.
     */
    Optional<Departamento> findByPublicId(UUID publicId);
}
