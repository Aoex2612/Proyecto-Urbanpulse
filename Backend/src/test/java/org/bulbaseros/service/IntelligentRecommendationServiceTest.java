package org.bulbaseros.service;

import org.bulbaseros.dto.AssistantPerformanceResponse;
import org.bulbaseros.dto.DepartmentRecommendationResponse;
import org.bulbaseros.entity.Departamento;
import org.bulbaseros.entity.Incidencia;
import org.bulbaseros.entity.Tareas;
import org.bulbaseros.exception.IncidentNotFoundException;
import org.bulbaseros.repository.DepartamentoRepository;
import org.bulbaseros.repository.IncidenciaRepository;
import org.bulbaseros.repository.TareasRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link IntelligentRecommendationService} con Mockito y JUnit 5.
 * <p>
 * Verifica el comportamiento del motor heurístico léxico con grado de certeza probabilística (RF-14, RF-15, RF-16),
 * la activación del distintivo explícito ante baja confianza inferior al 50% (RF-17), y la evaluación del desempeño
 * y tasa histórica de adopción asistida de sugerencias departamentales (RF-18).
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class IntelligentRecommendationServiceTest {

    @Mock
    private DepartamentoRepository departamentoRepository;

    @Mock
    private TareasRepository tareasRepository;

    @Mock
    private IncidenciaRepository incidenciaRepository;

    @InjectMocks
    private IntelligentRecommendationService recommendationService;

    private Departamento deptVias;
    private Departamento deptAlumbrado;
    private Departamento deptLimpieza;
    private Departamento deptParques;
    private Departamento deptMovilidad;
    private List<Departamento> catalog;

    @BeforeEach
    void setUp() {
        deptVias = new Departamento(1L, "Vías Públicas", "Mantenimiento de calzadas, aceras y pavimentos");
        deptAlumbrado = new Departamento(2L, "Alumbrado", "Red eléctrica, luminarias y farolas públicas");
        deptLimpieza = new Departamento(3L, "Limpieza", "Recogida de basuras, contenedores y saneamiento urbano");
        deptParques = new Departamento(4L, "Parques y Jardines", "Cuidado de árboles, zonas verdes y parques infantiles");
        deptMovilidad = new Departamento(5L, "Movilidad", "Tráfico rodado, semáforos y señalización vial");

        catalog = List.of(deptAlumbrado, deptLimpieza, deptMovilidad, deptParques, deptVias);
    }

    @Nested
    @DisplayName("Pruebas de Recomendación Asistida de Alta Confianza (RF-15, RF-16)")
    class HighConfidenceRecommendationTests {

        @Test
        @DisplayName("Debería recomendar 'Vías Públicas' con alta confianza para bache en calzada")
        void recommendDepartment_ViasPublicas_HighConfidence() {
            when(departamentoRepository.findAllByOrderByNameAsc()).thenReturn(catalog);

            DepartmentRecommendationResponse response = recommendationService.recommendDepartment(
                    "Bache profundo en el asfalto de la calzada",
                    "Hay un socavón peligroso en mitad de la vía que daña las ruedas de los coches",
                    "Vías Públicas"
            );

            assertNotNull(response);
            assertEquals(1L, response.getSuggestedDepartmentId());
            assertEquals("Vías Públicas", response.getSuggestedDepartmentName());
            assertTrue(response.getConfidencePercentage() >= 80.0, "La confianza debe ser >= 80% para coincidencia unívoca");
            assertFalse(response.getIsLowConfidenceWarning(), "No debe marcar advertencia de baja confianza");
            assertNotNull(response.getReasoning());
            assertTrue(response.getReasoning().contains("Vías Públicas"));
        }

        @Test
        @DisplayName("Debería recomendar 'Alumbrado' con alta confianza para farola fundida")
        void recommendDepartment_Alumbrado_HighConfidence() {
            when(departamentoRepository.findAllByOrderByNameAsc()).thenReturn(catalog);

            DepartmentRecommendationResponse response = recommendationService.recommendDepartment(
                    "Farola fundida que no enciende en la calle",
                    "Lleva tres noches consecutivas apagada y la luminaria no emite luz",
                    "Alumbrado"
            );

            assertNotNull(response);
            assertEquals(2L, response.getSuggestedDepartmentId());
            assertEquals("Alumbrado", response.getSuggestedDepartmentName());
            assertTrue(response.getConfidencePercentage() >= 80.0);
            assertFalse(response.getIsLowConfidenceWarning());
            assertTrue(response.getReasoning().contains("Alumbrado"));
        }

        @Test
        @DisplayName("Debería recomendar 'Limpieza' con alta confianza para contenedor desbordado")
        void recommendDepartment_Limpieza_HighConfidence() {
            when(departamentoRepository.findAllByOrderByNameAsc()).thenReturn(catalog);

            DepartmentRecommendationResponse response = recommendationService.recommendDepartment(
                    "Contenedor de basura desbordado y suciedad en acera",
                    "Acumulación excesiva de residuos y desperdicios con mal olor en la papelera",
                    "Limpieza"
            );

            assertNotNull(response);
            assertEquals(3L, response.getSuggestedDepartmentId());
            assertEquals("Limpieza", response.getSuggestedDepartmentName());
            assertTrue(response.getConfidencePercentage() >= 80.0);
            assertFalse(response.getIsLowConfidenceWarning());
            assertTrue(response.getReasoning().contains("Limpieza"));
        }

        @Test
        @DisplayName("Debería recomendar 'Parques y Jardines' con alta confianza para árbol caído")
        void recommendDepartment_ParquesYJardines_HighConfidence() {
            when(departamentoRepository.findAllByOrderByNameAsc()).thenReturn(catalog);

            DepartmentRecommendationResponse response = recommendationService.recommendDepartment(
                    "Árbol caído y ramas rotas en zona infantil del parque",
                    "Un pino de gran tamaño se ha derrumbado sobre los columpios rompiendo las ramas",
                    "Parques y Jardines"
            );

            assertNotNull(response);
            assertEquals(4L, response.getSuggestedDepartmentId());
            assertEquals("Parques y Jardines", response.getSuggestedDepartmentName());
            assertTrue(response.getConfidencePercentage() >= 80.0);
            assertFalse(response.getIsLowConfidenceWarning());
            assertTrue(response.getReasoning().contains("Parques y Jardines"));
        }
    }

    @Nested
    @DisplayName("Pruebas de Advertencia de Baja Confianza y Casos Límite (RF-17)")
    class LowConfidenceAndEdgeCasesTests {

        @Test
        @DisplayName("Debería activar advertencia de baja confianza (< 50%) ante texto ambiguo con empate")
        void recommendDepartment_AmbiguousText_LowConfidence() {
            when(departamentoRepository.findAllByOrderByNameAsc()).thenReturn(catalog);

            // "farola" (Alumbrado x3) vs "bache" (Vías Públicas x3) en el mismo título
            DepartmentRecommendationResponse response = recommendationService.recommendDepartment(
                    "Farola rota y bache peligroso",
                    "Avería reportada sin más datos",
                    "Incidencia Urbana"
            );

            assertNotNull(response);
            assertTrue(response.getConfidencePercentage() < 50.0, "La confianza en caso ambiguo debe ser < 50%");
            assertTrue(response.getIsLowConfidenceWarning(), "Debe activarse la bandera de advertencia");
            assertTrue(response.getReasoning().toLowerCase().contains("ambigüedad"));
        }

        @Test
        @DisplayName("Debería activar advertencia de baja confianza ante texto extremadamente escueto (< 15 caracteres)")
        void recommendDepartment_ShortText_LowConfidence() {
            when(departamentoRepository.findAllByOrderByNameAsc()).thenReturn(catalog);

            DepartmentRecommendationResponse response = recommendationService.recommendDepartment(
                    "Roto",
                    null,
                    null
            );

            assertNotNull(response);
            assertTrue(response.getConfidencePercentage() < 50.0, "La confianza debe ser < 50% para texto escueto");
            assertTrue(response.getIsLowConfidenceWarning());
            assertTrue(response.getReasoning().toLowerCase().contains("escueto"));
        }

        @Test
        @DisplayName("Debería activar advertencia y confianza 0.0% cuando no existen coincidencias léxicas")
        void recommendDepartment_NoMatches_ZeroConfidence() {
            when(departamentoRepository.findAllByOrderByNameAsc()).thenReturn(catalog);

            DepartmentRecommendationResponse response = recommendationService.recommendDepartment(
                    "Problema en la calle",
                    "Algo está ocurriendo de manera inusual en este lugar",
                    "General"
            );

            assertNotNull(response);
            assertEquals(0.0, response.getConfidencePercentage());
            assertTrue(response.getConfidencePercentage() < 50.0);
            assertTrue(response.getIsLowConfidenceWarning());
            assertTrue(response.getReasoning().toLowerCase().contains("sin coincidencias"));
        }

        @Test
        @DisplayName("Debería gestionar de forma segura valores nulos en todos los campos")
        void recommendDepartment_NullFields_ZeroConfidence() {
            when(departamentoRepository.findAllByOrderByNameAsc()).thenReturn(catalog);

            DepartmentRecommendationResponse response = recommendationService.recommendDepartment(
                    null, null, null
            );

            assertNotNull(response);
            assertEquals(0.0, response.getConfidencePercentage());
            assertTrue(response.getIsLowConfidenceWarning());
        }

        @Test
        @DisplayName("Debería ser resiliente ante catálogo de departamentos vacío")
        void recommendDepartment_EmptyCatalog_NullSuggestedDept() {
            when(departamentoRepository.findAllByOrderByNameAsc()).thenReturn(Collections.emptyList());

            DepartmentRecommendationResponse response = recommendationService.recommendDepartment(
                    "Bache en asfalto", "Descripción", "Vías Públicas"
            );

            assertNotNull(response);
            assertNull(response.getSuggestedDepartmentId());
            assertNull(response.getSuggestedDepartmentName());
            assertEquals(0.0, response.getConfidencePercentage());
            assertTrue(response.getIsLowConfidenceWarning());
            assertTrue(response.getReasoning().contains("No se encontraron departamentos"));
        }
    }

    @Nested
    @DisplayName("Pruebas de Sobrecargas y Delegación de Entidad e Identificador")
    class OverloadedMethodsTests {

        @Test
        @DisplayName("recommendDepartment(Incidencia) debería extraer correctamente los campos")
        void recommendDepartment_IncidentEntity_Success() {
            when(departamentoRepository.findAllByOrderByNameAsc()).thenReturn(catalog);

            Incidencia incident = new Incidencia();
            incident.setId(100L);
            incident.setTitle("Bache profundo en el asfalto de la calzada");
            incident.setDescription("Pavimento hundido");
            incident.setCategory("Vías Públicas");

            DepartmentRecommendationResponse response = recommendationService.recommendDepartment(incident);

            assertNotNull(response);
            assertEquals(1L, response.getSuggestedDepartmentId());
            assertFalse(response.getIsLowConfidenceWarning());
        }

        @Test
        @DisplayName("recommendDepartment(Incidencia) con incidencia nula debería retornar respuesta vacía segura")
        void recommendDepartment_NullIncidentEntity_SafeResponse() {
            DepartmentRecommendationResponse response = recommendationService.recommendDepartment((Incidencia) null);

            assertNotNull(response);
            assertNull(response.getSuggestedDepartmentId());
            assertEquals(0.0, response.getConfidencePercentage());
            assertTrue(response.getIsLowConfidenceWarning());
        }

        @Test
        @DisplayName("recommendDepartment(Long) debería consultar IncidenciaRepository")
        void recommendDepartment_ById_Success() {
            when(departamentoRepository.findAllByOrderByNameAsc()).thenReturn(catalog);

            Incidencia incident = new Incidencia();
            incident.setId(10L);
            incident.setTitle("Farola fundida que no enciende en la calle");
            incident.setDescription("Oscuridad total");
            incident.setCategory("Alumbrado");

            when(incidenciaRepository.findById(10L)).thenReturn(Optional.of(incident));

            DepartmentRecommendationResponse response = recommendationService.recommendDepartment(10L);

            assertNotNull(response);
            assertEquals(2L, response.getSuggestedDepartmentId());
            assertEquals("Alumbrado", response.getSuggestedDepartmentName());
            verify(incidenciaRepository, times(1)).findById(10L);
        }

        @Test
        @DisplayName("recommendDepartment(Long) debería lanzar IncidentNotFoundException si no existe")
        void recommendDepartment_ById_NotFound_ThrowsException() {
            when(incidenciaRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(IncidentNotFoundException.class, () -> recommendationService.recommendDepartment(999L));
        }

        @Test
        @DisplayName("recommendDepartment(Long) debería lanzar IllegalArgumentException si id es nulo")
        void recommendDepartment_NullId_ThrowsException() {
            assertThrows(IllegalArgumentException.class, () -> recommendationService.recommendDepartment((Long) null));
        }

        @Test
        @DisplayName("recommendDepartment(Long, IncidenciaRepository) debería utilizar el repositorio personalizado")
        void recommendDepartment_CustomRepo_Success() {
            when(departamentoRepository.findAllByOrderByNameAsc()).thenReturn(catalog);

            Incidencia incident = new Incidencia();
            incident.setId(20L);
            incident.setTitle("Contenedor de basura lleno");
            incident.setDescription("Suciedad en la calle");
            incident.setCategory("Limpieza");

            IncidenciaRepository customRepo = mock(IncidenciaRepository.class);
            when(customRepo.findById(20L)).thenReturn(Optional.of(incident));

            DepartmentRecommendationResponse response = recommendationService.recommendDepartment(20L, customRepo);

            assertNotNull(response);
            assertEquals(3L, response.getSuggestedDepartmentId());
            verify(customRepo, times(1)).findById(20L);
            verify(incidenciaRepository, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("Pruebas de Evaluación de Desempeño y Tasa de Adopción (RF-18)")
    class AssistantPerformanceTests {

        @Test
        @DisplayName("evaluatePerformance debería computar tasa de adopción parcial (ej. 2 de 3 -> 66.7%)")
        void evaluatePerformance_PartialAdoption_Success() {
            when(departamentoRepository.findAllByOrderByNameAsc()).thenReturn(catalog);

            Incidencia inc1 = new Incidencia();
            inc1.setId(101L);
            inc1.setTitle("Bache profundo en el asfalto de la calzada");
            inc1.setDescription("Socavón en calzada");
            inc1.setCategory("Vías Públicas");

            Incidencia inc2 = new Incidencia();
            inc2.setId(102L);
            inc2.setTitle("Farola fundida que no enciende en la calle");
            inc2.setDescription("Luz rota en la calle");
            inc2.setCategory("Alumbrado");

            Incidencia inc3 = new Incidencia();
            inc3.setId(103L);
            inc3.setTitle("Contenedor de basura lleno y suciedad");
            inc3.setDescription("Residuos en la acera");
            inc3.setCategory("Limpieza");

            List<Incidencia> incidents = List.of(inc1, inc2, inc3);

            // Simulamos asignaciones en BD:
            // inc1 -> asignada a Vías Públicas (coincide con recomendación -> aceptada)
            Tareas tarea1 = new Tareas();
            tarea1.setIncident(inc1);
            tarea1.setDepartment(deptVias);

            // inc2 -> asignada a Alumbrado (coincide con recomendación -> aceptada)
            Tareas tarea2 = new Tareas();
            tarea2.setIncident(inc2);
            tarea2.setDepartment(deptAlumbrado);

            // inc3 -> asignada a Movilidad (NO coincide con recomendación de Limpieza -> no aceptada)
            Tareas tarea3 = new Tareas();
            tarea3.setIncident(inc3);
            tarea3.setDepartment(deptMovilidad);

            when(tareasRepository.findByIncidentIdIn(anyCollection())).thenReturn(List.of(tarea1, tarea2, tarea3));

            AssistantPerformanceResponse performance = recommendationService.evaluatePerformance(incidents);

            assertNotNull(performance);
            assertEquals(3L, performance.getTotalRecommendations());
            assertEquals(2L, performance.getAcceptedRecommendations());
            assertEquals(66.7, performance.getAdoptionRate(), 0.01, "La tasa debe ser 66.7% (2 de 3)");
            assertTrue(performance.getAverageConfidence() >= 80.0, "La confianza promedio de estas 3 debe ser alta");
        }

        @Test
        @DisplayName("evaluatePerformance debería prevenir división por cero ante colección vacía")
        void evaluatePerformance_EmptyList_ReturnsZeroMetrics() {
            AssistantPerformanceResponse performance = recommendationService.evaluatePerformance(Collections.emptyList());

            assertNotNull(performance);
            assertEquals(0L, performance.getTotalRecommendations());
            assertEquals(0L, performance.getAcceptedRecommendations());
            assertEquals(0.0, performance.getAdoptionRate());
            assertEquals(0.0, performance.getAverageConfidence());
            verify(tareasRepository, never()).findByIncidentIdIn(anyCollection());
        }

        @Test
        @DisplayName("evaluatePerformance debería retornar ceros ante parámetro nulo")
        void evaluatePerformance_NullList_ReturnsZeroMetrics() {
            AssistantPerformanceResponse performance = recommendationService.evaluatePerformance(null);

            assertNotNull(performance);
            assertEquals(0L, performance.getTotalRecommendations());
            assertEquals(0L, performance.getAcceptedRecommendations());
            assertEquals(0.0, performance.getAdoptionRate());
            assertEquals(0.0, performance.getAverageConfidence());
        }

        @Test
        @DisplayName("evaluatePerformance debería computar cero aceptadas si no existen asignaciones")
        void evaluatePerformance_NoAssignments_ZeroAccepted() {
            when(departamentoRepository.findAllByOrderByNameAsc()).thenReturn(catalog);

            Incidencia inc = new Incidencia();
            inc.setId(201L);
            inc.setTitle("Bache profundo en el asfalto");
            inc.setDescription("Calzada rota");
            inc.setCategory("Vías Públicas");

            when(tareasRepository.findByIncidentIdIn(anyCollection())).thenReturn(Collections.emptyList());

            AssistantPerformanceResponse performance = recommendationService.evaluatePerformance(List.of(inc));

            assertNotNull(performance);
            assertEquals(1L, performance.getTotalRecommendations());
            assertEquals(0L, performance.getAcceptedRecommendations());
            assertEquals(0.0, performance.getAdoptionRate());
            assertTrue(performance.getAverageConfidence() > 0.0);
        }
    }
}
