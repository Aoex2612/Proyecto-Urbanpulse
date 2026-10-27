package org.bulbaseros.mappers;

import org.bulbaseros.dto.AttachmentResponse;
import org.bulbaseros.entity.ArchivoAdjunto;
import org.springframework.stereotype.Component;

/**
 * Mapeador de componentes Spring para la conversión entre entidades de dominio {@link ArchivoAdjunto}
 * y objetos de transferencia de datos {@link AttachmentResponse}.
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Component
public class AttachmentMapper extends BaseMapper<ArchivoAdjunto, AttachmentResponse> {

    /**
     * Convierte una entidad {@link ArchivoAdjunto} en su correspondiente DTO {@link AttachmentResponse}.
     *
     * @param entity Entidad de archivo adjunto de origen, o {@code null}.
     * @return DTO {@link AttachmentResponse} mapeado, o {@code null} si la entidad es nula.
     */
    @Override
    public AttachmentResponse toDto(ArchivoAdjunto entity) {
        if (entity == null) {
            return null;
        }

        Long incidentId = (entity.getIncident() != null) ? entity.getIncident().getId() : null;

        return AttachmentResponse.builder()
                .id(entity.getId())
                .incidentId(incidentId)
                .fileName(entity.getFileName())
                .fileUrl(entity.getFileUrl())
                .mimeType(entity.getMimeType())
                .fileSizeBytes(entity.getFileSizeBytes())
                .uploadedAt(entity.getUploadedAt())
                .build();
    }
}
