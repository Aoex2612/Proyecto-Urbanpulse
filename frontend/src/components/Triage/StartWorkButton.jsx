/**
 * @fileoverview Botón de acción operativa para iniciar trabajos sobre una incidencia en UrbanPulse.
 * Invoca el endpoint start-work transicionando la incidencia a estado IN_PROGRESS.
 * Se habilita cuando la incidencia se encuentra en estado ASSIGNED o REOPENED.
 */

import { useState } from 'react';
import triageService from '../../services/triageService';

/**
 * Componente de acción para iniciar la intervención técnica sobre el terreno.
 *
 * @param {Object} props - Propiedades del componente.
 * @param {number|string} props.incidentId - Identificador único de la incidencia.
 * @param {string} props.incidentStatus - Estado actual de la incidencia.
 * @param {Function} [props.onWorkStarted] - Callback ejecutado con la incidencia actualizada a IN_PROGRESS.
 * @param {boolean} [props.disabled=false] - Deshabilita externamente la acción.
 * @param {string} [props.className=''] - Clases CSS adicionales.
 * @returns {import('react').JSX.Element} Botón de inicio de trabajo técnico.
 */
export default function StartWorkButton({
  incidentId,
  incidentStatus,
  onWorkStarted,
  disabled = false,
  className = '',
}) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const canStart =
    incidentStatus === 'ASSIGNED' || incidentStatus === 'REOPENED';

  const handleClick = async () => {
    if (!incidentId || !canStart || loading || disabled) return;

    setLoading(true);
    setError(null);

    try {
      const updated = await triageService.startWork(incidentId);
      if (onWorkStarted) {
        onWorkStarted(updated);
      }
    } catch (err) {
      const msg =
        err?.response?.data?.message ||
        'Error al iniciar los trabajos de intervención técnica.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="start-work-container" style={{ display: 'inline-flex', flexDirection: 'column' }}>
      <button
        type="button"
        className={`btn-start-work ${className}`.trim()}
        onClick={handleClick}
        disabled={disabled || loading || !canStart}
        aria-label={`Iniciar trabajo en incidencia #${incidentId}`}
      >
        {loading ? 'Iniciando...' : '🛠️ Iniciar Trabajo'}
      </button>
      {error && (
        <span className="start-work-error" role="alert" style={{ color: '#dc2626', fontSize: '0.75rem', marginTop: '0.25rem' }}>
          {error}
        </span>
      )}
    </div>
  );
}
