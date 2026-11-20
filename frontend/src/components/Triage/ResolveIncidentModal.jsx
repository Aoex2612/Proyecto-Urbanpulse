/**
 * @fileoverview Modal de intervención técnica para reportar la resolución de una incidencia en UrbanPulse.
 * Permite a los técnicos asignados y administradores registrar la memoria de actuaciones realizadas
 * sobre el terreno y transicionar el expediente al estado RESOLVED, aplicando la validación de
 * justificación técnica obligatoria (>= 10 caracteres) para prioridades medias, altas y críticas.
 */

import { useState, useEffect, useRef, useCallback } from 'react';
import triageService from '../../services/triageService';
import { getPriorityLabel } from '../Map/mapConstants';
import './ResolveIncidentModal.css';

/**
 * Longitud mínima obligatoria de la memoria de actuaciones para prioridades no menores.
 * @type {number}
 */
const MIN_RESOLUTION_NOTES_LENGTH = 10;

/**
 * Componente modal accesible para el reporte de resolución de intervenciones técnicas.
 *
 * @param {Object} props - Propiedades del componente.
 * @param {boolean} [props.isOpen=true] - Controla la visibilidad del modal.
 * @param {Function} props.onClose - Callback invocado al descartar o cancelar el modal.
 * @param {Object|null} props.incident - Objeto con los datos de la incidencia a resolver.
 * @param {Function} [props.onIncidentResolved] - Callback ejecutado con la incidencia actualizada.
 * @returns {import('react').JSX.Element|null} Elemento JSX del modal de resolución o null si está cerrado.
 */
export default function ResolveIncidentModal({
  isOpen = true,
  onClose,
  incident,
  onIncidentResolved,
}) {
  const [resolutionNotes, setResolutionNotes] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [validationError, setValidationError] = useState(null);

  const closeBtnRef = useRef(null);

  // Autofoco accesible al abrir el modal
  useEffect(() => {
    if (!isOpen || !incident) {
      return;
    }

    const timer = setTimeout(() => {
      closeBtnRef.current?.focus();
    }, 50);

    return () => clearTimeout(timer);
  }, [isOpen, incident]);

  const handleDismiss = useCallback(() => {
    setResolutionNotes('');
    setError(null);
    setValidationError(null);
    if (onClose) {
      onClose();
    }
  }, [onClose]);

  // Manejo de pulsación en el fondo oscuro exterior para descartar el modal
  const handleBackdropClick = (e) => {
    if (e.target === e.currentTarget) {
      handleDismiss();
    }
  };

  // Manejador tecla Escape
  useEffect(() => {
    if (!isOpen) return;

    /**
     * @param {KeyboardEvent} e
     */
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        handleDismiss();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, handleDismiss]);

  if (!isOpen || !incident) {
    return null;
  }

  const priority = incident.priority || 'MEDIUM';
  const isNotesRequired = priority !== 'LOW';
  const trimmedNotes = resolutionNotes.trim();
  const currentLength = trimmedNotes.length;
  const isLengthValid = !isNotesRequired || currentLength >= MIN_RESOLUTION_NOTES_LENGTH;

  /**
   * Envía la solicitud de resolución técnica al backend.
   *
   * @param {import('react').FormEvent} e - Evento de envío.
   */
  const handleSubmit = async (e) => {
    e.preventDefault();

    if (isNotesRequired && currentLength < MIN_RESOLUTION_NOTES_LENGTH) {
      setValidationError(
        `La descripción de actuaciones es obligatoria y debe tener al menos ${MIN_RESOLUTION_NOTES_LENGTH} caracteres (actual: ${currentLength}).`
      );
      return;
    }

    setLoading(true);
    setError(null);
    setValidationError(null);

    try {
      const updatedIncident = await triageService.resolveIncident(incident.id, {
        resolutionNotes: trimmedNotes,
      });

      if (onIncidentResolved) {
        onIncidentResolved(updatedIncident);
      }
      handleDismiss();
    } catch (err) {
      const msg =
        err?.response?.data?.message ||
        'No se pudo reportar la resolución de la incidencia. Por favor, revisa los datos e inténtalo de nuevo.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      className="modal-backdrop"
      onClick={handleBackdropClick}
      role="presentation"
    >
      <div
        className="modal-card resolve-incident-modal-card"
        role="dialog"
        aria-modal="true"
        aria-labelledby="resolve-modal-title"
      >
        {/* Cabecera */}
        <header className="modal-header">
          <div className="modal-header-text">
            <span className="modal-eyebrow">
              Resolución Técnica • Incidencia #{incident.id}
            </span>
            <h2 id="resolve-modal-title" className="modal-title">
              Reportar Resolución de Incidencia
            </h2>
          </div>
          <button
            ref={closeBtnRef}
            type="button"
            className="modal-close-btn"
            onClick={handleDismiss}
            aria-label="Cerrar modal de resolución"
          >
            &times;
          </button>
        </header>

        {/* Formulario */}
        <form onSubmit={handleSubmit} className="modal-body resolve-modal-body">
          {/* Resumen de la Incidencia */}
          <section className="resolve-summary-card">
            <h3 className="resolve-incident-title">{incident.title}</h3>
            <div className="resolve-badges-row">
              <span className={`priority-badge priority-${priority.toLowerCase()}`}>
                Prioridad: {getPriorityLabel(priority)}
              </span>
              {incident.category && (
                <span className="category-badge">{incident.category}</span>
              )}
            </div>

            <p className="resolve-guideline-note">
              {isNotesRequired ? (
                <>
                  ⚠️ Al tratarse de una incidencia de prioridad{' '}
                  <strong>{getPriorityLabel(priority)}</strong>, es obligatorio
                  detallar las actuaciones técnicas llevadas a cabo (mínimo{' '}
                  {MIN_RESOLUTION_NOTES_LENGTH} caracteres).
                </>
              ) : (
                <>
                  ℹ️ Para incidencias de prioridad Baja, la descripción de
                  actuaciones es opcional pero recomendada para la memoria del expediente.
                </>
              )}
            </p>
          </section>

          {/* Campo Memoria de Actuaciones */}
          <div className="form-group">
            <div className="label-with-counter">
              <label htmlFor="resolution-notes-input" className="form-label">
                Memoria de Actuaciones Realizadas {isNotesRequired && '*'}
              </label>
              {isNotesRequired && (
                <span
                  className={`char-counter ${
                    currentLength < MIN_RESOLUTION_NOTES_LENGTH
                      ? 'counter-insufficient'
                      : 'counter-valid'
                  }`}
                  aria-live="polite"
                >
                  {currentLength} / {MIN_RESOLUTION_NOTES_LENGTH} caracteres mín.
                </span>
              )}
            </div>

            <textarea
              id="resolution-notes-input"
              name="resolutionNotes"
              className={`form-textarea ${
                validationError ? 'input-error-border' : ''
              }`}
              rows={4}
              placeholder="Describe detalladamente las reparaciones, comprobaciones o actuaciones ejecutadas sobre el terreno..."
              value={resolutionNotes}
              onChange={(e) => {
                setResolutionNotes(e.target.value);
                if (validationError) setValidationError(null);
              }}
              disabled={loading}
              minLength={isNotesRequired ? MIN_RESOLUTION_NOTES_LENGTH : undefined}
              required={isNotesRequired}
            />

            {validationError && (
              <p className="field-error-message" role="alert">
                {validationError}
              </p>
            )}
          </div>

          {/* Error de la llamada al servidor */}
          {error && (
            <div className="resolve-error-banner" role="alert">
              ⚠️ {error}
            </div>
          )}

          {/* Pie de acciones */}
          <footer className="modal-footer resolve-modal-footer">
            <button
              type="button"
              className="btn-secondary"
              onClick={handleDismiss}
              disabled={loading}
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="btn-primary btn-confirm-resolve"
              disabled={loading || !isLengthValid}
            >
              {loading ? 'Resolviendo...' : 'Confirmar Resolución'}
            </button>
          </footer>
        </form>
      </div>
    </div>
  );
}
