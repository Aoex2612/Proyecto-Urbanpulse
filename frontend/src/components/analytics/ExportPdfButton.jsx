/**
 * @fileoverview Botón interactivo de exportación de informes analíticos en formato PDF (RF-12, RF-13, RF-14).
 * Gestiona el bloqueo reactivo ante la ausencia de incidencias registradas, el feedback visual
 * mediante spinner durante la compilación del documento y la accesibilidad según directrices ARIA.
 */

import { useState } from 'react';
import { generateAnalyticsPdf } from '../../utils/reportUtils';
import './ExportPdfButton.css';

/**
 * Componente de acción para la descarga del informe ejecutivo en PDF de analítica urbana.
 *
 * @param {Object} props - Propiedades del componente.
 * @param {Object} [props.summaryData] - Resumen analítico consolidado (AnalyticsSummaryResponse).
 * @param {Object} [props.currentUser] - Usuario autenticado que solicita la exportación.
 * @param {boolean} [props.disabled=false] - Forzado externo de inhabilitación.
 * @param {string} [props.className=''] - Clases CSS adicionales para personalización.
 * @param {Object} [props.pdfOptions={}] - Opciones de configuración para el generador de PDF (ej. filename).
 * @param {Function} [props.onSuccess] - Callback ejecutado tras generar exitosamente el informe.
 * @param {Function} [props.onError] - Callback invocado en caso de excepción durante la generación.
 * @returns {import('react').JSX.Element} Botón accesible de exportación con control de estado y feedback.
 */
export default function ExportPdfButton({
  summaryData = null,
  currentUser = null,
  disabled = false,
  className = '',
  pdfOptions = {},
  onSuccess = null,
  onError = null,
}) {
  const [isGenerating, setIsGenerating] = useState(false);
  const [errorMessage, setErrorMessage] = useState(null);

  const hasIncidents = Boolean(
    summaryData &&
      Array.isArray(summaryData.filteredIncidents) &&
      summaryData.filteredIncidents.length > 0
  );

  const isButtonDisabled = disabled || isGenerating || !hasIncidents;

  const tooltipTitle = !hasIncidents
    ? 'Se requieren datos para exportar el informe en PDF'
    : 'Exportar informe en formato PDF';

  const handleClick = async () => {
    if (isButtonDisabled) return;

    setIsGenerating(true);
    setErrorMessage(null);

    try {
      // Breve pausa asíncrona para permitir la actualización reactiva del DOM con el estado "Generando..."
      await new Promise((resolve) => setTimeout(resolve, 30));

      const doc = generateAnalyticsPdf(summaryData, currentUser, pdfOptions);

      if (typeof onSuccess === 'function') {
        onSuccess(doc);
      }
    } catch (err) {
      const msg = err?.message || 'Error inesperado al generar el informe en PDF.';
      setErrorMessage(msg);
      if (typeof onError === 'function') {
        onError(err);
      }
    } finally {
      setIsGenerating(false);
    }
  };

  return (
    <div className="export-pdf-wrapper">
      <button
        type="button"
        className={`btn-export-pdf ${className}`.trim()}
        onClick={handleClick}
        disabled={isButtonDisabled}
        title={tooltipTitle}
        aria-label={
          isGenerating
            ? 'Generando informe en PDF...'
            : 'Exportar informe ejecutivo en formato PDF'
        }
        aria-busy={isGenerating}
      >
        {isGenerating ? (
          <>
            <svg
              className="btn-export-pdf-icon btn-export-pdf-spinner"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2.5"
              strokeLinecap="round"
              strokeLinejoin="round"
              aria-hidden="true"
            >
              <circle cx="12" cy="12" r="10" strokeDasharray="32" strokeDashoffset="12" />
            </svg>
            <span>Generando PDF...</span>
          </>
        ) : (
          <>
            <svg
              className="btn-export-pdf-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
              aria-hidden="true"
            >
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
              <polyline points="14 2 14 8 20 8" />
              <line x1="12" y1="18" x2="12" y2="12" />
              <polyline points="9 15 12 18 15 15" />
            </svg>
            <span>Exportar Informe PDF</span>
          </>
        )}
      </button>

      {errorMessage && (
        <span className="export-pdf-error" role="alert">
          {errorMessage}
        </span>
      )}
    </div>
  );
}
