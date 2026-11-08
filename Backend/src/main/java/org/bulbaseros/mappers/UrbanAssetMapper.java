package org.bulbaseros.mappers;

import org.bulbaseros.dto.IncidentAssetResponse;
import org.bulbaseros.entity.ActivoUrbano;
import org.bulbaseros.entity.AsociacionIncidenciaActivo;
import org.springframework.stereotype.Component;

/**
 * Mapeador de componentes Spring para la conversión entre entidades de persistencia
 * relacionadas con activos urbanos ({@link AsociacionIncidenciaActivo} y {@link ActivoUrbano})
 * y el objeto de transferencia de datos {@link IncidentAssetResponse}.
 * <p>
 * Garantiza el aislamiento total de la capa de dominio relacional frente a las capas de presentación REST.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Component
public class UrbanAssetMapper extends BaseMapper<AsociacionIncidenciaActivo, IncidentAssetResponse> {

    /**
     * Convierte una entidad de relación {@link AsociacionIncidenciaActivo} en su correspondiente DTO {@link IncidentAssetResponse}.
     * Extrae los datos descriptivos del activo vinculado y los atributos de la relación espacial (distancia, confianza y confirmación).
     *
     * @param entity Entidad de asociación entre incidencia y activo urbano, o {@code null}.
     * @return DTO {@link IncidentAssetResponse} mapeado, o {@code null} si la entidad es nula.
     */
    @Override
    public IncidentAssetResponse toDto(AsociacionIncidenciaActivo entity) {
        if (entity == null) {
            return null;
        }

        ActivoUrbano asset = entity.getAsset();
        Long assetId = null;
        String assetCode = null;
        String type = null;
        String name = null;
        Double latitude = null;
        Double longitude = null;

        if (asset != null) {
            assetId = asset.getId();
            assetCode = asset.getAssetCode();
            type = asset.getType();
            name = asset.getName();
            latitude = asset.getLatitude();
            longitude = asset.getLongitude();
        } else if (entity.getId() != null) {
            assetId = entity.getId().getAssetId();
        }

        return IncidentAssetResponse.builder()
                .assetId(assetId)
                .assetCode(assetCode)
                .type(type)
                .name(name)
                .latitude(latitude)
                .longitude(longitude)
                .distanceMeters(entity.getDistanceMeters())
                .confidenceScore(entity.getConfidenceScore())
                .isConfirmed(entity.getIsConfirmed())
                .build();
    }

    /**
     * Convierte una entidad directa {@link ActivoUrbano} en un {@link IncidentAssetResponse} incorporando
     * métricas calculadas en tiempo de ejecución (distancia geodésica, nivel de confianza y flag de confirmación).
     *
     * @param asset           Entidad de activo urbano de origen, o {@code null}.
     * @param distanceMeters  Distancia calculada en metros entre la incidencia y el activo.
     * @param confidenceScore Nivel de confianza o certidumbre probabilística (0.0 a 1.0).
     * @param isConfirmed     Indicador booleano de si la asociación ha sido confirmada por un operador.
     * @return DTO {@link IncidentAssetResponse} mapeado, o {@code null} si el activo es nulo.
     */
    public IncidentAssetResponse toDto(ActivoUrbano asset, Double distanceMeters, Double confidenceScore, Boolean isConfirmed) {
        if (asset == null) {
            return null;
        }

        return IncidentAssetResponse.builder()
                .assetId(asset.getId())
                .assetCode(asset.getAssetCode())
                .type(asset.getType())
                .name(asset.getName())
                .latitude(asset.getLatitude())
                .longitude(asset.getLongitude())
                .distanceMeters(distanceMeters)
                .confidenceScore(confidenceScore)
                .isConfirmed(isConfirmed)
                .build();
    }
}
