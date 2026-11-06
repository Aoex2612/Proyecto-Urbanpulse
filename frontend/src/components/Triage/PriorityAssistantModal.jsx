/**
 * @fileoverview Modal interactivo de Asistente de Priorización para el Centro de Triaje de UrbanPulse.
 * Permite a operadores y administradores evaluar factores de corte de vía y riesgo de seguridad,
 * obtener el cálculo asistido por la matriz determinista del backend y fijar la prioridad definitiva
 * con su justificación explicativa.
 */

import { useState, useEffect, useRef, useCallback } from 'react';
import triageService from '../../services/triageService';
import { getPriorityLabel } from '../Map/mapConstants';
import './PriorityAssistantModal.css';

/**
 * Componente modal accesible para el asistente de priorización y fijación de prioridad operativa.
 *
 * @param {Object} props - Propiedades del componente.
 * @param {boolean} [props.isOpen=true] - Controla la visibilidad del modal.
 * @param {Function} props.onClose - Función invocada al cerrar o descartar el modal.
 * @param {Object|null} props.incident - Datos de la incidencia sobre la que se realiza el triaje.
 * @param {Function} [props.onPriorityUpdated] - Callback emitido con la incidencia actualizada.
 * @param {Function} [props.onConfirm] - Alias de callback de confirmación compatible.
 * @returns {import('react').JSX.Element|null} Elemento JSX del modal o null si está cerrado.
 */
export default function PriorityAssistantModal({
  isOpen = true,
  onClose,
  incident,
  onPriorityUpdated,
  onConfirm,
}) {
  const [streetCut, setStreetCut] = useState(false);
  const [safetyRisk, setSafetyRisk] = useState('NONE');
  const [suggestedPriority, setSuggestedPriority] = useState(null);
  const [suggestedReason, setSuggestedReason] = useState(null);

  const [finalPriority, setFinalPriority] = useState(incident?.priority || 'MEDIUM');
  const [finalReason, setFinalReason] = useState(incident?.priorityReason || '');

  const [calculating, setCalculating] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const closeBtnRef = useRef(null);

  // Sincronizar estado cuando cambia la incidencia recibida
  useEffect(() => {
    if (!isOpen || !incident) {
      return;
    }

    let isMounted = true;

    Promise.resolve().then(() => {
      if (isMounted) {
        setFinalPriority(incident.priority || 'MEDIUM');
        setFinalReason(incident.priorityReason || '');
        setStreetCut(false);
        setSafetyRisk('NONE');
        setSuggestedPriority(null);
        setSuggestedReason(null);
        setError(null);
      }
    });

    return () => {
      isMounted = false;
    };
  }, [isOpen, incident]);

  // Manejo de cierre memorizado
  const handleDismiss = useCallback(() => {
    if (!saving && onClose) {
      onClose();
    }
  }, [saving, onClose]);

  // Cierre mediante tecla Escape y autofoco en botón de cierre
  useEffect(() => {
    if (isOpen && incident) {
      const timer = setTimeout(() => {
        closeBtnRef.current?.focus();
      }, 50);

      /**
       * Listener de teclado para capturar Escape.
       *
       * @param {KeyboardEvent} e - Evento de teclado.
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
  }, [isOpen, incident, handleDismiss]);

  if (!isOpen || !incident) {
    return null;
  }

  /**
   * Clic en el fondo semitransparente exterior para descartar.
   *
   * @param {import('react').MouseEvent} e - Evento de clic.
   */
  const handleBackdropClick = (e) => {
    if (e.target === e.currentTarget) {
      handleDismiss();
    }
  };

  /**
   * Invoca el endpoint del backend para calcular la prioridad asistida según los factores seleccionados.
   *
   * @returns {Promise<void>}
   */
  const handleCalculate = async () => {
    setCalculating(true);
    setError(null);
    try {
      const result = await triageService.calculatePriority(incident.id, {
        streetCut,
        safetyRisk,
      });
      setSuggestedPriority(result.suggestedPriority);
      setSuggestedReason(result.priorityReason);
      setFinalPriority(result.suggestedPriority);
      setFinalReason(result.priorityReason);
    } catch (err) {
      setError(
        err?.response?.data?.message ||
          'Error al calcular la prioridad asistida. Verifique la conexión con el servidor.'
      );
    } finally {
      setCalculating(false);
    }
  };

  /**
   * Confirma y guarda la prioridad definitiva de la incidencia en el servidor.
   *
   * @param {import('react').FormEvent} e - Evento de envío del formulario.
   * @returns {Promise<void>}
   */
  const handleSavePriority = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      const updated = await triageService.updatePriority(incident.id, {
        priority: finalPriority,
        priorityReason: finalReason.trim(),
      });
      if (onPriorityUpdated) {
        onPriorityUpdated(updated);
      }
      if (onConfirm) {
        onConfirm(updated);
      }
      if (onClose) {
        onClose();
      }
    } catch (err) {
      setError(
        err?.response?.data?.message ||
          'Error al actualizar la prioridad. Asegúrese de que la incidencia se encuentra en un estado editable.'
      );
    } finally {
      setSaving(false);
    }
  };

  return (
    <div
      className="modal-backdrop"
      onClick={handleBackdropClick}
      role="presentation"
    >
      <div
        className="priority-modal-card"
        role="dialog"
        aria-modal="true"
        aria-labelledby="priority-assistant-title"
      >
        {/* Cabecera del modal */}
        <header className="priority-modal-header">
          <div>
            <span className="priority-modal-eyebrow">
              Triaje Operativo &bull; Incidencia #{incident.id}
            </span>
            <h2 id="priority-assistant-title" className="priority-modal-title">
              Asistente de Priorización
            </h2>
          </div>
          <button
            ref={closeBtnRef}
            type="button"
            className="modal-close-btn"
            onClick={handleDismiss}
            aria-label="Cerrar asistente de prioridad"
          >
            &times;
          </button>
        </header>

        {/* Formulario */}
        <form onSubmit={handleSavePriority}>
          <div className="priority-modal-body">
            {error && <div className="priority-error-banner" role="alert">{error}</div>}

            {/* Factores de Evaluación */}
            <div className="priority-section">
              <h3 className="priority-section-title">Factores de Riesgo e Impacto</h3>

              <div className="priority-field-group">
                <label className="priority-checkbox-label">
                  <input
                    type="checkbox"
                    className="priority-checkbox"
                    checked={streetCut}
                    onChange={(e) => setStreetCut(e.target.checked)}
                    aria-label="Corte de vía o calle cortada"
                  />
                  <span>Corte de vía / afección grave al tráfico</span>
                </label>
              </div>

              <div className="priority-field-group">
                <label htmlFor="safety-risk-select" className="priority-label">
                  Riesgo para la seguridad física o peatonal
                </label>
                <select
                  id="safety-risk-select"
                  className="priority-select"
                  value={safetyRisk}
                  onChange={(e) => setSafetyRisk(e.target.value)}
                >
                  <option value="NONE">Sin riesgo de seguridad (NONE)</option>
                  <option value="LOW">Riesgo leve o potencial (LOW)</option>
                  <option value="HIGH">Riesgo inminente o grave (HIGH)</option>
                </select>
              </div>

              <button
                type="button"
                className="priority-calc-btn"
                onClick={handleCalculate}
                disabled={calculating || saving}
              >
                {calculating ? 'Calculando sugerencia...' : 'Calcular prioridad sugerida'}
              </button>
            </div>

            {/* Resultado sugerido */}
            {suggestedPriority && (
              <div className="priority-suggestion-card" aria-live="polite">
                <div className="priority-suggestion-header">
                  <span className="priority-suggestion-label">Sugerencia recomendada:</span>
                  <span
                    className={`priority-badge-lg priority-${suggestedPriority.toLowerCase()}`}
                  >
                    {getPriorityLabel(suggestedPriority)}
                  </span>
                </div>
                {suggestedReason && (
                  <p className="priority-suggestion-text">{suggestedReason}</p>
                )}
              </div>
            )}

            {/* Decisión final del operador */}
            <div className="priority-section">
              <h3 className="priority-section-title">Fijación de Prioridad Definitiva</h3>

              <div className="priority-field-group">
                <label htmlFor="final-priority-select" className="priority-label">
                  Prioridad a aplicar
                </label>
                <select
                  id="final-priority-select"
                  className="priority-select"
                  value={finalPriority}
                  onChange={(e) => setFinalPriority(e.target.value)}
                >
                  <option value="LOW">Baja (LOW)</option>
                  <option value="MEDIUM">Media (MEDIUM)</option>
                  <option value="HIGH">Alta (HIGH)</option>
                  <option value="CRITICAL">Crítica (CRITICAL)</option>
                </select>
              </div>

              <div className="priority-field-group">
                <label htmlFor="final-reason-textarea" className="priority-label">
                  Justificación de la prioridad
                </label>
                <textarea
                  id="final-reason-textarea"
                  className="priority-textarea"
                  rows={3}
                  value={finalReason}
                  onChange={(e) => setFinalReason(e.target.value)}
                  placeholder="Explique las circunstancias que motivan este nivel de prioridad..."
                />
              </div>
            </div>
          </div>

          {/* Pie de acción */}
          <footer className="priority-modal-footer">
            <button
              type="button"
              className="priority-btn-secondary"
              onClick={handleDismiss}
              disabled={saving}
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="priority-btn-primary"
              disabled={saving || calculating}
            >
              {saving ? 'Guardando...' : 'Confirmar Prioridad'}
            </button>
          </footer>
        </form>
      </div>
    </div>
  );
}
