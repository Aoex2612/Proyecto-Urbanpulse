import { describe, it, expect, vi } from 'vitest';
import reportUtils, {
  generateAnalyticsPdf,
  formatMetricHours,
  formatMetricPercent,
  formatFilterDescription,
  PRESET_LABELS,
} from './reportUtils';
import { jsPDF } from 'jspdf';

describe('reportUtils', () => {
  describe('PRESET_LABELS', () => {
    it('contiene las traducciones correctas en español para los presets temporales', () => {
      expect(PRESET_LABELS.TODAY).toBe('Hoy');
      expect(PRESET_LABELS.LAST_7_DAYS).toBe('Últimos 7 días');
      expect(PRESET_LABELS.LAST_30_DAYS).toBe('Últimos 30 días');
      expect(PRESET_LABELS.LAST_3_MONTHS).toBe('Últimos 3 meses');
      expect(PRESET_LABELS.LAST_6_MONTHS).toBe('Últimos 6 meses');
      expect(PRESET_LABELS.LAST_YEAR).toBe('Último año');
      expect(PRESET_LABELS.SPECIFIC_YEAR).toBe('Año específico');
    });
  });

  describe('formatMetricHours', () => {
    it('formatea números correctamente a un decimal con sufijo h', () => {
      expect(formatMetricHours(3.5)).toBe('3.5 h');
      expect(formatMetricHours(0)).toBe('0.0 h');
      expect(formatMetricHours(28.456)).toBe('28.5 h');
    });

    it('devuelve N/A ante valores nulos, indefinidos o NaN', () => {
      expect(formatMetricHours(null)).toBe('N/A');
      expect(formatMetricHours(undefined)).toBe('N/A');
      expect(formatMetricHours(NaN)).toBe('N/A');
      expect(formatMetricHours('invalido')).toBe('N/A');
    });
  });

  describe('formatMetricPercent', () => {
    it('formatea porcentajes correctamente a un decimal con sufijo %', () => {
      expect(formatMetricPercent(85.5)).toBe('85.5%');
      expect(formatMetricPercent(100)).toBe('100.0%');
      expect(formatMetricPercent(0)).toBe('0.0%');
    });

    it('devuelve 0.0% ante valores nulos, indefinidos o NaN', () => {
      expect(formatMetricPercent(null)).toBe('0.0%');
      expect(formatMetricPercent(undefined)).toBe('0.0%');
      expect(formatMetricPercent(NaN)).toBe('0.0%');
    });
  });

  describe('formatFilterDescription', () => {
    it('retorna valores por defecto cuando no se especifican filtros', () => {
      const result = formatFilterDescription();
      expect(result.periodText).toBe('Histórico completo');
      expect(result.districtText).toBe('Todos los distritos');
      expect(result.neighborhoodText).toBe('Todos los barrios');
    });

    it('traduce presets conocidos como LAST_30_DAYS', () => {
      const result = formatFilterDescription({ preset: 'LAST_30_DAYS', district: 'Centro' });
      expect(result.periodText).toBe('Últimos 30 días');
      expect(result.districtText).toBe('Centro');
      expect(result.neighborhoodText).toBe('Todos los barrios');
    });

    it('formatea SPECIFIC_YEAR con el año indicado', () => {
      const result = formatFilterDescription({ preset: 'SPECIFIC_YEAR', year: 2025 });
      expect(result.periodText).toBe('Año 2025');
    });

    it('formatea rango de fechas personalizado con startDate y endDate', () => {
      const result = formatFilterDescription({
        startDate: '2026-01-01',
        endDate: '2026-03-31',
        neighborhood: 'El Palo',
      });
      expect(result.periodText).toContain('01/01/2026');
      expect(result.periodText).toContain('31/03/2026');
      expect(result.neighborhoodText).toBe('El Palo');
    });
  });

  describe('generateAnalyticsPdf', () => {
    const mockSummaryData = {
      operationalMetrics: {
        totalIncidents: 2,
        resolvedIncidents: 1,
        pendingIncidents: 1,
        mtttHours: 3.5,
        mttrHours: 28.0,
        slaComplianceRate: 85.5,
        totalSlaCompliant: 1,
        totalSlaBreached: 1,
      },
      appliedFilters: {
        preset: 'LAST_30_DAYS',
        district: 'Centro',
        neighborhood: 'Soho',
      },
      filteredIncidents: [
        {
          id: 101,
          title: 'Farola rota en Alameda Principal',
          category: 'ALUMBRADO',
          district: 'Centro',
          neighborhood: 'Soho',
          priority: 'HIGH',
          status: 'RESOLVED',
          createdAt: '2026-10-15T10:00:00Z',
        },
        {
          id: 102,
          title: 'Bache profundo en calzada',
          category: 'VIALIDAD',
          district: 'Centro',
          neighborhood: 'Soho',
          priority: 'MEDIUM',
          status: 'IN_PROGRESS',
          createdAt: '2026-10-16T12:30:00Z',
        },
      ],
    };

    const mockUser = {
      fullName: 'Carlos Analista',
      email: 'carlos@urbanpulse.es',
      role: 'ANALYST',
    };

    it('lanza un error explicativo si summaryData es nulo (RF-14)', () => {
      expect(() => generateAnalyticsPdf(null, mockUser)).toThrow(
        'No hay incidencias disponibles para generar el informe PDF'
      );
    });

    it('lanza un error explicativo si filteredIncidents está vacío (RF-14)', () => {
      expect(() => generateAnalyticsPdf({ filteredIncidents: [] }, mockUser)).toThrow(
        'No hay incidencias disponibles para generar el informe PDF'
      );
    });

    it('genera y retorna la instancia del documento PDF cuando returnDoc es true', () => {
      const doc = generateAnalyticsPdf(mockSummaryData, mockUser, { returnDoc: true });

      expect(doc).toBeDefined();
      expect(doc.internal.getNumberOfPages()).toBeGreaterThanOrEqual(1);
    });

    it('soporta la generación con usuario por defecto y métricas nulas', () => {
      const dataWithoutMetrics = {
        filteredIncidents: [
          {
            id: 1,
            title: 'Sin datos',
            district: null,
            neighborhood: null,
          },
        ],
      };

      const doc = generateAnalyticsPdf(dataWithoutMetrics, null, { returnDoc: true });

      expect(doc).toBeDefined();
      expect(doc.internal.getNumberOfPages()).toBe(1);
    });

    it('genera múltiples páginas cuando existe un volumen amplio de incidencias', () => {
      const largeList = Array.from({ length: 65 }, (_, index) => ({
        id: index + 1,
        title: `Incidencia urbana de prueba #${index + 1}`,
        category: 'PARQUES',
        district: 'Carretera de Cádiz',
        neighborhood: 'Huelin',
        priority: 'LOW',
        status: 'VALIDATED',
        createdAt: '2026-10-20T08:00:00Z',
      }));

      const largeSummary = {
        ...mockSummaryData,
        filteredIncidents: largeList,
      };

      const doc = generateAnalyticsPdf(largeSummary, mockUser, { returnDoc: true });
      expect(doc.internal.getNumberOfPages()).toBeGreaterThan(1);
    });

    it('invoca doc.save con el nombre de archivo configurado cuando returnDoc no es true', () => {
      const customDoc = new jsPDF();
      const saveSpy = vi.spyOn(customDoc, 'save').mockImplementation(() => {});

      generateAnalyticsPdf(mockSummaryData, mockUser, {
        doc: customDoc,
        filename: 'informe-personalizado.pdf',
      });

      expect(saveSpy).toHaveBeenCalledWith('informe-personalizado.pdf');
    });

    it('invoca doc.save con nombre por defecto con fecha actual cuando no se aporta filename', () => {
      const customDoc = new jsPDF();
      const saveSpy = vi.spyOn(customDoc, 'save').mockImplementation(() => {});

      generateAnalyticsPdf(mockSummaryData, mockUser, { doc: customDoc });

      expect(saveSpy).toHaveBeenCalledTimes(1);
      const callArg = saveSpy.mock.calls[0][0];
      expect(callArg).toMatch(/^informe-analitica-urbanpulse-\d{4}-\d{2}-\d{2}\.pdf$/);
    });

    it('ejecuta el guardado directo de archivo sin errores de entorno', () => {
      const customDoc = new jsPDF();
      const saveSpy = vi.spyOn(customDoc, 'save').mockImplementation(() => {});
      const doc = generateAnalyticsPdf(mockSummaryData, mockUser, { doc: customDoc });
      expect(doc).toBeDefined();
      expect(doc.internal.getNumberOfPages()).toBeGreaterThanOrEqual(1);
      expect(saveSpy).toHaveBeenCalledTimes(1);
    });
  });

  describe('reportUtils default export', () => {
    it('contiene todas las funciones y constantes utilitarias', () => {
      expect(reportUtils.generateAnalyticsPdf).toBe(generateAnalyticsPdf);
      expect(reportUtils.formatMetricHours).toBe(formatMetricHours);
      expect(reportUtils.formatMetricPercent).toBe(formatMetricPercent);
      expect(reportUtils.formatFilterDescription).toBe(formatFilterDescription);
      expect(reportUtils.PRESET_LABELS).toBe(PRESET_LABELS);
    });
  });
});
