/**
 * @fileoverview Modal accesible para la captura obligatoria de motivos en transiciones de ciclo de vida.
 * Empleado en el Centro de Triaje para:
 *  - Rechazo de incidencias (REPORTED/VALIDATED -> REJECTED, motivo >= 5 caracteres)
 *  - Reapertura de incidencias resueltas (RESOLVED -> REOPENED, motivo >= 5 caracteres)
 *  - Reevaluación de incidencias rechazadas (REJECTED -> REPORTED | VALIDATED, motivo >= 5 caracteres)
 */

import { useState, useEffect, useRef, useCallback } from 'react';
import './ReasonModal.css';

/**
 * Longitud mínima por defecto para los motivos de transición operativa.
 * @type {number}
 */
const DEFAULT_MIN_LENGTH = 5;

/**
 * Componente modal para solicitar y validar un motivo justificado en cambios de estado de incidencias.
 *
 * @param {Object} props - Propiedades del componente.
 * @param {boolean} [props.isOpen=true] - Visibilidad del modal.
 * @param {Function} props.onClose - Callback al cancelar o cerrar el diálogo.
 * @param {Function} props.onSubmit - Callback async `({ reason, targetStatus })` ejecutado al confirmar.
 * @param {'REJECT'|'REOPEN'|'REEVALUATE'|'CUSTOM'} [props.actionType='REJECT'] - Tipo de acción operativa a ejecutar.
 * @param {Object} props.incident - Objeto con los datos de la incidencia.
 * @param {string} [props.title] - Título personalizado para la cabecera.
 * @param {string} [props.description] - Texto descriptivo y explicativo para el usuario.
 * @param {string} [props.actionLabel] - Etiqueta textual del botón de confirmación.
 * @param {number} [props.minLength=5] - Longitud mínima requerida para el motivo.
 * @returns {import('react').JSX.Element|null} Elemento JSX del diálogo modal accesible o null si está cerrado.
 */
export default function ReasonModal({
  isOpen = true,
  onClose,
  onSubmit,
  actionType = 'REJECT',
  incident,
  title,
  description,
  actionLabel,
  minLength = DEFAULT_MIN_LENGTH,
}) {
  const [reason, setReason] = useState('');
  const [targetStatus, setTargetStatus] = useState('VALIDATED');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [validationError, setValidationError] = useState(null);

  const closeBtnRef = useRef(null);
  const textareaRef = useRef(null);

  const handleDismiss = useCallback(() => {
    setReason('');
    setTargetStatus('VALIDATED');
    setError(null);
    setValidationError(null);
    if (onClose) {
      onClose();
    }
  }, [onClose]);

  // Autofoco accesible al abrir
  useEffect(() => {
    if (!isOpen) return;

    const timer = setTimeout(() => {
      if (textareaRef.current) {
        textareaRef.current.focus();
      } else {
        closeBtnRef.current?.focus();
      }
    }, 50);

    return () => clearTimeout(timer);
  }, [isOpen]);

  // Manejo de la tecla Escape para cierre accesible
  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && !loading) {
        handleDismiss();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, loading, handleDismiss]);

  if (!isOpen || !incident) {
    return null;
  }

  // Textos predeterminados según actionType
  const getModalConfig = () => {
    switch (actionType) {
      case 'REOPEN':
        return {
          defaultTitle: `Reabrir Incidencia #${incident.id}`,
          defaultDesc: 'Indica la justificación operativa para reabrir este expediente resuelto y coordinar nuevas actuaciones.',
          defaultActionLabel: 'Confirmar Reapertura',
          btnClass: 'btn-reopen-confirm',
          placeholder: 'Explica por qué la resolución fue insuficiente o qué tareas restan...',
        };
      case 'REEVALUATE':
        return {
          defaultTitle: `Reevaluar Incidencia Rechazada #${incident.id}`,
          defaultDesc: 'Selecciona el estado operativo de retorno y aporta el motivo de rectificación del expediente.',
          defaultActionLabel: 'Confirmar Reevaluación',
          btnClass: 'btn-reevaluate-confirm',
          placeholder: 'Justifica la rectificación y los nuevos datos aportados...',
        };
      case 'REJECT':
      default:
        return {
          defaultTitle: `Rechazar Incidencia #${incident.id}`,
          defaultDesc: 'Indica el motivo por el cual se desestima o rechaza este reporte del circuito municipal. Se inactivarán las asignaciones.',
          defaultActionLabel: 'Confirmar Rechazo',
          btnClass: 'btn-reject-confirm',
          placeholder: 'Detalla el motivo del rechazo (improcedencia, competencia de otra entidad, duplicado, etc.)...',
        };
    }
  };

  const config = getModalConfig();
  const modalTitle = title || config.defaultTitle;
  const modalDesc = description || config.defaultDesc;
  const submitButtonLabel = actionLabel || config.defaultActionLabel;

  const trimmedReason = reason.trim();
  const currentLength = trimmedReason.length;
  const isLengthValid = currentLength >= minLength;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setValidationError(null);

    if (!isLengthValid) {
      setValidationError(`El motivo debe contener al menos ${minLength} caracteres (actualmente: ${currentLength}).`);
      return;
    }

    setLoading(true);
    try {
      if (onSubmit) {
        await onSubmit({
          reason: trimmedReason,
          targetStatus: actionType === 'REEVALUATE' ? targetStatus : undefined,
        });
      }
      handleDismiss();
    } catch (err) {
      const msg =
        err?.response?.data?.message ||
        err?.message ||
        'Error al procesar la transición de la incidencia.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      className="modal-backdrop reason-modal-backdrop"
      role="presentation"
      onClick={(e) => {
        if (e.target === e.currentTarget && !loading) {
          handleDismiss();
        }
      }}
    >
      <div
        className="modal-card reason-modal-card"
        role="dialog"
        aria-modal="true"
        aria-labelledby="reason-modal-title"
        aria-describedby="reason-modal-desc"
      >
        <header className="modal-header reason-modal-header">
          <div>
            <h3 id="reason-modal-title" className="modal-title">
              {modalTitle}
            </h3>
            <p id="reason-modal-desc" className="modal-subtitle">
              {modalDesc}
            </p>
          </div>
          <button
            ref={closeBtnRef}
            type="button"
            className="modal-close-btn"
            onClick={handleDismiss}
            disabled={loading}
            aria-label="Cerrar modal"
          >
            &times;
          </button>
        </header>

        <form onSubmit={handleSubmit} className="reason-modal-body">
          {error && (
            <div className="reason-alert reason-alert-error" role="alert">
              <span>{error}</span>
            </div>
          )}

          {actionType === 'REEVALUATE' && (
            <div className="reason-form-group">
              <label htmlFor="target-status-select" className="reason-label">
                Estado Operativo de Destino <span className="required-star">*</span>
              </label>
              <select
                id="target-status-select"
                className="reason-select"
                value={targetStatus}
                onChange={(e) => setTargetStatus(e.target.value)}
                disabled={loading}
              >
                <option value="VALIDATED">Validada (VALIDATED) — Lista para asignación</option>
                <option value="REPORTED">Reportada (REPORTED) — Pendiente de revisión inicial</option>
              </select>
            </div>
          )}

          <div className="reason-form-group">
            <div className="label-with-counter">
              <label htmlFor="reason-textarea" className="reason-label">
                Motivo / Justificación <span className="required-star">*</span>
              </label>
              <span
                className={`char-counter ${isLengthValid ? 'valid' : 'invalid'}`}
                aria-live="polite"
              >
                {currentLength} / {minLength} mín.
              </span>
            </div>

            <textarea
              ref={textareaRef}
              id="reason-textarea"
              className={`reason-textarea ${validationError ? 'has-error' : ''}`}
              rows={4}
              value={reason}
              onChange={(e) => {
                setReason(e.target.value);
                if (validationError) setValidationError(null);
              }}
              placeholder={config.placeholder}
              disabled={loading}
              aria-required="true"
              aria-invalid={!isLengthValid}
            />

            {validationError && (
              <p className="field-error-message" role="alert">
                {validationError}
              </p>
            )}
          </div>

          <footer className="modal-footer reason-modal-footer">
            <button
              type="button"
              className="btn-cancel"
              onClick={handleDismiss}
              disabled={loading}
            >
              Cancelar
            </button>
            <button
              type="submit"
              className={`btn-submit ${config.btnClass}`}
              disabled={loading || !isLengthValid}
            >
              {loading ? 'Procesando...' : submitButtonLabel}
            </button>
          </footer>
        </form>
      </div>
    </div>
  );
}

