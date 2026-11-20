/**
 * @fileoverview Componente de tarjetas de métricas ejecutivas (KPIs) para el Panel de Analítica Urbana.
 * Muestra el volumen total de incidencias (resueltas vs pendientes), tiempo medio de triaje (MTTT),
 * tiempo medio de resolución (MTTR), tasa global de cumplimiento de SLA y desempeño del asistente inteligente (RF-1 al RF-5, RF-11, RF-18).
 */

import { formatMetricHours, formatMetricPercent } from '../../utils/reportUtils';
import './AnalyticsKpiCards.css';

/**
 * Determina la clase CSS de estado según el porcentaje de cumplimiento de SLA.
 *
 * @param {number|null|undefined} rate - Tasa porcentual de cumplimiento (0 a 100).
 * @returns {string} Clase modificadora de color.
 */
const getSlaHealthClass = (rate) => {
  if (rate === null || rate === undefined || Number.isNaN(Number(rate))) {
    return 'kpi-sla-neutral';
  }
  const numeric = Number(rate);
  if (numeric >= 80) return 'kpi-sla-good';
  if (numeric >= 50) return 'kpi-sla-warning';
  return 'kpi-sla-danger';
};

/**
 * Obtiene la etiqueta explicativa del estado de cumplimiento de SLA.
 *
 * @param {number|null|undefined} rate - Tasa porcentual.
 * @returns {string} Etiqueta cualitativa.
 */
const getSlaHealthLabel = (rate) => {
  if (rate === null || rate === undefined || Number.isNaN(Number(rate))) {
    return 'Sin evaluar';
  }
  const numeric = Number(rate);
  if (numeric >= 80) return 'Cumplimiento óptimo';
  if (numeric >= 50) return 'En seguimiento';
  return 'Atención prioritaria';
};

/**
 * Componente que renderiza el panel ejecutivo de tarjetas de métricas operativas.
 *
 * @param {Object} props - Propiedades del componente.
 * @param {Object} [props.operationalMetrics] - Métricas operativas agregadas (OperationalMetricsResponse).
 * @param {Object} [props.assistantPerformance] - Desempeño del recomendador inteligente (AssistantPerformanceResponse).
 * @param {Object} [props.summaryData] - DTO consolidado alternativo de analítica (AnalyticsSummaryResponse).
 * @returns {import('react').JSX.Element} Conjunto de tarjetas de indicadores clave de rendimiento.
 */
export default function AnalyticsKpiCards({
  operationalMetrics = null,
  assistantPerformance = null,
  summaryData = null,
}) {
  const metrics = operationalMetrics || summaryData?.operationalMetrics || {};
  const assistant = assistantPerformance || summaryData?.assistantPerformance || {};

  const total = metrics.totalIncidents ?? 0;
  const resolved = metrics.resolvedIncidents ?? 0;
  const pending = metrics.pendingIncidents ?? (total >= resolved ? total - resolved : 0);

  const mttt = formatMetricHours(metrics.mtttHours);
  const mttr = formatMetricHours(metrics.mttrHours);

  const slaRate = metrics.slaComplianceRate ?? 0;
  const slaText = formatMetricPercent(slaRate);
  const slaCompliant = metrics.totalSlaCompliant ?? 0;
  const slaBreached = metrics.totalSlaBreached ?? 0;
  const slaHealthClass = getSlaHealthClass(metrics.slaComplianceRate);
  const slaHealthLabel = getSlaHealthLabel(metrics.slaComplianceRate);

  const adoptionRate = assistant.adoptionRate ?? 0;
  const adoptionText = formatMetricPercent(adoptionRate);
  const avgConfidence = assistant.averageConfidence ?? 0;
  const avgConfidenceText = formatMetricPercent(avgConfidence);
  const totalRecommendations = assistant.totalRecommendations ?? 0;
  const acceptedRecommendations = assistant.acceptedRecommendations ?? 0;

  return (
    <section className="analytics-kpi-container" aria-label="Indicadores Clave de Rendimiento">
      <div className="analytics-kpi-grid">
        {/* KPI 1: Volumen Total */}
        <article className="analytics-kpi-card" data-testid="kpi-card-total">
          <header className="analytics-kpi-header">
            <span className="analytics-kpi-icon-wrap" aria-hidden="true">
              <svg
                viewBox="0 0 24 24"
                className="analytics-kpi-icon"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                <polyline points="14 2 14 8 20 8" />
                <line x1="16" y1="13" x2="8" y2="13" />
                <line x1="16" y1="17" x2="8" y2="17" />
                <polyline points="10 9 9 9 8 9" />
              </svg>
            </span>
            <h3 className="analytics-kpi-label">Volumen Total</h3>
          </header>
          <div className="analytics-kpi-body">
            <span className="analytics-kpi-value" aria-label={`${total} incidencias registradas`}>
              {total}
            </span>
            <p className="analytics-kpi-subtext">
              <span className="analytics-kpi-pill kpi-pill-resolved">{resolved} resueltas</span>
              <span className="analytics-kpi-separator">·</span>
              <span className="analytics-kpi-pill kpi-pill-pending">{pending} pendientes</span>
            </p>
          </div>
          <footer className="analytics-kpi-footer">
            <span className="analytics-kpi-help">Incidencias computadas en el período activo</span>
          </footer>
        </article>

        {/* KPI 2: Tiempo Medio de Triaje (MTTT) */}
        <article className="analytics-kpi-card" data-testid="kpi-card-mttt">
          <header className="analytics-kpi-header">
            <span className="analytics-kpi-icon-wrap" aria-hidden="true">
              <svg
                viewBox="0 0 24 24"
                className="analytics-kpi-icon"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <circle cx="12" cy="12" r="10" />
                <polyline points="12 6 12 12 16 14" />
              </svg>
            </span>
            <h3 className="analytics-kpi-label">Tiempo Medio de Triaje (MTTT)</h3>
          </header>
          <div className="analytics-kpi-body">
            <span className="analytics-kpi-value" aria-label={`MTTT: ${mttt}`}>
              {mttt}
            </span>
            <p className="analytics-kpi-subtext">
              Validación y clasificación técnica inicial
            </p>
          </div>
          <footer className="analytics-kpi-footer">
            <span className="analytics-kpi-help">Desde el reporte ciudadano hasta la validación inicial</span>
          </footer>
        </article>

        {/* KPI 3: Tiempo Medio de Resolución (MTTR) */}
        <article className="analytics-kpi-card" data-testid="kpi-card-mttr">
          <header className="analytics-kpi-header">
            <span className="analytics-kpi-icon-wrap" aria-hidden="true">
              <svg
                viewBox="0 0 24 24"
                className="analytics-kpi-icon"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z" />
              </svg>
            </span>
            <h3 className="analytics-kpi-label">Tiempo Medio de Resolución (MTTR)</h3>
          </header>
          <div className="analytics-kpi-body">
            <span className="analytics-kpi-value" aria-label={`MTTR: ${mttr}`}>
              {mttr}
            </span>
            <p className="analytics-kpi-subtext">
              Cierre resolutivo por servicios operativos
            </p>
          </div>
          <footer className="analytics-kpi-footer">
            <span className="analytics-kpi-help">Desde la validación formal hasta la resolución definitiva</span>
          </footer>
        </article>

        {/* KPI 4: Tasa de Cumplimiento de SLA */}
        <article className={`analytics-kpi-card ${slaHealthClass}`} data-testid="kpi-card-sla">
          <header className="analytics-kpi-header">
            <span className="analytics-kpi-icon-wrap" aria-hidden="true">
              <svg
                viewBox="0 0 24 24"
                className="analytics-kpi-icon"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                <polyline points="9 12 11 14 15 10" />
              </svg>
            </span>
            <h3 className="analytics-kpi-label">Cumplimiento de SLA</h3>
          </header>
          <div className="analytics-kpi-body">
            <div className="analytics-kpi-metric-row">
              <span className="analytics-kpi-value" aria-label={`Cumplimiento de SLA: ${slaText}`}>
                {slaText}
              </span>
              <span className={`analytics-kpi-health-badge ${slaHealthClass}`}>
                {slaHealthLabel}
              </span>
            </div>
            <p className="analytics-kpi-subtext">
              <span className="analytics-kpi-pill kpi-pill-compliant">{slaCompliant} en plazo</span>
              <span className="analytics-kpi-separator">·</span>
              <span className="analytics-kpi-pill kpi-pill-breached">{slaBreached} fuera de plazo</span>
            </p>
          </div>
          <footer className="analytics-kpi-footer">
            <span className="analytics-kpi-help">Conforme a compromisos por nivel de prioridad</span>
          </footer>
        </article>

        {/* KPI 5: Desempeño Asistente Inteligente */}
        <article className="analytics-kpi-card analytics-kpi-card-assistant" data-testid="kpi-card-assistant">
          <header className="analytics-kpi-header">
            <span className="analytics-kpi-icon-wrap icon-wrap-sparkle" aria-hidden="true">
              <svg
                viewBox="0 0 24 24"
                className="analytics-kpi-icon"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83" />
              </svg>
            </span>
            <h3 className="analytics-kpi-label">Asistente Inteligente</h3>
          </header>
          <div className="analytics-kpi-body">
            <div className="analytics-kpi-metric-row">
              <span className="analytics-kpi-value" aria-label={`Tasa de adopción: ${adoptionText}`}>
                {adoptionText}
              </span>
              <span className="analytics-kpi-badge-caption">adopción</span>
            </div>
            <p className="analytics-kpi-subtext">
              Confianza promedio: <strong>{avgConfidenceText}</strong>
            </p>
          </div>
          <footer className="analytics-kpi-footer">
            <span className="analytics-kpi-help">
              {acceptedRecommendations} aceptadas de {totalRecommendations} sugeridas
            </span>
          </footer>
        </article>
      </div>
    </section>
  );
}

