package org.bulbaseros.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Objeto de transferencia de datos (DTO) que representa la información de un activo urbano
 * asociado o sugerido para una incidencia en la plataforma UrbanPulse.
 * <p>
 * Incluye los atributos del activo catalogado en el inventario municipal junto con las métricas
 * espaciales derivadas (distancia geodésica en metros), el índice probabilístico de confianza
 * y el estado de confirmación manual por parte del operador técnico.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentAssetResponse {

    /**
     * Identificador único del activo urbano en la base de datos municipal.
     */
    private Long assetId;

    /**
     * Código unívoco de inventario municipal del activo (ej. "SEM-MAL-00142").
     */
    private String assetCode;

    /**
     * Tipología o categoría del activo (ej. "SEMAFORO", "FUENTE", "CONTENEDOR", "ALUMBRADO").
     */
    private String type;

    /**
     * Nombre descriptivo o denominación pública del elemento urbano.
     */
    private String name;

    /**
     * Coordenada geográfica de latitud del activo en grados decimales (WGS84).
     */
    private Double latitude;

    /**
     * Coordenada geográfica de longitud del activo en grados decimales (WGS84).
     */
    private Double longitude;

    /**
     * Distancia euclidiana/geodésica calculada entre las coordenadas de la incidencia y el activo, en metros.
     */
    private Double distanceMeters;

    /**
     * Nivel de confianza o certidumbre probabilística normalizado entre 0.0 y 1.0 (o porcentaje).
     */
    private Double confidenceScore;

    /**
     * Indicador de si la vinculación ha sido confirmada formalmente por un operador o técnico de servicio.
     */
    private Boolean isConfirmed;
}
