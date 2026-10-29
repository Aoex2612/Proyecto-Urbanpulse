import { describe, it, expect } from 'vitest';
import {
  formatDate,
  formatDateTime,
  isWithinDateRange,
  filterIncidentsByDate,
  DATE_FILTER_PRESETS,
} from './dateUtils';

describe('dateUtils', () => {
  describe('formatDate', () => {
    it('formatea correctamente una fecha ISO incluyendo hora por defecto', () => {
      const isoString = '2026-03-15T10:30:00Z';
      const formatted = formatDate(isoString);
      expect(formatted).toBeDefined();
      expect(formatted).toContain('2026');
      expect(formatted).toContain('15');
    });

    it('formatea fecha sin hora cuando includeTime es false', () => {
      const isoString = '2026-03-15T10:30:00Z';
      const formatted = formatDate(isoString, false);
      expect(formatted).toBeDefined();
      expect(formatted).toContain('2026');
      expect(formatted).toContain('15');
      expect(formatted).not.toContain(':');
    });

    it('retorna texto informativo cuando la fecha es nula, vacia o no valida', () => {
      expect(formatDate(null)).toBe('Fecha no disponible');
      expect(formatDate('')).toBe('Fecha no disponible');
      expect(formatDate(undefined)).toBe('Fecha no disponible');
      expect(formatDate('invalida')).toBe('invalida');
    });
  });

  describe('formatDateTime', () => {
    it('formatea correctamente una fecha ISO a formato local legible en espanol', () => {
      const isoString = '2026-03-15T10:30:00Z';
      const formatted = formatDateTime(isoString);
      expect(formatted).toBeDefined();
      expect(formatted).toContain('2026');
      expect(formatted).toContain('15');
    });

    it('retorna texto informativo cuando la fecha es nula, vacia o no valida', () => {
      expect(formatDateTime(null)).toBe('Fecha no disponible');
      expect(formatDateTime('')).toBe('Fecha no disponible');
      expect(formatDateTime(undefined)).toBe('Fecha no disponible');
    });
  });

  describe('isWithinDateRange', () => {
    it('valida correctamente fechas dentro del rango', () => {
      expect(isWithinDateRange('2026-10-15T12:00:00Z', '2026-10-10', '2026-10-20')).toBe(true);
      expect(isWithinDateRange('2026-10-10T00:01:00Z', '2026-10-10', '2026-10-20')).toBe(true);
      expect(isWithinDateRange('2026-10-20T23:59:00Z', '2026-10-10', '2026-10-20')).toBe(true);
    });

    it('rechaza fechas fuera del rango', () => {
      expect(isWithinDateRange('2026-10-05T12:00:00Z', '2026-10-10', '2026-10-20')).toBe(false);
      expect(isWithinDateRange('2026-10-25T12:00:00Z', '2026-10-10', '2026-10-20')).toBe(false);
    });

    it('admite limites abiertos si startDate o endDate son nulos', () => {
      expect(isWithinDateRange('2026-10-15T12:00:00Z', '2026-10-10', null)).toBe(true);
      expect(isWithinDateRange('2026-10-05T12:00:00Z', '2026-10-10', null)).toBe(false);
      expect(isWithinDateRange('2026-10-15T12:00:00Z', null, '2026-10-20')).toBe(true);
      expect(isWithinDateRange('2026-10-25T12:00:00Z', null, '2026-10-20')).toBe(false);
    });

    it('retorna false si targetDate es invalida', () => {
      expect(isWithinDateRange(null, '2026-10-10', '2026-10-20')).toBe(false);
      expect(isWithinDateRange('invalida', '2026-10-10', '2026-10-20')).toBe(false);
    });
  });

  describe('filterIncidentsByDate', () => {
    const referenceDate = new Date('2026-10-27T12:00:00Z');

    const sampleIncidents = [
      { id: 1, title: 'Hoy temprano', createdAt: '2026-10-27T08:00:00Z' },
      { id: 2, title: 'Hace 3 dias', createdAt: '2026-10-24T10:00:00Z' },
      { id: 3, title: 'Hace 6 dias', createdAt: '2026-10-21T15:00:00Z' },
      { id: 4, title: 'Hace 15 dias', createdAt: '2026-10-12T12:00:00Z' },
      { id: 5, title: 'Hace 45 dias', createdAt: '2026-09-12T12:00:00Z' },
      { id: 6, title: 'Sin fecha', createdAt: null },
    ];

    it('preset ALL retorna todas las incidencias', () => {
      const result = filterIncidentsByDate(
        sampleIncidents,
        { preset: DATE_FILTER_PRESETS.ALL },
        referenceDate
      );
      expect(result).toHaveLength(6);
    });

    it('preset TODAY solo incluye las del dia de hoy', () => {
      const result = filterIncidentsByDate(
        sampleIncidents,
        { preset: DATE_FILTER_PRESETS.TODAY },
        referenceDate
      );
      expect(result).toHaveLength(1);
      expect(result[0].id).toBe(1);
    });

    it('preset WEEK incluye las de los ultimos 7 dias', () => {
      const result = filterIncidentsByDate(
        sampleIncidents,
        { preset: DATE_FILTER_PRESETS.WEEK },
        referenceDate
      );
      expect(result).toHaveLength(3); // id 1, 2, 3
      expect(result.map((i) => i.id)).toEqual([1, 2, 3]);
    });

    it('preset MONTH incluye las de los ultimos 30 dias', () => {
      const result = filterIncidentsByDate(
        sampleIncidents,
        { preset: DATE_FILTER_PRESETS.MONTH },
        referenceDate
      );
      expect(result).toHaveLength(4); // id 1, 2, 3, 4
      expect(result.map((i) => i.id)).toEqual([1, 2, 3, 4]);
    });

    it('preset CUSTOM filtra correctamente entre startDate y endDate', () => {
      const result = filterIncidentsByDate(
        sampleIncidents,
        {
          preset: DATE_FILTER_PRESETS.CUSTOM,
          startDate: '2026-10-20',
          endDate: '2026-10-25',
        },
        referenceDate
      );
      expect(result).toHaveLength(2); // id 2 (24 oct) y id 3 (21 oct)
      expect(result.map((i) => i.id)).toEqual([2, 3]);
    });

    it('preset CUSTOM sin fechas fijadas retorna todas las incidencias', () => {
      const result = filterIncidentsByDate(
        sampleIncidents,
        {
          preset: DATE_FILTER_PRESETS.CUSTOM,
          startDate: '',
          endDate: '',
        },
        referenceDate
      );
      expect(result).toHaveLength(6);
    });
  });
});
