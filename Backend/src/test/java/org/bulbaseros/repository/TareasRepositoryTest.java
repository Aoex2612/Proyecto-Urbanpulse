package org.bulbaseros.repository;

import org.bulbaseros.entity.Departamento;
import org.bulbaseros.entity.Incidencia;
import org.bulbaseros.entity.RolUsuario;
import org.bulbaseros.entity.Tareas;
import org.bulbaseros.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración y persistencia para el repositorio {@link TareasRepository}.
 * <p>
 * Verifica mediante {@link DataJpaTest} las consultas de asignaciones activas,
 * filtrado por departamento, comprobación de técnicos asignados e inactividad lógica.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@DataJpaTest
class TareasRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TareasRepository tareasRepository;

    private Incidencia incident1;
    private Incidencia incident2;
    private Departamento deptVias;
    private Departamento deptAlumbrado;
    private Usuario technicianVias;
    private Usuario technicianAlumbrado;
    private Usuario operatorUser;

    @BeforeEach
    void setUp() {
        Usuario citizen = new Usuario("vecino.triaje@urbanpulse.es", "$2a$10$xyzHashedPass", "Vecino Triaje", RolUsuario.CITIZEN);
        citizen = entityManager.persistAndFlush(citizen);

        operatorUser = new Usuario("operador.triaje@urbanpulse.es", "$2a$10$xyzHashedPass", "Operador Triaje", RolUsuario.OPERATOR);
        operatorUser = entityManager.persistAndFlush(operatorUser);

        deptVias = new Departamento("Vías Públicas", "Reparación de calzadas y aceras");
        deptVias = entityManager.persistAndFlush(deptVias);

        deptAlumbrado = new Departamento("Alumbrado Público", "Mantenimiento de luminarias");
        deptAlumbrado = entityManager.persistAndFlush(deptAlumbrado);

        technicianVias = new Usuario("tech.vias@urbanpulse.es", "$2a$10$xyzHashedPass", "Juan Vías", RolUsuario.TECHNICIAN, deptVias);
        technicianVias = entityManager.persistAndFlush(technicianVias);

        technicianAlumbrado = new Usuario("tech.alumbrado@urbanpulse.es", "$2a$10$xyzHashedPass", "Pedro Luz", RolUsuario.TECHNICIAN, deptAlumbrado);
        technicianAlumbrado = entityManager.persistAndFlush(technicianAlumbrado);

        incident1 = new Incidencia("Socavón en calle principal", "Hoyo profundo en el asfalto", "Vías Públicas", 36.72, -4.42, citizen);
        incident1 = entityManager.persistAndFlush(incident1);

        incident2 = new Incidencia("Farola rota en parque", "Luminaria caída tras temporal", "Alumbrado", 36.73, -4.41, citizen);
        incident2 = entityManager.persistAndFlush(incident2);
    }

    @Test
    @DisplayName("Debería recuperar únicamente las asignaciones activas de una incidencia dada")
    void shouldFindOnlyActiveAssignmentsByIncidentId() {
        Tareas activeTask1 = new Tareas(incident1, deptVias, technicianVias, operatorUser);
        activeTask1.setIsActive(true);
        entityManager.persistAndFlush(activeTask1);

        Tareas inactiveTask = new Tareas(incident1, deptAlumbrado, null, operatorUser);
        inactiveTask.setIsActive(false);
        entityManager.persistAndFlush(inactiveTask);

        Tareas otherIncidentTask = new Tareas(incident2, deptAlumbrado, technicianAlumbrado, operatorUser);
        otherIncidentTask.setIsActive(true);
        entityManager.persistAndFlush(otherIncidentTask);

        List<Tareas> activeTasks = tareasRepository.findByIncidentIdAndIsActiveTrue(incident1.getId());

        assertEquals(1, activeTasks.size(), "Debe recuperar exactamente una asignación activa para incident1");
        Tareas result = activeTasks.get(0);
        assertTrue(result.getIsActive(), "La tarea devuelta debe estar activa");
        assertEquals(deptVias.getId(), result.getDepartment().getId(), "El departamento debe coincidir con Vías");
        assertEquals(technicianVias.getId(), result.getTechnician().getId(), "El técnico debe coincidir con Juan Vías");
    }

    @Test
    @DisplayName("Debería verificar correctamente si un técnico tiene asignación activa para una incidencia")
    void shouldCheckIfExistsByIncidentIdAndTechnicianIdAndIsActiveTrue() {
        Tareas activeTask = new Tareas(incident1, deptVias, technicianVias, operatorUser);
        activeTask.setIsActive(true);
        entityManager.persistAndFlush(activeTask);

        boolean existsActive = tareasRepository.existsByIncidentIdAndTechnicianIdAndIsActiveTrue(incident1.getId(), technicianVias.getId());
        assertTrue(existsActive, "Debe retornar true para un técnico asignado activamente");

        boolean existsOtherTech = tareasRepository.existsByIncidentIdAndTechnicianIdAndIsActiveTrue(incident1.getId(), technicianAlumbrado.getId());
        assertFalse(existsOtherTech, "Debe retornar false para un técnico sin asignación en la incidencia");

        boolean existsInOtherIncident = tareasRepository.existsByIncidentIdAndTechnicianIdAndIsActiveTrue(incident2.getId(), technicianVias.getId());
        assertFalse(existsInOtherIncident, "Debe retornar false para un técnico en otra incidencia no asignada");
    }

    @Test
    @DisplayName("existsByIncidentIdAndTechnicianIdAndIsActiveTrue debe retornar false si la asignación está inactiva lógicamente")
    void shouldReturnFalseWhenTechnicianAssignmentIsInactive() {
        Tareas inactiveTask = new Tareas(incident1, deptVias, technicianVias, operatorUser);
        inactiveTask.setIsActive(false);
        entityManager.persistAndFlush(inactiveTask);

        boolean exists = tareasRepository.existsByIncidentIdAndTechnicianIdAndIsActiveTrue(incident1.getId(), technicianVias.getId());
        assertFalse(exists, "Debe retornar false si la asignación del técnico fue desactivada lógicamente");
    }

    @Test
    @DisplayName("Debería retornar todo el historial de asignaciones activas e inactivas mediante findByIncidentId")
    void shouldReturnAllAssignmentsIncludingInactiveByIncidentId() {
        Tareas activeTask = new Tareas(incident1, deptVias, technicianVias, operatorUser);
        activeTask.setIsActive(true);
        entityManager.persistAndFlush(activeTask);

        Tareas inactiveTask = new Tareas(incident1, deptAlumbrado, technicianAlumbrado, operatorUser);
        inactiveTask.setIsActive(false);
        entityManager.persistAndFlush(inactiveTask);

        List<Tareas> allTasks = tareasRepository.findByIncidentId(incident1.getId());

        assertEquals(2, allTasks.size(), "Debe incluir tanto la asignación activa como la inactiva");
    }

    @Test
    @DisplayName("Debería filtrar asignaciones activas por incidencia y departamento específico")
    void shouldFilterActiveAssignmentsByIncidentAndDepartment() {
        Tareas taskVias = new Tareas(incident1, deptVias, technicianVias, operatorUser);
        taskVias.setIsActive(true);
        entityManager.persistAndFlush(taskVias);

        Tareas taskAlumbradoActive = new Tareas(incident1, deptAlumbrado, technicianAlumbrado, operatorUser);
        taskAlumbradoActive.setIsActive(true);
        entityManager.persistAndFlush(taskAlumbradoActive);

        Tareas taskAlumbradoInactive = new Tareas(incident1, deptAlumbrado, null, operatorUser);
        taskAlumbradoInactive.setIsActive(false);
        entityManager.persistAndFlush(taskAlumbradoInactive);

        List<Tareas> viasResults = tareasRepository.findByIncidentIdAndDepartmentIdAndIsActiveTrue(incident1.getId(), deptVias.getId());
        assertEquals(1, viasResults.size(), "Debe haber 1 asignación activa en Vías");
        assertEquals(deptVias.getId(), viasResults.get(0).getDepartment().getId());

        List<Tareas> alumbradoResults = tareasRepository.findByIncidentIdAndDepartmentIdAndIsActiveTrue(incident1.getId(), deptAlumbrado.getId());
        assertEquals(1, alumbradoResults.size(), "Debe haber 1 asignación activa en Alumbrado (omitiendo la inactiva)");
        assertEquals(technicianAlumbrado.getId(), alumbradoResults.get(0).getTechnician().getId());
    }
}
