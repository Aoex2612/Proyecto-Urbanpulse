/**
 * @fileoverview Módulo utilitario para la generación y exportación de informes ejecutivos en formato PDF
 * de analítica urbana en UrbanPulse (RF-12, RF-13, RF-14).
 * Utiliza jsPDF y el complemento jspdf-autotable para renderizar documentos vectoriales estructurados
 * con cabecera oficial municipal, resumen de KPIs operacionales y tabla detallada de incidencias filtradas.
 */

import { jsPDF } from 'jspdf';
import autoTable from 'jspdf-autotable';
import { formatDate } from './dateUtils';

/**
 * Diccionario de traducción de presets temporales a etiquetas legibles en español.
 */
export const PRESET_LABELS = Object.freeze({
  TODAY: 'Hoy',
  LAST_7_DAYS: 'Últimos 7 días',
  LAST_30_DAYS: 'Últimos 30 días',
  LAST_3_MONTHS: 'Últimos 3 meses',
  LAST_6_MONTHS: 'Últimos 6 meses',
  LAST_YEAR: 'Último año',
  SPECIFIC_YEAR: 'Año específico',
});

/**
 * Formatea un valor en horas con un decimal y sufijo 'h'.
 *
 * @param {number|null|undefined} hours - Valor numérico en horas.
 * @returns {string} Cadena formateada (ej. "4.2 h") o "N/A" si es nulo.
 */
export const formatMetricHours = (hours) => {
  if (hours === null || hours === undefined || Number.isNaN(Number(hours))) {
    return 'N/A';
  }
  return `${Number(hours).toFixed(1)} h`;
};

/**
 * Formatea una tasa porcentual con un decimal y símbolo '%'.
 *
 * @param {number|null|undefined} percentage - Valor porcentual entre 0 y 100.
 * @returns {string} Cadena formateada (ej. "88.5%").
 */
export const formatMetricPercent = (percentage) => {
  if (percentage === null || percentage === undefined || Number.isNaN(Number(percentage))) {
    return '0.0%';
  }
  return `${Number(percentage).toFixed(1)}%`;
};

/**
 * Compone una descripción legible de los criterios de filtrado aplicados en la consulta analítica.
 *
 * @param {Object} [filters={}] - Objeto de filtros aplicados.
 * @returns {{periodText: string, districtText: string, neighborhoodText: string}} Textos descriptivos de los filtros.
 */
export const formatFilterDescription = (filters = {}) => {
  let periodText = 'Histórico completo';

  if (filters.preset && PRESET_LABELS[filters.preset]) {
    periodText = PRESET_LABELS[filters.preset];
    if (filters.preset === 'SPECIFIC_YEAR' && filters.year) {
      periodText = `Año ${filters.year}`;
    }
  } else if (filters.startDate || filters.endDate) {
    const start = filters.startDate ? formatDate(filters.startDate, false) : 'Inicio';
    const end = filters.endDate ? formatDate(filters.endDate, false) : 'Actualidad';
    periodText = `${start} — ${end}`;
  } else if (filters.year) {
    periodText = `Año ${filters.year}`;
  }

  const districtText = filters.district ? String(filters.district) : 'Todos los distritos';
  const neighborhoodText = filters.neighborhood ? String(filters.neighborhood) : 'Todos los barrios';

  return { periodText, districtText, neighborhoodText };
};

/**
 * Genera y descarga (o retorna) un informe ejecutivo estructurado en PDF a partir de los datos analíticos.
 *
 * @param {Object} summaryData - DTO AnalyticsSummaryResponse con métricas y lista de incidencias filtradas.
 * @param {Object} [currentUser={}] - Usuario emisor del informe autenticado en la sesión.
 * @param {Object} [options={}] - Opciones adicionales de generación.
 * @param {boolean} [options.returnDoc=false] - Si es true, retorna la instancia de jsPDF sin invocar doc.save().
 * @param {string} [options.filename] - Nombre del archivo PDF a descargar (por defecto con fecha actual).
 * @returns {jsPDF} Instancia del documento PDF generado.
 * @throws {Error} Si no existen incidencias en summaryData (RF-14).
 */
export const generateAnalyticsPdf = (summaryData, currentUser = {}, options = {}) => {
  if (
    !summaryData ||
    !Array.isArray(summaryData.filteredIncidents) ||
    summaryData.filteredIncidents.length === 0
  ) {
    throw new Error('No hay incidencias disponibles para generar el informe PDF');
  }

  const incidents = summaryData.filteredIncidents;
  const metrics = summaryData.operationalMetrics || {};
  const filters = summaryData.appliedFilters || {};

  // Creación del documento en formato vertical (Portrait) A4 en milímetros o uso de instancia inyectada
  const doc =
    options.doc ||
    new jsPDF({
      orientation: 'portrait',
      unit: 'mm',
      format: 'a4',
    });

  const pageWidth = doc.internal.pageSize.getWidth(); // 210 mm
  const marginX = 14;
  const contentWidth = pageWidth - marginX * 2; // 182 mm

  // ==========================================
  // 1. CABECERA INSTITUCIONAL OFICIAL
  // ==========================================
  doc.setFillColor(15, 23, 42); // #0F172A - Slate 900
  doc.rect(marginX, 12, contentWidth, 20, 'F');

  // Título institucional
  doc.setTextColor(255, 255, 255);
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(13);
  doc.text('UrbanPulse — Informe Ejecutivo de Analítica Urbana', marginX + 4, 21);

  // Subtítulo municipal
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(8.5);
  doc.setTextColor(203, 213, 225); // #CBD5E1 - Slate 300
  doc.text(
    'Ayuntamiento de Málaga · Plataforma de Gestión Urbana Inteligente',
    marginX + 4,
    27
  );

  // ==========================================
  // 2. METADATOS Y FILTROS DEL INFORME
  // ==========================================
  const now = new Date();
  const generationDateText = formatDate(now, true);
  const userName = currentUser?.fullName || currentUser?.email || 'Usuario del Sistema';
  const userRole = currentUser?.role ? ` (${currentUser.role})` : '';
  const filterSummary = formatFilterDescription(filters);

  doc.setFillColor(248, 250, 252); // #F8FAFC
  doc.setDrawColor(226, 232, 240); // #E2E8F0
  doc.rect(marginX, 36, contentWidth, 24, 'FD');

  doc.setTextColor(51, 65, 85); // #334155
  doc.setFontSize(8.5);

  // Columna izquierda de metadatos
  doc.setFont('helvetica', 'bold');
  doc.text('Emisión:', marginX + 4, 42);
  doc.setFont('helvetica', 'normal');
  doc.text(generationDateText, marginX + 22, 42);

  doc.setFont('helvetica', 'bold');
  doc.text('Generado por:', marginX + 4, 48);
  doc.setFont('helvetica', 'normal');
  doc.text(`${userName}${userRole}`, marginX + 28, 48);

  doc.setFont('helvetica', 'bold');
  doc.text('Período:', marginX + 4, 54);
  doc.setFont('helvetica', 'normal');
  doc.text(filterSummary.periodText, marginX + 22, 54);

  // Columna derecha de metadatos
  const colRightX = marginX + 96;
  doc.setFont('helvetica', 'bold');
  doc.text('Distrito:', colRightX, 42);
  doc.setFont('helvetica', 'normal');
  doc.text(filterSummary.districtText, colRightX + 18, 42);

  doc.setFont('helvetica', 'bold');
  doc.text('Barrio:', colRightX, 48);
  doc.setFont('helvetica', 'normal');
  doc.text(filterSummary.neighborhoodText, colRightX + 16, 48);

  doc.setFont('helvetica', 'bold');
  doc.text('Total registros:', colRightX, 54);
  doc.setFont('helvetica', 'normal');
  doc.text(`${incidents.length} incidencias`, colRightX + 26, 54);

  // ==========================================
  // 3. RESUMEN EJECUTIVO DE KPIS (TABLA)
  // ==========================================
  const totalIncidents = metrics.totalIncidents ?? incidents.length;
  const resolved = metrics.resolvedIncidents ?? 0;
  const pending = metrics.pendingIncidents ?? Math.max(0, totalIncidents - resolved);
  const mttt = formatMetricHours(metrics.mtttHours);
  const mttr = formatMetricHours(metrics.mttrHours);
  const slaCompliance = formatMetricPercent(metrics.slaComplianceRate);
  const slaCompliant = metrics.totalSlaCompliant ?? 0;
  const slaBreached = metrics.totalSlaBreached ?? 0;

  autoTable(doc, {
    startY: 64,
    theme: 'grid',
    head: [['Indicador Operativo / KPI', 'Valor Global', 'Desglose y Detalle Operativo']],
    body: [
      [
        'Volumen de Incidencias',
        `${totalIncidents}`,
        `Resueltas: ${resolved} | Pendientes: ${pending}`,
      ],
      [
        'Tiempo Medio de Triaje (MTTT)',
        mttt,
        'Tiempo transcurrido desde el reporte hasta la validación/rechazo inicial',
      ],
      [
        'Tiempo Medio de Resolución (MTTR)',
        mttr,
        'Tiempo promedio desde la validación hasta el cierre resolutivo',
      ],
      [
        'Tasa de Cumplimiento de SLA',
        slaCompliance,
        `En plazo: ${slaCompliant} | Fuera de plazo: ${slaBreached}`,
      ],
    ],
    styles: {
      font: 'helvetica',
      fontSize: 8,
      cellPadding: 2.2,
      textColor: [30, 41, 59],
      lineColor: [226, 232, 240],
      lineWidth: 0.2,
    },
    headStyles: {
      fillColor: [30, 41, 59], // #1E293B
      textColor: [255, 255, 255],
      fontStyle: 'bold',
      halign: 'left',
    },
    columnStyles: {
      0: { fontStyle: 'bold', cellWidth: 55 },
      1: { fontStyle: 'bold', halign: 'center', cellWidth: 30 },
      2: { cellWidth: 'auto' },
    },
    margin: { left: marginX, right: marginX },
  });

  // ==========================================
  // 4. TABLA DETALLADA DE INCIDENCIAS FILTRADAS
  // ==========================================
  const tableRows = incidents.map((inc) => [
    inc.id != null ? `#${inc.id}` : '—',
    inc.title || 'Sin título',
    inc.category || 'General',
    inc.district || 'Sin distrito especificado',
    inc.neighborhood || 'Sin barrio',
    inc.priority || '—',
    inc.status || '—',
    inc.createdAt ? formatDate(inc.createdAt, false) : '—',
  ]);

  const afterKpiY = doc.lastAutoTable ? doc.lastAutoTable.finalY + 6 : 95;

  autoTable(doc, {
    startY: afterKpiY,
    theme: 'striped',
    head: [['ID', 'Título', 'Categoría', 'Distrito', 'Barrio', 'Prioridad', 'Estado', 'Fecha']],
    body: tableRows,
    styles: {
      font: 'helvetica',
      fontSize: 7.2,
      cellPadding: 1.8,
      overflow: 'linebreak',
      textColor: [30, 41, 59],
      lineColor: [226, 232, 240],
      lineWidth: 0.15,
    },
    headStyles: {
      fillColor: [15, 23, 42], // #0F172A
      textColor: [255, 255, 255],
      fontStyle: 'bold',
      halign: 'center',
    },
    alternateRowStyles: {
      fillColor: [248, 250, 252], // #F8FAFC
    },
    columnStyles: {
      0: { cellWidth: 12, halign: 'center', fontStyle: 'bold' },
      1: { cellWidth: 42 },
      2: { cellWidth: 26 },
      3: { cellWidth: 25 },
      4: { cellWidth: 23 },
      5: { cellWidth: 18, halign: 'center' },
      6: { cellWidth: 18, halign: 'center' },
      7: { cellWidth: 18, halign: 'center' },
    },
    margin: { left: marginX, right: marginX, bottom: 18 },
  });

  // ==========================================
  // 5. PIE DE PÁGINA EN TODAS LAS PÁGINAS
  // ==========================================
  const totalPages = doc.internal.getNumberOfPages();
  const pageHeight = doc.internal.pageSize.getHeight();

  for (let pageNum = 1; pageNum <= totalPages; pageNum += 1) {
    doc.setPage(pageNum);

    // Línea separadora tenue de pie
    doc.setDrawColor(226, 232, 240);
    doc.setLineWidth(0.3);
    doc.line(marginX, pageHeight - 12, pageWidth - marginX, pageHeight - 12);

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(7.5);
    doc.setTextColor(100, 116, 139); // #64748B - Slate 500

    // Nota de confidencialidad a la izquierda
    doc.text(
      'UrbanPulse © 2026 · Ayuntamiento de Málaga · Documento Oficial de Uso Interno',
      marginX,
      pageHeight - 8
    );

    // Numeración a la derecha
    doc.text(
      `Página ${pageNum} de ${totalPages}`,
      pageWidth - marginX,
      pageHeight - 8,
      { align: 'right' }
    );
  }

  // ==========================================
  // 6. RETORNO O GUARDADO EN DISCO
  // ==========================================
  if (options.returnDoc) {
    return doc;
  }

  const todayIso = new Date().toISOString().split('T')[0];
  const finalFilename = options.filename || `informe-analitica-urbanpulse-${todayIso}.pdf`;
  doc.save(finalFilename);

  return doc;
};

const reportUtils = {
  PRESET_LABELS,
  formatMetricHours,
  formatMetricPercent,
  formatFilterDescription,
  generateAnalyticsPdf,
};

export default reportUtils;
