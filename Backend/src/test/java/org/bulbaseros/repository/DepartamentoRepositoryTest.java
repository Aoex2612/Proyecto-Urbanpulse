package org.bulbaseros.repository;

import org.bulbaseros.entity.Departamento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de integración y persistencia para el repositorio {@link DepartamentoRepository}.
 * <p>
 * Verifica la recuperación ordenada alfabéticamente de los departamentos municipales mediante {@link DataJpaTest}.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@DataJpaTest
class DepartamentoRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DepartamentoRepository departamentoRepository;

    @Test
    @DisplayName("Debería recuperar todos los departamentos ordenados alfabéticamente por nombre")
    void shouldFindAllOrderedByNameAsc() {
        Departamento d3 = new Departamento("Vías Públicas", "Calzadas y aceras");
        entityManager.persistAndFlush(d3);

        Departamento d1 = new Departamento("Alumbrado", "Iluminación de calles");
        entityManager.persistAndFlush(d1);

        Departamento d2 = new Departamento("Limpieza", "Recogida de residuos");
        entityManager.persistAndFlush(d2);

        List<Departamento> results = departamentoRepository.findAllByOrderByNameAsc();

        assertTrue(results.size() >= 3, "Debe contener al menos los 3 departamentos persistidos");
        // Verificar que los 3 departamentos específicos aparecen ordenados alfabéticamente
        List<String> names = results.stream()
                .map(Departamento::getName)
                .filter(name -> name.equals("Alumbrado") || name.equals("Limpieza") || name.equals("Vías Públicas"))
                .toList();

        assertEquals(List.of("Alumbrado", "Limpieza", "Vías Públicas"), names,
                "Los nombres deben aparecer estrictamente ordenados de la A a la Z");
    }
}
