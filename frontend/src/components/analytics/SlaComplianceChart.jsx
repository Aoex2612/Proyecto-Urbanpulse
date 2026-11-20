/**
 * @fileoverview Gráfico SVG nativo interactivo para la visualización del cumplimiento de SLAs por departamento (RF-6, RF-10).
 * Representa la tasa porcentual (0% a 100%) por área técnica municipal con códigos de color institucional
 * (verde >= 80%, ámbar 50-79%, rojo < 50%), línea de meta institucional al 80% y accesibilidad ARIA completa.
 */

import { useState, useId } from 'react';
import { formatMetricPercent, formatMetricHours } from '../../utils/reportUtils';
import './SlaComplianceChart.css';

/**
 * Devuelve el código cromático SVG en función de la tasa porcentual de cumplimiento de SLA.
 *
 * @param {number|null|undefined} rate - Tasa porcentual (0 a 100).
 * @returns {string} Código de color hexadecimal.
 */
const getSlaBarColor = (rate) => {
  if (rate === null || rate === undefined || Number.isNaN(Number(rate))) {
    return '#94a3b8'; // Slate 400
  }
  const numeric = Number(rate);
  if (numeric >= 80) return '#10b981'; // Emerald 500
  if (numeric >= 50) return '#f59e0b'; // Amber 500
  return '#ef4444'; // Red 500
};

/**
 * Componente gráfico SVG nativo para el análisis del rendimiento y acuerdos SLA por departamento municipal.
 *
 * @param {Object} props - Propiedades del componente.
 * @param {Array<Object>} [props.data=[]] - Lista de métricas departamentales (DepartmentMetricResponse).
 * @param {string} [props.title='Cumplimiento de SLA por Departamento'] - Título del bloque visual.
 * @returns {import('react').JSX.Element} Visualización gráfica interactiva con SVG nativo.
 */
export default function SlaComplianceChart({
  data = [],
  title = 'Cumplimiento de SLA por Departamento',
}) {
  const [hoveredDept, setHoveredDept] = useState(null);
  const chartId = useId();

  const deptList = Array.isArray(data) ? data : [];
  const validDepartments = deptList.filter(
    (item) => item && typeof item.departmentName === 'string'
  );

  if (validDepartments.length === 0) {
    return (
      <div className="sla-chart-card" data-testid="sla-chart-empty">
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
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="8" x2="12" y2="12" />
            <line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
          <p>No hay métricas departamentales disponibles para el período actual.</p>
        </div>
      </div>
    );
  }

  // Dimensiones del canvas SVG
  const labelWidth = 170;
  const chartRightMargin = 65;
  const rowHeight = 40;
  const barHeight = 20;
  const svgWidth = 600;
  const svgHeight = Math.max(160, validDepartments.length * rowHeight + 26);
  const barAreaWidth = svgWidth - labelWidth - chartRightMargin;

  // Posición de la línea de meta institucional (80% SLA)
  const targetX = labelWidth + barAreaWidth * 0.8;

  return (
    <div className="sla-chart-card" data-testid="sla-chart-card">
      <header className="chart-card-header">
        <div className="chart-title-group">
          <h3 className="chart-card-title">{title}</h3>
          <span className="sla-target-legend" aria-hidden="true">
            <span className="sla-target-dot" /> Meta institucional: 80%
          </span>
        </div>
        <span className="chart-card-meta">
          {validDepartments.length} {validDepartments.length === 1 ? 'departamento' : 'departamentos'}
        </span>
      </header>

      <div className="chart-svg-container">
        <svg
          role="img"
          aria-label={`Gráfico de cumplimiento de SLA por departamento municipal: ${validDepartments.length} departamentos evaluados`}
          aria-describedby={`desc-${chartId}`}
          className="sla-chart-svg"
          viewBox={`0 0 ${svgWidth} ${svgHeight}`}
          preserveAspectRatio="xMidYMid meet"
        >
          <title>{title}</title>
          <desc id={`desc-${chartId}`}>
            Muestra el porcentaje de cumplimiento de SLA por cada departamento municipal y la meta institucional del 80%.
          </desc>

          {/* Línea base del eje Y */}
          <line
            x1={labelWidth}
            y1={10}
            x2={labelWidth}
            y2={svgHeight - 14}
            className="chart-axis-line"
            aria-hidden="true"
          />

          {/* Línea de referencia meta al 80% */}
          <g className="sla-target-guideline" aria-hidden="true">
            <line
              x1={targetX}
              y1={10}
              x2={targetX}
              y2={svgHeight - 14}
              className="sla-target-line"
            />
            <text
              x={targetX}
              y={svgHeight - 4}
              textAnchor="middle"
              className="sla-target-label"
            >
              80%
            </text>
          </g>

          {/* Filas por departamento */}
          <g className="chart-departments-group">
            {validDepartments.map((dept, index) => {
              const rate = Math.max(0, Math.min(100, dept.slaComplianceRate ?? 0));
              const barWidth = (rate / 100) * barAreaWidth;
              const yPos = index * rowHeight + 16;
              const barColor = getSlaBarColor(dept.slaComplianceRate);
              const isHovered = hoveredDept?.departmentName === dept.departmentName;

              return (
                <g
                  key={dept.departmentId ?? dept.departmentName}
                  className={`sla-chart-row ${isHovered ? 'is-active' : ''}`}
                  tabIndex="0"
                  role="graphics-symbol"
                  aria-label={`${dept.departmentName}: ${formatMetricPercent(rate)} cumplimiento de SLA`}
                  onMouseEnter={() => setHoveredDept(dept)}
                  onMouseLeave={() => setHoveredDept(null)}
                  onFocus={() => setHoveredDept(dept)}
                  onBlur={() => setHoveredDept(null)}
                >
                  {/* Nombre del departamento en el eje Y */}
                  <text
                    x={labelWidth - 10}
                    y={yPos + barHeight / 2 + 4}
                    textAnchor="end"
                    className="chart-axis-label dept-axis-label"
                  >
                    {dept.departmentName}
                  </text>

                  {/* Barra track (fondo 100%) */}
                  <rect
                    x={labelWidth}
                    y={yPos}
                    width={barAreaWidth}
                    height={barHeight}
                    rx="4"
                    className="chart-bar-track"
                    aria-hidden="true"
                  />

                  {/* Barra de progreso con color según salud */}
                  <rect
                    x={labelWidth}
                    y={yPos}
                    width={Math.max(3, barWidth)}
                    height={barHeight}
                    rx="4"
                    fill={barColor}
                    className="sla-bar-fill"
                    data-testid={`sla-bar-${dept.departmentName.toLowerCase().replace(/\s+/g, '-')}`}
                  />

                  {/* Etiqueta con el porcentaje */}
                  <text
                    x={labelWidth + Math.max(3, barWidth) + 8}
                    y={yPos + barHeight / 2 + 4}
                    textAnchor="start"
                    className="sla-percentage-label"
                    fill={barColor}
                  >
                    {formatMetricPercent(rate)}
                  </text>
                </g>
              );
            })}
          </g>
        </svg>

        {/* Tooltip interactivo flotante */}
        {hoveredDept && (
          <aside
            className="chart-tooltip-panel"
            aria-live="polite"
            role="status"
          >
            <div className="chart-tooltip-title">{hoveredDept.departmentName}</div>
            <div className="chart-tooltip-content">
              <div>
                <strong>Cumplimiento SLA:</strong>{' '}
                {formatMetricPercent(hoveredDept.slaComplianceRate)}
              </div>
              <div>
                <strong>Asignadas:</strong> {hoveredDept.totalAssigned ?? 0} ·{' '}
                <strong>Resueltas:</strong> {hoveredDept.resolvedCount ?? 0}
              </div>
              {hoveredDept.avgResolutionHours !== undefined &&
                hoveredDept.avgResolutionHours !== null && (
                  <div>
                    <strong>Tiempo medio:</strong>{' '}
                    {formatMetricHours(hoveredDept.avgResolutionHours)}
                  </div>
                )}
            </div>
          </aside>
        )}
      </div>
    </div>
  );
}

