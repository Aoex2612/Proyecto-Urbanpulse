package org.bulbaseros.dto;

/**
 * Enumeración que define los niveles de riesgo para la seguridad ciudadana
 * utilizados en el cálculo de priorización asistida de incidencias operativas.
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
public enum SafetyRiskLevel {

    /**
     * Sin riesgo apreciable para la integridad física de peatones o vehículos.
     */
    NONE,

    /**
     * Riesgo moderado o potencial para la seguridad vial o ciudadana.
     */
    LOW,

    /**
     * Riesgo inminente o severo para la integridad física de personas o bienes públicos.
     */
    HIGH
}
