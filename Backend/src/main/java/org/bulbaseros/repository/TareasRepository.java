package org.bulbaseros.repository;

import org.bulbaseros.entity.Tareas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Repositorio Spring Data JPA para la gestión de persistencia de asignaciones departamentales y técnicas ({@link Tareas}).
 * <p>
 * Facilita operaciones CRUD y consultas derivadas para determinar la vigencia operativa de asignaciones,
 * verificar la intervención activa de técnicos específicos y consultar el historial de asignaciones de cada incidencia.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Repository
public interface TareasRepository extends JpaRepository<Tareas, Long> {

    /**
     * Recupera todas las asignaciones que se encuentran activas para una incidencia dada.
     *
     * @param incidentId Identificador único de la incidencia.
     * @return Lista de asignaciones activas ({@code is_active = true}).
     */
    List<Tareas> findByIncidentIdAndIsActiveTrue(Long incidentId);

    /**
     * Comprueba si un técnico específico cuenta con una asignación activa para una incidencia determinada.
     *
     * @param incidentId   Identificador único de la incidencia.
     * @param technicianId Identificador único del usuario técnico.
     * @return {@code true} si existe al menos una asignación activa para dicho técnico en la incidencia; {@code false} en caso contrario.
     */
    boolean existsByIncidentIdAndTechnicianIdAndIsActiveTrue(Long incidentId, Long technicianId);

    /**
     * Recupera el histórico íntegro de asignaciones asociadas a una incidencia, tanto activas como inactivadas lógicamente.
     *
     * @param incidentId Identificador único de la incidencia.
     * @return Lista total de asignaciones vinculadas a la incidencia.
     */
    List<Tareas> findByIncidentId(Long incidentId);

    /**
     * Recupera las asignaciones activas de un departamento específico para una incidencia dada.
     *
     * @param incidentId   Identificador único de la incidencia.
     * @param departmentId Identificador único del departamento municipal.
     * @return Lista de asignaciones activas pertenecientes al departamento especificado.
     */
    List<Tareas> findByIncidentIdAndDepartmentIdAndIsActiveTrue(Long incidentId, Long departmentId);

    /**
     * Recupera todas las asignaciones que se encuentran activas para un técnico dado (RF-25).
     *
     * @param technicianId Identificador único del usuario técnico.
     * @return Lista de asignaciones activas ({@code is_active = true}) asociadas al técnico.
     */
    List<Tareas> findByTechnicianIdAndIsActiveTrue(Long technicianId);

    /**
     * Recupera todas las asignaciones vinculadas a una coleccion de identificadores de incidencias.
     *
     * @param incidentIds Coleccion de identificadores unicos de incidencias.
     * @return Lista de asignaciones pertenecientes a las incidencias indicadas.
     */
    List<Tareas> findByIncidentIdIn(Collection<Long> incidentIds);

    /**
     * Recupera todas las asignaciones que se encuentran activas vinculadas a una coleccion de incidencias.
     *
     * @param incidentIds Coleccion de identificadores unicos de incidencias.
     * @return Lista de asignaciones activas vinculadas a dichas incidencias.
     */
    List<Tareas> findByIncidentIdInAndIsActiveTrue(Collection<Long> incidentIds);

    /**
     * Recupera todas las asignaciones pertenecientes a un departamento municipal concreto.
     *
     * @param departmentId Identificador unico del departamento.
     * @return Lista total de asignaciones del departamento.
     */
    List<Tareas> findByDepartmentId(Long departmentId);

    /**
     * Recupera todas las asignaciones activas pertenecientes a un departamento municipal especifico.
     *
     * @param departmentId Identificador unico del departamento.
     * @return Lista de asignaciones activas del departamento.
     */
    List<Tareas> findByDepartmentIdAndIsActiveTrue(Long departmentId);
}
