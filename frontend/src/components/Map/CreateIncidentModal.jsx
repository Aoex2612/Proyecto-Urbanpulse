/**
 * @fileoverview Modal de creación asistida de incidencias para UrbanPulse.
 * Despliega un formulario modal con validación en frontend para capturar el título,
 * descripción y categoría de la incidencia, mostrando las coordenadas geográficas
 * seleccionadas sobre el mapa en modo de solo lectura. Persiste la nueva incidencia
 * a través de incidentService y notifica al componente padre para actualizar el mapa.
 */

import { useState, useEffect, useRef, useCallback } from 'react';
import incidentService from '../../services/incidentService';
import { INCIDENT_CATEGORIES } from './mapConstants';
import './Map.css';

/**
 * Componente modal para dar de alta una nueva incidencia geolocalizada.
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

  const [errors, setErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [serverError, setServerError] = useState(null);

  const titleInputRef = useRef(null);

  // Manejador unificado de cierre y cancelación memorizado
  const handleDismiss = useCallback(() => {
    if (isSubmitting) return;

    setFormData({
      title: '',
      description: '',
      category: '',
    });
    setErrors({});
    setServerError(null);

    if (onClose) {
      onClose();
    } else if (onCancel) {
      onCancel();
    }
  }, [isSubmitting, onClose, onCancel]);

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

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  /**
   * Procesa el envío del formulario de creación y notifica al contenedor padre.
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

      // Limpieza de estado local tras éxito
      setFormData({
        title: '',
        description: '',
        category: '',
      });
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
