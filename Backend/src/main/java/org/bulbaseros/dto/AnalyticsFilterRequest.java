package org.bulbaseros.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;

/**
 * Objeto de transferencia de datos (DTO) de solicitud para encapsular los parámetros de filtrado
 * temporal y territorial en las consultas de analítica urbana.
 * <p>
 * Admite filtros predefinidos mediante {@link AnalyticsTimePreset}, rangos de fechas personalizados
 * con marcas temporales ISO-8601, selección de un año histórico específico y delimitación geográfica
 * opcional por distrito o barrio municipal de Málaga.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsFilterRequest {

    /**
     * Rango temporal preconfigurado seleccionado por el usuario.
     */
    private AnalyticsTimePreset preset;

    /**
     * Fecha y hora inicial del rango temporal personalizado (opcional).
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime startDate;

    /**
     * Fecha y hora final del rango temporal personalizado (opcional).
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime endDate;

    /**
     * Denominación del distrito municipal para delimitación territorial (opcional, máximo 100 caracteres).
     */
    @Size(max = 100, message = "El nombre del distrito no puede superar los 100 caracteres")
    private String district;

    /**
     * Denominación del barrio municipal para delimitación territorial (opcional, máximo 100 caracteres).
     */
    @Size(max = 100, message = "El nombre del barrio no puede superar los 100 caracteres")
    private String neighborhood;

    /**
     * Año natural específico para el filtro {@link AnalyticsTimePreset#SPECIFIC_YEAR} (opcional, rango 2000-2100).
     */
    @Min(value = 2000, message = "El año debe ser igual o posterior al año 2000")
    @Max(value = 2100, message = "El año no puede exceder el año 2100")
    private Integer year;
}
