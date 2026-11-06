package org.bulbaseros.service;

import org.bulbaseros.dto.PriorityCalculationRequest;
import org.bulbaseros.dto.PriorityCalculationResponse;
import org.bulbaseros.dto.SafetyRiskLevel;
import org.bulbaseros.entity.PrioridadIncidencia;
import org.springframework.stereotype.Service;

/**
 * Servicio de negocio responsable del motor de reglas determinista para la priorización asistida
 * de incidencias urbanas en el proceso de triaje operativo.
 * <p>
 * Implementa una función pura basada en el cruce del indicador de corte de vía pública
 * y el nivel discreto de riesgo de seguridad ciudadana (RF-1), devolviendo la prioridad sugerida
 * y una justificación textual descriptiva en español.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Service
public class PriorityService {

    /**
     * Calcula la prioridad asistida recomendada para una incidencia a partir de los factores de riesgo suministrados.
     * <p>
     * Aplica la matriz de decisión determinista:
     * <ul>
     *   <li>Corte de vía = true Y Seguridad = HIGH &rarr; CRITICAL</li>
     *   <li>Corte de vía = true Y Seguridad &isin; {LOW, NONE} &rarr; HIGH</li>
     *   <li>Corte de vía = false Y Seguridad = HIGH &rarr; HIGH</li>
     *   <li>Corte de vía = false Y Seguridad = LOW &rarr; MEDIUM</li>
     *   <li>Corte de vía = false Y Seguridad = NONE &rarr; LOW</li>
     * </ul>
     * </p>
     *
     * @param request Datos de entrada con el indicador de corte de calle y nivel de riesgo de seguridad.
     * @return {@link PriorityCalculationResponse} con la prioridad recomendada y la justificación explicativa.
     * @throws IllegalArgumentException si la solicitud o alguno de sus parámetros obligatorios es nulo.
     */
    public PriorityCalculationResponse calculateAssistedPriority(PriorityCalculationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud de cálculo de prioridad no puede ser nula");
        }
        if (request.getStreetCut() == null) {
            throw new IllegalArgumentException("El indicador de corte de vía es obligatorio");
        }
        if (request.getSafetyRisk() == null) {
            throw new IllegalArgumentException("El nivel de riesgo de seguridad es obligatorio");
        }

        boolean streetCut = request.getStreetCut();
        SafetyRiskLevel safetyRisk = request.getSafetyRisk();

        PrioridadIncidencia priority;
        String reason;

        if (streetCut && safetyRisk == SafetyRiskLevel.HIGH) {
            priority = PrioridadIncidencia.CRITICAL;
            reason = "Prioridad CRÍTICA sugerida debido al corte de vía pública combinado con un nivel de riesgo alto para la seguridad ciudadana.";
        } else if (streetCut) { // streetCut == true && (safetyRisk == LOW || safetyRisk == NONE)
            priority = PrioridadIncidencia.HIGH;
            if (safetyRisk == SafetyRiskLevel.LOW) {
                reason = "Prioridad ALTA sugerida debido al corte de vía pública con afectación a la circulación y nivel de riesgo bajo para la seguridad.";
            } else {
                reason = "Prioridad ALTA sugerida debido al corte de vía pública con afectación a la circulación sin riesgo adicional de seguridad.";
            }
        } else if (safetyRisk == SafetyRiskLevel.HIGH) { // streetCut == false && safetyRisk == HIGH
            priority = PrioridadIncidencia.HIGH;
            reason = "Prioridad ALTA sugerida por implicar un riesgo alto para la seguridad ciudadana sin requerir corte de vía.";
        } else if (safetyRisk == SafetyRiskLevel.LOW) { // streetCut == false && safetyRisk == LOW
            priority = PrioridadIncidencia.MEDIUM;
            reason = "Prioridad MEDIA sugerida al presentar un nivel de riesgo bajo para la seguridad sin corte de vía pública.";
        } else { // streetCut == false && safetyRisk == NONE
            priority = PrioridadIncidencia.LOW;
            reason = "Prioridad BAJA sugerida sin afectación apreciable a la seguridad ciudadana ni corte de vía pública.";
        }

        return PriorityCalculationResponse.builder()
                .suggestedPriority(priority)
                .priorityReason(reason)
                .build();
    }
}
