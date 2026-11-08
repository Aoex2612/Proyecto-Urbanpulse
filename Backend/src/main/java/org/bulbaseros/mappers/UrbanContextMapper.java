package org.bulbaseros.mappers;

import org.bulbaseros.dto.ExternalObservationResponse;
import org.bulbaseros.entity.ObservacionesExternas;
import org.springframework.stereotype.Component;

/**
 * Mapeador de componentes Spring para la conversión entre la entidad de persistencia
 * {@link ObservacionesExternas} y el DTO {@link ExternalObservationResponse}.
 * <p>
 * Aísla las lecturas y métricas ambientales o de tráfico almacenadas en base de datos
 * para su serialización segura y desacoplada hacia los clientes API.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Component
public class UrbanContextMapper extends BaseMapper<ObservacionesExternas, ExternalObservationResponse> {

    /**
     * Convierte una entidad {@link ObservacionesExternas} en su correspondiente DTO {@link ExternalObservationResponse}.
     *
     * @param entity Entidad de observación externa de origen, o {@code null}.
     * @return DTO {@link ExternalObservationResponse} poblado, o {@code null} si la entidad es nula.
     */
    @Override
    public ExternalObservationResponse toDto(ObservacionesExternas entity) {
        if (entity == null) {
            return null;
        }

        return ExternalObservationResponse.builder()
                .id(entity.getId())
                .source(entity.getSource())
                .metricType(entity.getMetricType())
                .valueNum(entity.getValueNum())
                .valueText(entity.getValueText())
                .observedAt(entity.getObservedAt())
                .build();
    }
}
