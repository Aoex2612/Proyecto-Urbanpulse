package org.bulbaseros.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Objeto de transferencia de datos (DTO) que representa una observación contextual externa
 * vinculada a una incidencia en la plataforma UrbanPulse.
 * <p>
 * Proporciona información ambiental o de tráfico capturada en tiempo real desde servicios
 * e APIs externas (Open-Meteo, sensores municipales de tráfico, etc.) en el momento del reporte.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalObservationResponse {

    /**
     * Identificador único de la observación externa.
     */
    private Long id;

    /**
     * Fuente u origen del dato contextual externo (ej. "OPEN_METEO", "DGT_TRAFFIC", "MUNICIPAL_SENSORS").
     */
    private String source;

    /**
     * Tipo de métrica meteorológica o de tráfico (ej. "TEMPERATURE_C", "PRECIPITATION_MM", "WIND_SPEED_KMH", "TRAFFIC_LEVEL").
     */
    private String metricType;

    /**
     * Valor numérico cuantitativo registrado para la métrica, si aplica.
     */
    private Double valueNum;

    /**
     * Valor textual o categórico descriptivo registrado para la métrica, si aplica (ej. "HEAVY_TRAFFIC", "RAINY").
     */
    private String valueText;

    /**
     * Marca temporal con desplazamiento horario en que se produjo o capturó la medición.
     */
    private OffsetDateTime observedAt;
}
