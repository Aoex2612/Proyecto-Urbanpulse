package org.bulbaseros.mappers;

import org.bulbaseros.dto.v2.AttachmentResponseV2;
import org.bulbaseros.entity.ArchivoAdjunto;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapeador de componentes Spring para transformar {@link ArchivoAdjunto} a {@link AttachmentResponseV2} en API v2.
 *
 * @author UrbanPulse Team
 * @version 2.0
 */
@Component
public class AttachmentMapperV2 {

    public AttachmentResponseV2 toDto(ArchivoAdjunto entity) {
        if (entity == null) {
            return null;
        }

        String id = entity.getPublicId() != null ? entity.getPublicId().toString() : null;
        String incidentId = (entity.getIncident() != null && entity.getIncident().getPublicId() != null)
                ? entity.getIncident().getPublicId().toString() : null;

        return AttachmentResponseV2.builder()
                .id(id)
                .incidentId(incidentId)
                .fileName(entity.getFileName())
                .fileUrl(entity.getFileUrl())
                .mimeType(entity.getMimeType())
                .fileSizeBytes(entity.getFileSizeBytes())
                .uploadedAt(entity.getUploadedAt())
                .build();
    }

    public List<AttachmentResponseV2> toDtoList(List<ArchivoAdjunto> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }
}
