package org.bulbaseros.repository;

import org.bulbaseros.entity.Departamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}
