package org.bulbaseros.service;

import org.bulbaseros.dto.PriorityCalculationRequest;
import org.bulbaseros.dto.PriorityCalculationResponse;
import org.bulbaseros.dto.SafetyRiskLevel;
import org.bulbaseros.entity.PrioridadIncidencia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para {@link PriorityService}.
 * <p>
 * Verifica de forma exhaustiva la matriz determinista de priorización asistida (RF-1),
 * la consistencia de las justificaciones explicativas y el manejo riguroso de excepciones ante entradas inválidas.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
class PriorityServiceTest {

    private PriorityService priorityService;

    @BeforeEach
    void setUp() {
        priorityService = new PriorityService();
    }

    @Test
    @DisplayName("Corte de vía = true Y Seguridad = HIGH debe resultar en prioridad CRITICAL con justificación adecuada")
    void shouldReturnCriticalWhenStreetCutTrueAndSafetyRiskHigh() {
        PriorityCalculationRequest request = PriorityCalculationRequest.builder()
                .streetCut(true)
                .safetyRisk(SafetyRiskLevel.HIGH)
                .build();

        PriorityCalculationResponse response = priorityService.calculateAssistedPriority(request);

        assertNotNull(response);
        assertEquals(PrioridadIncidencia.CRITICAL, response.getSuggestedPriority());
        assertNotNull(response.getPriorityReason());
        assertTrue(response.getPriorityReason().contains("CRÍTICA"));
        assertTrue(response.getPriorityReason().toLowerCase().contains("corte de vía"));
    }

    @Test
    @DisplayName("Corte de vía = true Y Seguridad = LOW debe resultar en prioridad HIGH")
    void shouldReturnHighWhenStreetCutTrueAndSafetyRiskLow() {
        PriorityCalculationRequest request = PriorityCalculationRequest.builder()
                .streetCut(true)
                .safetyRisk(SafetyRiskLevel.LOW)
                .build();

        PriorityCalculationResponse response = priorityService.calculateAssistedPriority(request);

        assertNotNull(response);
        assertEquals(PrioridadIncidencia.HIGH, response.getSuggestedPriority());
        assertNotNull(response.getPriorityReason());
        assertTrue(response.getPriorityReason().contains("ALTA"));
    }

    @Test
    @DisplayName("Corte de vía = true Y Seguridad = NONE debe resultar en prioridad HIGH")
    void shouldReturnHighWhenStreetCutTrueAndSafetyRiskNone() {
        PriorityCalculationRequest request = PriorityCalculationRequest.builder()
                .streetCut(true)
                .safetyRisk(SafetyRiskLevel.NONE)
                .build();

        PriorityCalculationResponse response = priorityService.calculateAssistedPriority(request);

        assertNotNull(response);
        assertEquals(PrioridadIncidencia.HIGH, response.getSuggestedPriority());
        assertNotNull(response.getPriorityReason());
        assertTrue(response.getPriorityReason().contains("ALTA"));
    }

    @Test
    @DisplayName("Corte de vía = false Y Seguridad = HIGH debe resultar en prioridad HIGH")
    void shouldReturnHighWhenStreetCutFalseAndSafetyRiskHigh() {
        PriorityCalculationRequest request = PriorityCalculationRequest.builder()
                .streetCut(false)
                .safetyRisk(SafetyRiskLevel.HIGH)
                .build();

        PriorityCalculationResponse response = priorityService.calculateAssistedPriority(request);

        assertNotNull(response);
        assertEquals(PrioridadIncidencia.HIGH, response.getSuggestedPriority());
        assertNotNull(response.getPriorityReason());
        assertTrue(response.getPriorityReason().contains("ALTA"));
        assertTrue(response.getPriorityReason().toLowerCase().contains("riesgo alto"));
    }

    @Test
    @DisplayName("Corte de vía = false Y Seguridad = LOW debe resultar en prioridad MEDIUM")
    void shouldReturnMediumWhenStreetCutFalseAndSafetyRiskLow() {
        PriorityCalculationRequest request = PriorityCalculationRequest.builder()
                .streetCut(false)
                .safetyRisk(SafetyRiskLevel.LOW)
                .build();

        PriorityCalculationResponse response = priorityService.calculateAssistedPriority(request);

        assertNotNull(response);
        assertEquals(PrioridadIncidencia.MEDIUM, response.getSuggestedPriority());
        assertNotNull(response.getPriorityReason());
        assertTrue(response.getPriorityReason().contains("MEDIA"));
    }

    @Test
    @DisplayName("Corte de vía = false Y Seguridad = NONE debe resultar en prioridad LOW")
    void shouldReturnLowWhenStreetCutFalseAndSafetyRiskNone() {
        PriorityCalculationRequest request = PriorityCalculationRequest.builder()
                .streetCut(false)
                .safetyRisk(SafetyRiskLevel.NONE)
                .build();

        PriorityCalculationResponse response = priorityService.calculateAssistedPriority(request);

        assertNotNull(response);
        assertEquals(PrioridadIncidencia.LOW, response.getSuggestedPriority());
        assertNotNull(response.getPriorityReason());
        assertTrue(response.getPriorityReason().contains("BAJA"));
    }

    @Test
    @DisplayName("calculateAssistedPriority debe lanzar IllegalArgumentException si la solicitud es nula")
    void shouldThrowExceptionWhenRequestIsNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> priorityService.calculateAssistedPriority(null));
        assertTrue(ex.getMessage().contains("no puede ser nula"));
    }

    @Test
    @DisplayName("calculateAssistedPriority debe lanzar IllegalArgumentException si streetCut es nulo")
    void shouldThrowExceptionWhenStreetCutIsNull() {
        PriorityCalculationRequest request = PriorityCalculationRequest.builder()
                .streetCut(null)
                .safetyRisk(SafetyRiskLevel.HIGH)
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> priorityService.calculateAssistedPriority(request));
        assertTrue(ex.getMessage().contains("corte de vía es obligatorio"));
    }

    @Test
    @DisplayName("calculateAssistedPriority debe lanzar IllegalArgumentException si safetyRisk es nulo")
    void shouldThrowExceptionWhenSafetyRiskIsNull() {
        PriorityCalculationRequest request = PriorityCalculationRequest.builder()
                .streetCut(false)
                .safetyRisk(null)
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> priorityService.calculateAssistedPriority(request));
        assertTrue(ex.getMessage().contains("riesgo de seguridad es obligatorio"));
    }
}
