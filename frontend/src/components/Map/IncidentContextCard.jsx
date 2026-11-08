/**
 * @fileoverview Componente de tarjeta para visualizar las observaciones de contexto
 * externo vinculadas a una incidencia urbana (HU17, RF-10).
 * Renderiza métricas meteorológicas (temperatura en °C, precipitación/lluvia y viento)
 * e información de estado del tráfico municipal en el momento del reporte, o un
 * mensaje informativo ante degradación controlada / falta de datos.
 */

import { formatDateTime } from '../../utils/dateUtils';
import './IncidentContextCard.css';

/**
 * @typedef {Object} ExternalObservation
 * @property {number} [id] - Identificador único de la observación.
 * @property {string} [source] - Fuente de datos externa (ej. OPEN_METEO, TRAFFIC_MALAGA).
 * @property {string} metricType - Tipo de métrica (TEMPERATURE_C, PRECIPITATION_MM, WIND_SPEED_KMH, TRAFFIC_LEVEL).
 * @property {number|null} [valueNum] - Valor numérico registrado.
 * @property {string|null} [valueText] - Valor textual registrado.
 * @property {string} [observedAt] - Marca temporal ISO de la observación.
 */

/**
 * Propiedades del componente IncidentContextCard.
 *
 * @param {Object} props
 * @param {ExternalObservation[]} [props.observations=[]] - Listado de observaciones registradas para la incidencia.
 * @returns {import('react').JSX.Element}
 */
export default function IncidentContextCard({ observations = [] }) {
  if (!observations || observations.length === 0) {
    return (
      <section className="incident-context-card" aria-labelledby="context-card-title">
        <div className="incident-context-header">
          <h4 id="context-card-title" className="detail-section-heading">
            <span className="context-header-icon" aria-hidden="true">🌤️</span>
            Contexto Ambiental y de Tráfico
          </h4>
        </div>
        <p className="incident-context-empty">
          No hay lecturas meteorológicas ni de tráfico registradas en el instante del aviso (degradación controlada o sin cobertura de sensores).
        </p>
      </section>
    );
  }

  // Búsqueda de métricas específicas
  const tempObs = observations.find(
    (o) => o.metricType === 'TEMPERATURE_C' || o.metricType === 'TEMPERATURE'
  );
  const precipObs = observations.find(
    (o) => o.metricType === 'PRECIPITATION_MM' || o.metricType === 'PRECIPITATION'
  );
  const windObs = observations.find(
    (o) => o.metricType === 'WIND_SPEED_KMH' || o.metricType === 'WIND'
  );
  const trafficObs = observations.find(
    (o) => o.metricType === 'TRAFFIC_LEVEL' || o.metricType === 'TRAFFIC'
  );

  // Fecha de la primera observación disponible
  const timestamp = observations[0]?.observedAt
    ? formatDateTime(observations[0].observedAt)
    : null;

  return (
    <section className="incident-context-card" aria-labelledby="context-card-title">
      <div className="incident-context-header">
        <h4 id="context-card-title" className="detail-section-heading">
          <span className="context-header-icon" aria-hidden="true">🌤️</span>
          Contexto Ambiental y de Tráfico
        </h4>
        {timestamp && (
          <span className="context-timestamp" title="Momento de captura del entorno">
            Capturado: {timestamp}
          </span>
        )}
      </div>

      <div className="context-metrics-grid">
        {/* Temperatura */}
        <div className="context-metric-box">
          <span className="metric-icon" aria-hidden="true">🌡️</span>
          <div className="metric-data">
            <span className="metric-label">Temperatura</span>
            <strong className="metric-value">
              {tempObs?.valueNum != null
                ? `${tempObs.valueNum.toFixed(1)} °C`
                : tempObs?.valueText || 'Sin datos'}
            </strong>
          </div>
        </div>

        {/* Precipitación / Lluvia */}
        <div className="context-metric-box">
          <span className="metric-icon" aria-hidden="true">🌧️</span>
          <div className="metric-data">
            <span className="metric-label">Precipitación</span>
            <strong className="metric-value">
              {precipObs?.valueNum != null
                ? `${precipObs.valueNum.toFixed(1)} mm`
                : precipObs?.valueText || '0.0 mm'}
            </strong>
          </div>
        </div>

        {/* Viento */}
        <div className="context-metric-box">
          <span className="metric-icon" aria-hidden="true">💨</span>
          <div className="metric-data">
            <span className="metric-label">Viento</span>
            <strong className="metric-value">
              {windObs?.valueNum != null
                ? `${windObs.valueNum.toFixed(1)} km/h`
                : windObs?.valueText || 'Sin datos'}
            </strong>
          </div>
        </div>

        {/* Tráfico Municipal */}
        <div className="context-metric-box">
          <span className="metric-icon" aria-hidden="true">🚗</span>
          <div className="metric-data">
            <span className="metric-label">Tráfico</span>
            <strong className="metric-value">
              {trafficObs?.valueText ||
                (trafficObs?.valueNum != null
                  ? `Nivel ${trafficObs.valueNum}`
                  : 'Normal')}
            </strong>
          </div>
        </div>
      </div>
    </section>
  );
}
