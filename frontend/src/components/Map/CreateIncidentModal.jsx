/**
 * @fileoverview Modal de creación asistida de incidencias para UrbanPulse.
 * Despliega un formulario modal con validación en frontend para capturar el título,
 * descripción, categoría y archivo multimedia adjunto (imagen JPEG/PNG opcional)
 * de la incidencia, mostrando las coordenadas geográficas seleccionadas sobre el mapa
 * en modo de solo lectura. Permite además obtener la ubicación actual del dispositivo
 * mediante la API de geolocalización del navegador (HU14.4). Persiste la nueva
 * incidencia a través de incidentService y sube el archivo adjunto si fue provisto,
 * notificando al componente padre para actualizar el mapa.
 */

import { useState, useEffect, useRef, useCallback } from 'react';
import incidentService from '../../services/incidentService';
import { INCIDENT_CATEGORIES } from './mapConstants';
import { isGeolocationSupported, getCurrentCoordinates } from '../../utils/geolocationUtils';
import './Map.css';

/**
 * Componente modal para dar de alta una nueva incidencia geolocalizada con soporte para adjunto multimedia
 * y captura de ubicación actual via API de geolocalización del navegador.
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

  const [locationInfo, setLocationInfo] = useState({
    address: null,
    district: null,
    neighborhood: null,
  });
  const [isLoadingLocation, setIsLoadingLocation] = useState(false);

  const [selectedFile, setSelectedFile] = useState(null);
  const [previewUrl, setPreviewUrl] = useState(null);

  const [errors, setErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [serverError, setServerError] = useState(null);

  /**
   * Coordenadas obtenidas por GPS. Cuando tiene valor, reemplaza a selectedLocation
   * como fuente de coordenadas activa para el formulario y el envío.
   * Al cerrarse el modal o al cambiar el punto del mapa, se resetea a null.
   *
   * @type {[{latitude: number, longitude: number}|null, Function]}
   */
  const [gpsLocation, setGpsLocation] = useState(null);

  /**
   * Indica si se está obteniendo la geolocalización del dispositivo en curso.
   * Bloquea el botón de envío y el botón de ubicación durante la petición.
   *
   * @type {[boolean, Function]}
   */
  const [isLocating, setIsLocating] = useState(false);

  /**
   * Mensaje de error de geolocalización, o null si no hay error activo.
   *
   * @type {[string|null, Function]}
   */
  const [geoError, setGeoError] = useState(null);

  /**
   * Indica si las coordenadas activas provienen de GPS (true) o del clic en el mapa (false).
   * Controla la visibilidad del badge de confirmación de ubicación GPS.
   *
   * @type {[boolean, Function]}
   */
  const [isLocationOverridden, setIsLocationOverridden] = useState(false);

  /**
   * Indica si la API de geolocalización está disponible en el navegador actual.
   * Se calcula una sola vez al montarse el componente.
   *
   * @type {[boolean, Function]}
   */
  const [isGeoSupported] = useState(() => isGeolocationSupported());

  const titleInputRef = useRef(null);
  const fileInputRef = useRef(null);

  /**
   * Ref de salvaguarda contra actualizaciones de estado en componentes desmontados.
   * Se establece a false en el cleanup del useEffect de ciclo de vida.
   *
   * @type {import('react').MutableRefObject<boolean>}
   */
  const isMountedRef = useRef(true);

  // Efecto de ciclo de vida: marcar desmontado al limpiar
  useEffect(() => {
    isMountedRef.current = true;
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  /**
   * Ubicación efectiva del formulario. Usa las coordenadas GPS si están disponibles,
   * o el punto seleccionado en el mapa en caso contrario.
   * Derivada durante el render sin necesidad de efectos de sincronización.
   *
   * @type {{latitude: number|string, longitude: number|string}|null}
   */
  const internalLocation = gpsLocation ?? selectedLocation;

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
    setLocationInfo({
      address: null,
      district: null,
      neighborhood: null,
    });
    setIsLoadingLocation(false);
    setErrors({});
    setServerError(null);

    // Resetear estados de geolocalización
    setIsLocating(false);
    setGeoError(null);
    setIsLocationOverridden(false);
    setGpsLocation(null);

    if (onClose) {
      onClose();
    } else if (onCancel) {
      onCancel();
    }
  }, [isSubmitting, onClose, onCancel, previewUrl]);

  // Resolución territorial automática (geocodificación inversa) al recibir coordenadas
  useEffect(() => {
    let isMounted = true;

    if (isOpen && internalLocation) {
      const lat = Number(internalLocation.latitude);
      const lng = Number(internalLocation.longitude);

      if (!Number.isNaN(lat) && !Number.isNaN(lng)) {
        Promise.resolve().then(() => {
          if (!isMounted) return;
          setIsLoadingLocation(true);
          incidentService
            .reverseGeocode(lat, lng)
            .then((result) => {
              if (isMounted) {
                setLocationInfo({
                  address: result?.address || null,
                  district: result?.district || null,
                  neighborhood: result?.neighborhood || null,
                });
                setIsLoadingLocation(false);
              }
            })
            .catch(() => {
              if (isMounted) {
                setLocationInfo({
                  address: null,
                  district: null,
                  neighborhood: null,
                });
                setIsLoadingLocation(false);
              }
            });
        });
      } else {
        Promise.resolve().then(() => {
          if (!isMounted) return;
          setLocationInfo({
            address: null,
            district: null,
            neighborhood: null,
          });
          setIsLoadingLocation(false);
        });
      }
    } else {
      Promise.resolve().then(() => {
        if (!isMounted) return;
        setLocationInfo({
          address: null,
          district: null,
          neighborhood: null,
        });
        setIsLoadingLocation(false);
      });
    }

    return () => {
      isMounted = false;
    };
  }, [isOpen, internalLocation]);

  // Foco automático en el primer campo al abrirse el modal y escucha de tecla Escape
  useEffect(() => {
    if (isOpen && internalLocation) {
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
  }, [isOpen, internalLocation, handleDismiss]);

  // Si no está abierto o no existen coordenadas válidas, no renderizar nada
  if (!isOpen || !internalLocation) {
    return null;
  }

  const latNumber = Number(internalLocation.latitude);
  const lngNumber = Number(internalLocation.longitude);
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
   * Solicita la ubicación actual del dispositivo mediante la API de geolocalización del navegador.
   * Actualiza gpsLocation con las coordenadas GPS obtenidas, convirtiendo éste en la fuente
   * activa de coordenadas para el formulario (RF-21, RF-22, RF-24).
   * Si el componente se desmonta antes de recibir respuesta, descarta la actualización de estado
   * para evitar memory leaks (salvaguarda con isMountedRef).
   * Preserva las coordenadas previas (mapa) ante cualquier error (RF-32).
   *
   * @async
   * @returns {Promise<void>}
   */
  const handleUseCurrentLocation = async () => {
    setIsLocating(true);
    setGeoError(null);
    setIsLocationOverridden(false);

    try {
      const coords = await getCurrentCoordinates();

      if (isMountedRef.current) {
        setGpsLocation({ latitude: coords.latitude, longitude: coords.longitude });
        setIsLocationOverridden(true);
        setGeoError(null);
      }
    } catch (err) {
      if (isMountedRef.current) {
        // Preservar gpsLocation/selectedLocation sin cambios (RF-32)
        setGeoError(err.message);
      }
    } finally {
      if (isMountedRef.current) {
        setIsLocating(false);
      }
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
      ...(locationInfo.address ? { address: locationInfo.address } : {}),
      ...(locationInfo.district ? { district: locationInfo.district } : {}),
      ...(locationInfo.neighborhood ? { neighborhood: locationInfo.neighborhood } : {}),
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
      setLocationInfo({
        address: null,
        district: null,
        neighborhood: null,
      });
      setIsLoadingLocation(false);
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

            {/* Sección Territorial: Dirección postal, Barrio, Distrito y Coordenadas en Sólo Lectura */}
            <div className="territorial-section">
              {/* Indicador de resolución territorial */}
              {isLoadingLocation && (
                <div className="location-resolving-indicator" role="status" aria-live="polite">
                  <span className="location-resolving-spinner" aria-hidden="true">📍</span>
                  <span>Resolviendo dirección...</span>
                </div>
              )}

              {/* Campo Dirección postal */}
              <div className="form-group">
                <div className="form-label-row">
                  <label htmlFor="incident-address" className="form-label">
                    Dirección postal
                  </label>
                  {!isLoadingLocation && !locationInfo.address && (
                    <span className="unassigned-badge">Sin asignar</span>
                  )}
                </div>
                <input
                  id="incident-address"
                  name="address"
                  type="text"
                  className="form-input form-input-readonly"
                  value={
                    isLoadingLocation
                      ? 'Resolviendo dirección...'
                      : locationInfo.address || 'Sin asignar'
                  }
                  readOnly
                  disabled={isSubmitting}
                  aria-readonly="true"
                />
              </div>

              {/* Rejilla de Barrio y Distrito */}
              <div className="territorial-grid">
                <div className="form-group">
                  <div className="form-label-row">
                    <label htmlFor="incident-neighborhood" className="form-label">
                      Barrio
                    </label>
                    {!isLoadingLocation && !locationInfo.neighborhood && (
                      <span className="unassigned-badge">Sin asignar</span>
                    )}
                  </div>
                  <input
                    id="incident-neighborhood"
                    name="neighborhood"
                    type="text"
                    className="form-input form-input-readonly"
                    value={
                      isLoadingLocation
                        ? 'Resolviendo dirección...'
                        : locationInfo.neighborhood || 'Sin asignar'
                    }
                    readOnly
                    disabled={isSubmitting}
                    aria-readonly="true"
                  />
                </div>

                <div className="form-group">
                  <div className="form-label-row">
                    <label htmlFor="incident-district" className="form-label">
                      Distrito
                    </label>
                    {!isLoadingLocation && !locationInfo.district && (
                      <span className="unassigned-badge">Sin asignar</span>
                    )}
                  </div>
                  <input
                    id="incident-district"
                    name="district"
                    type="text"
                    className="form-input form-input-readonly"
                    value={
                      isLoadingLocation
                        ? 'Resolviendo dirección...'
                        : locationInfo.district || 'Sin asignar'
                    }
                    readOnly
                    disabled={isSubmitting}
                    aria-readonly="true"
                  />
                </div>
              </div>

              {/* Coordenadas en sólo lectura con opción de geolocalización GPS */}
              <div className="form-group">
                <label className="form-label">
                  Ubicación geolocalizada (Sólo lectura)
                </label>

                {/* Botón de geolocalización GPS (RF-21, RF-22, RF-24, RF-25) */}
                <button
                  type="button"
                  className={`btn-current-location${isLocating ? ' is-locating' : ''}`}
                  onClick={handleUseCurrentLocation}
                  disabled={isLocating || !isGeoSupported}
                  aria-label="Usar mi ubicación actual por GPS"
                >
                  {isLocating ? 'Obteniendo ubicación...' : 'Usar mi ubicación actual'}
                </button>

                {/* Badge de confirmación GPS (RF-26) */}
                {isLocationOverridden && (
                  <p className="geo-status-badge" aria-live="polite">
                    Ubicación actual obtenida
                  </p>
                )}

                {/* Banner de error de geolocalización (RF-29, RF-30, RF-31) */}
                {geoError && (
                  <div className="geo-error-banner" role="alert" aria-live="assertive">
                    {geoError}
                  </div>
                )}

                {/* Aviso de geolocalización no soportada (RF-33) */}
                {!isGeoSupported && (
                  <p className="coordinates-hint" aria-live="polite">
                    Geolocalización no disponible en este navegador
                  </p>
                )}


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
              disabled={isSubmitting || isLocating}
            >
              {isSubmitting ? 'Guardando...' : 'Guardar incidencia'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
