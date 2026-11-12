package org.bulbaseros.mappers;

import org.bulbaseros.dto.v2.IncidentAssetResponseV2;
import org.bulbaseros.entity.ActivoUrbano;
import org.bulbaseros.entity.AsociacionIncidenciaActivo;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapeador de componentes Spring para transformar {@link AsociacionIncidenciaActivo} y {@link ActivoUrbano}
 * a {@link IncidentAssetResponseV2} en API v2.
 *
 * @author UrbanPulse Team
 * @version 2.0
 */
@Component
public class UrbanAssetMapperV2 {

    public IncidentAssetResponseV2 toDto(AsociacionIncidenciaActivo entity) {
        if (entity == null) {
            return null;
        }

        ActivoUrbano asset = entity.getAsset();
        String assetId = (asset != null && asset.getPublicId() != null) ? asset.getPublicId().toString() : null;
        String assetCode = asset != null ? asset.getAssetCode() : null;
        String type = asset != null ? asset.getType() : null;
        String name = asset != null ? asset.getName() : null;
        Double latitude = asset != null ? asset.getLatitude() : null;
        Double longitude = asset != null ? asset.getLongitude() : null;

        return IncidentAssetResponseV2.builder()
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

    public List<IncidentAssetResponseV2> toDtoList(List<AsociacionIncidenciaActivo> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }
}
