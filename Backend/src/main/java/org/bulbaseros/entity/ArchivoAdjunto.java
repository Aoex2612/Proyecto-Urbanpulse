package org.bulbaseros.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Entidad de dominio JPA que representa un archivo multimedia adjunto a una incidencia urbana.
 * <p>
 * Mapea la tabla {@code attachments} en la base de datos, almacenando la referencia
 * a la incidencia asociada, el nombre del archivo, su URL estática de recuperación,
 * el tipo MIME, su tamaño en bytes y la fecha de carga.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "attachments")
public class ArchivoAdjunto {

    /**
     * Identificador único secuencial del archivo adjunto (clave primaria).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Incidencia urbana a la que pertenece este archivo adjunto.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incidencia incident;

    /**
     * Nombre original del archivo proporcionado durante la subida.
     */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /**
     * Ruta relativa o URL pública estática para acceder al recurso.
     */
    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    /**
     * Tipo MIME del archivo multimedia (e.g. image/jpeg, image/png).
     */
    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    /**
     * Tamaño físico del archivo almacenado, en bytes.
     */
    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    /**
     * Marca temporal en que se registró y almacenó el archivo adjunto.
     */
    @Column(name = "uploaded_at")
    private OffsetDateTime uploadedAt;

    /**
     * Asigna la fecha y hora de carga previa persistencia si no se ha especificado.
     */
    @PrePersist
    public void prePersist() {
        if (this.uploadedAt == null) {
            this.uploadedAt = OffsetDateTime.now();
        }
    }
}
