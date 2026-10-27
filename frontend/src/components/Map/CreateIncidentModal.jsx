/**
 * @fileoverview Modal de creación asistida de incidencias para UrbanPulse.
 * Despliega un formulario modal con validación en frontend para capturar el título,
 * descripción, categoría y archivo multimedia adjunto (imagen JPEG/PNG opcional)
 * de la incidencia, mostrando las coordenadas geográficas seleccionadas sobre el mapa
 * en modo de solo lectura. Persiste la nueva incidencia a través de incidentService
 * y sube el archivo adjunto si fue provisto, notificando al componente padre para actualizar el mapa.
 */

import { useState, useEffect, useRef, useCallback } from 'react';
import incidentService from '../../services/incidentService';
import { INCIDENT_CATEGORIES } from './mapConstants';
import './Map.css';

/**
 * Componente modal para dar de alta una nueva incidencia geolocalizada con soporte para adjunto multimedia.
 *
 * @param {Object} props - Propiedades del componente.
 * @param {boolean} [props.isOpen=true] - Controla la visibilidad del modal.
 * @param {{latitude: number|string, longitude: number|string}|null} props.selectedLocation - Coordenadas capturadas desde el mapa.
 * @param {Function} [props.onClose] - Callback para cerrar el modal y descartar la selección.
 * @param {Function} [props.onCancel] - Alias de onClose para descarte de selección.
 * @param {Function} [props.onCreated] - Callback invocado tras la creación exitosa con la incidencia retornada por la API.
 * @param {Function} [props.onSubmit] - Función opcional para sobrescribir la invocación al servicio de creación.
 * @returns {import('react').JSX.Element|null} Elemento JSX del modal o null si no se debe renderizar.
 */
export default function CreateIncidentModal({
  isOpen = true,
  selectedLocation,
  onClose,
  onCancel,
  onCreated,
  onSubmit,
}) {
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    category: '',
  });

  const [selectedFile, setSelectedFile] = useState(null);
  const [previewUrl, setPreviewUrl] = useState(null);

  const [errors, setErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [serverError, setServerError] = useState(null);

  const titleInputRef = useRef(null);
  const fileInputRef = useRef(null);

  // Manejador unificado de cierre y cancelación memorizado
  const handleDismiss = useCallback(() => {
    if (isSubmitting) return;

    setFormData({
      title: '',
      description: '',
      category: '',
    });
    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
    }
    setSelectedFile(null);
    setPreviewUrl(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
    setErrors({});
    setServerError(null);

    if (onClose) {
      onClose();
    } else if (onCancel) {
      onCancel();
    }
  }, [isSubmitting, onClose, onCancel, previewUrl]);

  // Foco automático en el primer campo al abrirse el modal y escucha de tecla Escape
  useEffect(() => {
    if (isOpen && selectedLocation) {
      const timer = setTimeout(() => {
        titleInputRef.current?.focus();
      }, 50);

      /**
       * Listener de teclado para cerrar el modal con Escape.
       *
       * @param {KeyboardEvent} e - Evento de teclado.
       * @returns {void}
       */
      const handleKeyDown = (e) => {
        if (e.key === 'Escape') {
          handleDismiss();
        }
      };

      window.addEventListener('keydown', handleKeyDown);

      return () => {
        clearTimeout(timer);
        window.removeEventListener('keydown', handleKeyDown);
      };
    }
  }, [isOpen, selectedLocation, handleDismiss]);

  // Si no está abierto o no existen coordenadas válidas, no renderizar nada
  if (!isOpen || !selectedLocation) {
    return null;
  }

  const latNumber = Number(selectedLocation.latitude);
  const lngNumber = Number(selectedLocation.longitude);
  const formattedLatitude = !Number.isNaN(latNumber) ? latNumber.toFixed(6) : '';
  const formattedLongitude = !Number.isNaN(lngNumber) ? lngNumber.toFixed(6) : '';

  /**
   * Actualiza los valores de los campos del formulario y limpia el error correspondiente.
   *
   * @param {import('react').ChangeEvent<HTMLInputElement|HTMLTextAreaElement|HTMLSelectElement>} e - Evento de cambio.
   * @returns {void}
   */
  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));

    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: null }));
    }
    if (serverError) {
      setServerError(null);
    }
  };

  /**
   * Maneja el cambio en el selector de archivos multimedia.
   * Valida formato (JPEG/PNG) y tamaño máximo (5MB) en el cliente y genera la previsualización.
   *
   * @param {import('react').ChangeEvent<HTMLInputElement>} e - Evento de selección de archivo.
   * @returns {void}
   */
  const handleFileChange = (e) => {
    const file = e.target.files?.[0];
    if (!file) {
      return;
    }

    const allowedTypes = ['image/jpeg', 'image/png', 'image/jpg'];
    if (!allowedTypes.includes(file.type.toLowerCase())) {
      setErrors((prev) => ({
        ...prev,
        file: 'Formato no permitido. Solo se aceptan imágenes JPEG y PNG.',
      }));
      return;
    }

    const maxSizeBytes = 5 * 1024 * 1024;
    if (file.size > maxSizeBytes) {
      setErrors((prev) => ({
        ...prev,
        file: 'El tamaño de la imagen no puede superar los 5MB.',
      }));
      return;
    }

    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
    }

    setErrors((prev) => ({ ...prev, file: null }));
    setSelectedFile(file);
    setPreviewUrl(URL.createObjectURL(file));
  };

  /**
   * Descarta el archivo multimedia seleccionado y limpia la vista previa asociada.
   *
   * @returns {void}
   */
  const handleRemoveFile = () => {
    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
    }
    setSelectedFile(null);
    setPreviewUrl(null);
    setErrors((prev) => ({ ...prev, file: null }));
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  /**
   * Valida exhaustivamente los campos requeridos en el cliente antes de emitir la petición HTTP.
   *
   * @returns {boolean} True si el formulario es válido, False en caso contrario.
   */
  const validateForm = () => {
    const newErrors = {};

    const trimmedTitle = formData.title.trim();
    if (!trimmedTitle) {
      newErrors.title = 'El título de la incidencia es obligatorio.';
    } else if (trimmedTitle.length < 3) {
      newErrors.title = 'El título debe tener al menos 3 caracteres.';
    } else if (trimmedTitle.length > 100) {
      newErrors.title = 'El título no puede superar los 100 caracteres.';
    }

    const trimmedDesc = formData.description.trim();
    if (!trimmedDesc) {
      newErrors.description = 'La descripción es obligatoria.';
    } else if (trimmedDesc.length < 10) {
      newErrors.description = 'La descripción debe tener al menos 10 caracteres.';
    }

    if (!formData.category) {
      newErrors.category = 'Debe seleccionar una categoría del catálogo.';
    } else if (!INCIDENT_CATEGORIES.includes(formData.category)) {
      newErrors.category = 'La categoría seleccionada no es válida.';
    }

    if (
      Number.isNaN(latNumber) ||
      Number.isNaN(lngNumber) ||
      latNumber < -90 ||
      latNumber > 90 ||
      lngNumber < -180 ||
      lngNumber > 180
    ) {
      newErrors.coordinates = 'Las coordenadas geográficas capturadas no son válidas.';
    }

    if (errors.file) {
      newErrors.file = errors.file;
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  /**
   * Procesa el envío del formulario de creación, adjunta la imagen si fue suministrada y notifica al contenedor.
   *
   * @param {import('react').FormEvent<HTMLFormElement>} e - Evento de envío del formulario.
   * @returns {Promise<void>}
   */
  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    setIsSubmitting(true);
    setServerError(null);

    const incidentPayload = {
      title: formData.title.trim(),
      description: formData.description.trim(),
      category: formData.category,
      latitude: latNumber,
      longitude: lngNumber,
    };

    try {
      let createdIncident;
      if (onSubmit) {
        createdIncident = await onSubmit(incidentPayload);
      } else {
        createdIncident = await incidentService.createIncident(incidentPayload);
      }

      // Si el usuario proporcionó un archivo multimedia, se sube y asocia a la incidencia
      if (selectedFile && createdIncident?.id) {
        try {
          await incidentService.uploadIncidentAttachment(createdIncident.id, selectedFile);
        } catch (uploadErr) {
          console.error('Error al subir la imagen adjunta a la incidencia:', uploadErr);
        }
      }

      // Limpieza de estado local tras éxito
      setFormData({
        title: '',
        description: '',
        category: '',
      });
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
      }
      setSelectedFile(null);
      setPreviewUrl(null);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
      setErrors({});

      if (onCreated) {
        onCreated(createdIncident);
      }
    } catch (err) {
      const errorMessage =
        err?.response?.data?.message ||
        err?.message ||
        'Error al registrar la incidencia. Por favor, revise los datos e inténtelo de nuevo.';
      setServerError(errorMessage);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div
      className="modal-backdrop"
      onClick={handleDismiss}
      role="presentation"
    >
      <div
        className="modal-card"
        role="dialog"
        aria-modal="true"
        aria-labelledby="create-incident-modal-title"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="modal-header">
          <h2 id="create-incident-modal-title" className="modal-title">
            Nueva Incidencia Urbana
          </h2>
          <button
            type="button"
            className="modal-close-btn"
            onClick={handleDismiss}
            disabled={isSubmitting}
            aria-label="Cerrar modal"
          >
            &times;
          </button>
        </div>

        <form onSubmit={handleSubmit} noValidate>
          <div className="modal-body">
            {serverError && (
              <div className="modal-alert-error" role="alert">
                <span aria-hidden="true">⚠️</span>
                <span>{serverError}</span>
              </div>
            )}

            {/* Campo Título */}
            <div className="form-group">
              <label htmlFor="incident-title" className="form-label">
                Título <span className="required-asterisk">*</span>
              </label>
              <input
                ref={titleInputRef}
                id="incident-title"
                name="title"
                type="text"
                className={`form-input ${errors.title ? 'is-invalid' : ''}`}
                placeholder="Ej. Baches profundos en calzada"
                value={formData.title}
                onChange={handleInputChange}
                disabled={isSubmitting}
                maxLength={100}
                required
              />
              {errors.title && (
                <span className="input-error-msg" role="alert">
                  {errors.title}
                </span>
              )}
            </div>

            {/* Campo Categoría */}
            <div className="form-group">
              <label htmlFor="incident-category" className="form-label">
                Categoría <span className="required-asterisk">*</span>
              </label>
              <select
                id="incident-category"
                name="category"
                className={`form-select ${errors.category ? 'is-invalid' : ''}`}
                value={formData.category}
                onChange={handleInputChange}
                disabled={isSubmitting}
                required
              >
                <option value="">-- Selecciona una categoría --</option>
                {INCIDENT_CATEGORIES.map((cat) => (
                  <option key={cat} value={cat}>
                    {cat}
                  </option>
                ))}
              </select>
              {errors.category && (
                <span className="input-error-msg" role="alert">
                  {errors.category}
                </span>
              )}
            </div>

            {/* Campo Descripción */}
            <div className="form-group">
              <label htmlFor="incident-description" className="form-label">
                Descripción <span className="required-asterisk">*</span>
              </label>
              <textarea
                id="incident-description"
                name="description"
                rows={4}
                className={`form-textarea ${errors.description ? 'is-invalid' : ''}`}
                placeholder="Indica con detalle el estado del desperfecto, posibles peligros y referencias de ubicación..."
                value={formData.description}
                onChange={handleInputChange}
                disabled={isSubmitting}
                required
              />
              {errors.description && (
                <span className="input-error-msg" role="alert">
                  {errors.description}
                </span>
              )}
            </div>

            {/* Campo Adjunto Multimedia (Opcional) */}
            <div className="form-group">
              <label htmlFor="incident-attachment" className="form-label">
                Fotografía o imagen adjunta <span className="optional-tag">(Opcional · JPEG o PNG, máx. 5MB)</span>
              </label>
              <div className="file-upload-control">
                <input
                  ref={fileInputRef}
                  id="incident-attachment"
                  name="attachment"
                  type="file"
                  accept="image/jpeg,image/png"
                  className={`form-input-file ${errors.file ? 'is-invalid' : ''}`}
                  onChange={handleFileChange}
                  disabled={isSubmitting}
                  aria-label="Seleccionar imagen adjunta"
                />
              </div>

              {errors.file && (
                <span className="input-error-msg" role="alert">
                  {errors.file}
                </span>
              )}

              {/* Vista previa de imagen seleccionada */}
              {previewUrl && (
                <div className="image-preview-card">
                  <div className="image-preview-thumbnail-wrapper">
                    <img
                      src={previewUrl}
                      alt="Vista previa de la fotografía adjunta"
                      className="image-preview-thumbnail"
                    />
                    <button
                      type="button"
                      className="image-preview-remove-btn"
                      onClick={handleRemoveFile}
                      disabled={isSubmitting}
                      title="Quitar imagen"
                      aria-label="Quitar fotografía adjunta"
                    >
                      &times;
                    </button>
                  </div>
                  <div className="image-preview-meta">
                    <span className="image-preview-name">{selectedFile?.name}</span>
                    <span className="image-preview-size">
                      {selectedFile ? (selectedFile.size / 1024 / 1024).toFixed(2) + ' MB' : ''}
                    </span>
                  </div>
                </div>
              )}
            </div>

            {/* Coordenadas en sólo lectura */}
            <div className="form-group">
              <label className="form-label">
                Ubicación geolocalizada (Sólo lectura)
              </label>
              <div className="coordinates-grid">
                <div>
                  <input
                    type="text"
                    id="incident-latitude"
                    aria-label="Latitud"
                    className="form-input form-input-readonly"
                    value={formattedLatitude ? `Lat: ${formattedLatitude}` : ''}
                    readOnly
                  />
                </div>
                <div>
                  <input
                    type="text"
                    id="incident-longitude"
                    aria-label="Longitud"
                    className="form-input form-input-readonly"
                    value={formattedLongitude ? `Lng: ${formattedLongitude}` : ''}
                    readOnly
                  />
                </div>
              </div>
              <p className="coordinates-hint">
                Punto geográfico fijado por pulsación sobre el mapa interactivo.
              </p>
              {errors.coordinates && (
                <span className="input-error-msg" role="alert">
                  {errors.coordinates}
                </span>
              )}
            </div>
          </div>

          <div className="modal-footer">
            <button
              type="button"
              className="btn-secondary"
              onClick={handleDismiss}
              disabled={isSubmitting}
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="btn-primary"
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Guardando...' : 'Guardar incidencia'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
