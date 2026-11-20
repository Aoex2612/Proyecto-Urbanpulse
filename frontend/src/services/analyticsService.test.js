import { describe, it, expect, vi, beforeEach } from 'vitest';
import analyticsService, {
  getAnalyticsSummary,
  getDepartmentRecommendation,
} from './analyticsService';
import api from './api';

vi.mock('./api', () => ({
  default: {
    get: vi.fn(),
  },
}));

describe('analyticsService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getAnalyticsSummary', () => {
    it('llama a GET /api/analytics/summary con parámetros vacíos por defecto y retorna los datos', async () => {
      const mockSummary = {
        operationalMetrics: {
          totalIncidents: 120,
          resolvedIncidents: 95,
          pendingIncidents: 25,
          mtttHours: 4.2,
          mttrHours: 26.5,
          slaComplianceRate: 88.5,
        },
        filteredIncidents: [{ id: 1, title: 'Bache en calzada' }],
      };
      api.get.mockResolvedValueOnce({ data: mockSummary });

      const result = await getAnalyticsSummary();

      expect(api.get).toHaveBeenCalledWith('/api/v2/analytics/summary', { params: {} });
      expect(result).toEqual(mockSummary);
    });

    it('llama a GET /api/v2/analytics/summary pasando los parámetros de filtrado personalizados', async () => {
      const filters = {
        preset: 'LAST_30_DAYS',
        district: 'Centro',
        neighborhood: 'Soho',
      };
      const mockSummary = {
        operationalMetrics: { totalIncidents: 15 },
        filteredIncidents: [{ id: 10, title: 'Luminaria rota' }],
      };
      api.get.mockResolvedValueOnce({ data: mockSummary });

      const result = await getAnalyticsSummary(filters);

      expect(api.get).toHaveBeenCalledWith('/api/v2/analytics/summary', { params: filters });
      expect(result).toEqual(mockSummary);
    });

    it('propaga el error si la petición HTTP falla', async () => {
      const mockError = new Error('Acceso no autorizado 403');
      api.get.mockRejectedValueOnce(mockError);

      await expect(getAnalyticsSummary()).rejects.toThrow('Acceso no autorizado 403');
      expect(api.get).toHaveBeenCalledTimes(1);
    });
  });

  describe('getDepartmentRecommendation', () => {
    it('llama a GET /api/v2/analytics/recommend-department con incidentId y retorna la sugerencia', async () => {
      const mockRecommendation = {
        suggestedDepartmentId: 3,
        suggestedDepartmentName: 'Alumbrado Público',
        confidencePercentage: 92.5,
        isLowConfidenceWarning: false,
        reasoning: 'Coincidencia léxica alta con alumbrado y farola.',
      };
      api.get.mockResolvedValueOnce({ data: mockRecommendation });

      const result = await getDepartmentRecommendation({ incidentId: 45 });

      expect(api.get).toHaveBeenCalledWith('/api/v2/analytics/recommend-department', {
        params: { incidentId: 45 },
      });
      expect(result).toEqual(mockRecommendation);
    });

    it('llama a GET /api/v2/analytics/recommend-department con título, descripción y categoría', async () => {
      const params = {
        title: 'Árbol caído en acera',
        description: 'Una rama de grandes dimensiones obstaculiza el paso peatonal',
        category: 'PARQUES_Y_JARDINES',
      };
      const mockRecommendation = {
        suggestedDepartmentId: 2,
        suggestedDepartmentName: 'Parques y Jardines',
        confidencePercentage: 85.0,
        isLowConfidenceWarning: false,
        reasoning: 'Términos detectados: árbol, rama.',
      };
      api.get.mockResolvedValueOnce({ data: mockRecommendation });

      const result = await getDepartmentRecommendation(params);

      expect(api.get).toHaveBeenCalledWith('/api/v2/analytics/recommend-department', {
        params,
      });
      expect(result).toEqual(mockRecommendation);
    });

    it('propaga el error si la llamada de recomendación falla', async () => {
      const mockError = new Error('Error interno del servidor 500');
      api.get.mockRejectedValueOnce(mockError);

      await expect(getDepartmentRecommendation({ incidentId: 99 })).rejects.toThrow(
        'Error interno del servidor 500'
      );
    });
  });

  describe('analyticsService default export', () => {
    it('contiene las funciones getAnalyticsSummary y getDepartmentRecommendation', () => {
      expect(analyticsService.getAnalyticsSummary).toBe(getAnalyticsSummary);
      expect(analyticsService.getDepartmentRecommendation).toBe(getDepartmentRecommendation);
    });
  });
});
