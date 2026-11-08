/**
 * @fileoverview Gráfico SVG nativo interactivo para la distribución territorial de incidencias (RF-7, RF-10).
 * Renderiza barras horizontales proporcionales con etiquetas de distrito, conteo de incidencias,
 * accesibilidad ARIA completa (role="img", title, desc) e interactividad con foco y tooltips informativos.
 */

import { useState, useId } from 'react';
import { formatMetricPercent } from '../../utils/reportUtils';
import './TerritorialChart.css';

/**
 * Componente gráfico SVG nativo para representar la carga de incidencias por territorio municipal.
 *
 * @param {Object} props - Propiedades del componente.
 * @param {Array<Object>} [props.data=[]] - Lista de métricas territoriales (TerritorialMetricResponse).
 * @param {string} [props.title='Distribución Territorial de Incidencias'] - Título del bloque gráfico.
 * @returns {import('react').JSX.Element} Visualización SVG interactiva y accesible.
 */
export default function TerritorialChart({
  data = [],
  title = 'Distribución Territorial de Incidencias',
}) {
  const [hoveredItem, setHoveredItem] = useState(null);
  const chartId = useId();

  const metricsList = Array.isArray(data) ? data : [];
  const validMetrics = metricsList.filter(
    (item) => item && typeof item.territoryName === 'string' && (item.totalIncidents ?? 0) >= 0
  );

  const maxIncidents = Math.max(1, ...validMetrics.map((item) => item.totalIncidents || 0));

  if (validMetrics.length === 0 || maxIncidents === 0) {
    return (
      <div className="territorial-chart-card" data-testid="territorial-chart-empty">
        <h3 className="chart-card-title">{title}</h3>
        <div className="chart-empty-placeholder">
          <svg
            viewBox="0 0 24 24"
            className="chart-empty-icon"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.5"
            aria-hidden="true"
          >
            <polygon points="1 6 1 22 8 18 16 22 23 18 23 2 16 6 8 2 1 6" />
            <line x1="8" y1="2" x2="8" y2="18" />
            <line x1="16" y1="6" x2="16" y2="22" />
          </svg>
          <p>No hay datos territoriales disponibles para el período seleccionado.</p>
        </div>
      </div>
    );
  }

  // Dimensiones del canvas SVG
  const labelWidth = 140;
  const chartRightMargin = 60;
  const rowHeight = 36;
  const barHeight = 18;
  const svgWidth = 600;
  const svgHeight = Math.max(160, validMetrics.length * rowHeight + 20);
  const barAreaWidth = svgWidth - labelWidth - chartRightMargin;

  return (
    <div className="territorial-chart-card" data-testid="territorial-chart-card">
      <header className="chart-card-header">
        <h3 className="chart-card-title">{title}</h3>
        <span className="chart-card-meta">
          {validMetrics.length} {validMetrics.length === 1 ? 'territorio' : 'territorios'} analizados
        </span>
      </header>

      <div className="chart-svg-container">
        <svg
          role="img"
          aria-label={`Gráfico de distribución territorial de incidencias: ${validMetrics.length} demarcaciones analizadas`}
          aria-describedby={`desc-${chartId}`}
          className="territorial-chart-svg"
          viewBox={`0 0 ${svgWidth} ${svgHeight}`}
          preserveAspectRatio="xMidYMid meet"
        >
          <title>{title}</title>
          <desc id={`desc-${chartId}`}>
            Gráfico de barras que compara el volumen de incidencias por demarcación territorial de Málaga.
          </desc>

          {/* Líneas guía sutiles */}
          <g className="chart-grid-lines" aria-hidden="true">
            <line
              x1={labelWidth}
              y1={10}
              x2={labelWidth}
              y2={svgHeight - 10}
              className="chart-axis-line"
            />
            <line
              x1={labelWidth + barAreaWidth * 0.5}
              y1={10}
              x2={labelWidth + barAreaWidth * 0.5}
              y2={svgHeight - 10}
              className="chart-grid-line"
            />
            <line
              x1={labelWidth + barAreaWidth}
              y1={10}
              x2={labelWidth + barAreaWidth}
              y2={svgHeight - 10}
              className="chart-grid-line"
            />
          </g>

          {/* Barras por territorio */}
          <g className="chart-bars-group">
            {validMetrics.map((item, index) => {
              const count = item.totalIncidents ?? 0;
              const barWidth = maxIncidents > 0 ? (count / maxIncidents) * barAreaWidth : 0;
              const yPos = index * rowHeight + 14;
              const isHovered = hoveredItem?.territoryName === item.territoryName;

              return (
                <g
                  key={item.territoryName}
                  className={`chart-bar-row ${isHovered ? 'is-active' : ''}`}
                  tabIndex="0"
                  role="graphics-symbol"
                  aria-label={`${item.territoryName}: ${count} incidencias`}
                  onMouseEnter={() => setHoveredItem(item)}
                  onMouseLeave={() => setHoveredItem(null)}
                  onFocus={() => setHoveredItem(item)}
                  onBlur={() => setHoveredItem(null)}
                >
                  {/* Etiqueta del territorio en el eje Y */}
                  <text
                    x={labelWidth - 10}
                    y={yPos + barHeight / 2 + 4}
                    textAnchor="end"
                    className="chart-axis-label"
                  >
                    {item.territoryName}
                  </text>

                  {/* Barra de fondo / track */}
                  <rect
                    x={labelWidth}
                    y={yPos}
                    width={barAreaWidth}
                    height={barHeight}
                    rx="4"
                    className="chart-bar-track"
                    aria-hidden="true"
                  />

                  {/* Barra de datos */}
                  <rect
                    x={labelWidth}
                    y={yPos}
                    width={Math.max(2, barWidth)}
                    height={barHeight}
                    rx="4"
                    className="chart-bar-fill"
                    data-testid={`territorial-bar-${item.territoryName.toLowerCase().replace(/\s+/g, '-')}`}
                  />

                  {/* Etiqueta con el valor numérico */}
                  <text
                    x={labelWidth + Math.max(2, barWidth) + 8}
                    y={yPos + barHeight / 2 + 4}
                    textAnchor="start"
                    className="chart-bar-value"
                  >
                    {count}
                  </text>
                </g>
              );
            })}
          </g>
        </svg>

        {/* Panel / Tooltip interactivo contextual */}
        {hoveredItem && (
          <aside
            className="chart-tooltip-panel"
            aria-live="polite"
            role="status"
          >
            <div className="chart-tooltip-title">{hoveredItem.territoryName}</div>
            <div className="chart-tooltip-content">
              <div>
                <strong>Total incidencias:</strong> {hoveredItem.totalIncidents ?? 0}
              </div>
              <div>
                <strong>Resueltas:</strong> {hoveredItem.resolvedCount ?? 0} ·{' '}
                <strong>Pendientes:</strong> {hoveredItem.pendingCount ?? 0}
              </div>
              {hoveredItem.slaComplianceRate !== undefined &&
                hoveredItem.slaComplianceRate !== null && (
                  <div>
                    <strong>Cumplimiento SLA:</strong>{' '}
                    {formatMetricPercent(hoveredItem.slaComplianceRate)}
                  </div>
                )}
            </div>
          </aside>
        )}
      </div>
    </div>
  );
}

