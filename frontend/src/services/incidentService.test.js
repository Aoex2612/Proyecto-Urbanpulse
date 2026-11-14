import { describe, it, expect, vi, beforeEach } from 'vitest';
import incidentService, {
  searchIncidents,
  getMyIncidents,
  reverseGeocode,
  getIncidentPublicComments,
} from './incidentService';
import api from './api';

vi.mock('./api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
  },
}));

describe('incidentService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('searchIncidents', () => {
    it('realiza una busqueda multicriterio en /api/v2/incidents/search propagando los filtros', async () => {
      const mockResponse = { content: [{ id: 1, title: 'Farola rota' }], totalElements: 1 };
      api.get.mockResolvedValueOnce({ data: mockResponse });

      const params = {
        text: 'farola',
        category: 'Alumbrado',
        start: '2026-01-01',
        end: '2026-02-01',
        status: 'REPORTED',
        district: 'Centro',
        priority: 'HIGH',
        departmentId: 3,
        unassignedDepartment: false,
      };
      const result = await searchIncidents(params);

      expect(api.get).toHaveBeenCalledTimes(1);
      expect(api.get).toHaveBeenCalledWith('/api/v2/incidents/search', { params });
      expect(result).toEqual(mockResponse);
    });

    it('usa un objeto de parametros vacio por defecto y esta disponible en el default export', async () => {
      api.get.mockResolvedValueOnce({ data: [] });

      const result = await incidentService.searchIncidents();

      expect(api.get).toHaveBeenCalledWith('/api/v2/incidents/search', { params: {} });
      expect(result).toEqual([]);
    });
  });

  describe('getMyIncidents', () => {
    it('realiza una petición GET a /api/v2/incidents/my-incidents con los parámetros adecuados', async () => {
      const mockPageResponse = {
        content: [
          { id: 1, title: 'Farola rota', status: 'REPORTED' },
          { id: 2, title: 'Bache en calzada', status: 'IN_PROGRESS' },
        ],
        totalElements: 2,
        totalPages: 1,
        page: 0,
        size: 10,
      };

      api.get.mockResolvedValueOnce({ data: mockPageResponse });

      const params = { page: 0, size: 10, status: 'REPORTED' };
      const result = await getMyIncidents(params);

      expect(api.get).toHaveBeenCalledTimes(1);
      expect(api.get).toHaveBeenCalledWith('/api/v2/incidents/my-incidents', { params });
      expect(result).toEqual(mockPageResponse);
    });

    it('también está disponible a través del objeto default export', async () => {
      const mockPageResponse = { content: [], totalElements: 0, totalPages: 0, page: 0, size: 10 };
      api.get.mockResolvedValueOnce({ data: mockPageResponse });

      const result = await incidentService.getMyIncidents();
      expect(result).toEqual(mockPageResponse);
    });
  });

  describe('reverseGeocode', () => {
    it('resuelve dirección, distrito y barrio al recibir coordenadas válidas', async () => {
      const mockGeoResponse = {
        address: 'Calle Marqués de Larios, 5',
        district: 'Centro',
        neighborhood: 'Centro Histórico',
      };

      api.get.mockResolvedValueOnce({ data: mockGeoResponse });

      const result = await reverseGeocode(36.7213, -4.4214);

      expect(api.get).toHaveBeenCalledTimes(1);
      expect(api.get).toHaveBeenCalledWith('/api/v2/incidents/geocode/reverse', {
        params: { latitude: 36.7213, longitude: -4.4214 },
      });
      expect(result).toEqual({
        address: 'Calle Marqués de Larios, 5',
        district: 'Centro',
        neighborhood: 'Centro Histórico',
      });
    });

    it('aplica degradación controlada retornando nulls si el servidor falla o hay error de red', async () => {
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
      api.get.mockRejectedValueOnce(new Error('500 Internal Server Error'));

      const result = await reverseGeocode(36.7213, -4.4214);

      expect(result).toEqual({
        address: null,
        district: null,
        neighborhood: null,
      });

      consoleSpy.mockRestore();
    });

    it('también está disponible a través del objeto default export', async () => {
      api.get.mockResolvedValueOnce({
        data: { address: 'Calle Granada, 1', district: 'Centro', neighborhood: null },
      });

      const result = await incidentService.reverseGeocode(36.722, -4.42);
      expect(result).toEqual({
        address: 'Calle Granada, 1',
        district: 'Centro',
        neighborhood: null,
      });
    });
  });

  describe('getIncidentPublicComments', () => {
    it('consulta los comentarios de una incidencia mediante GET /api/v2/incidents/{id}/comments', async () => {
      const mockComments = [
        {
          id: 101,
          incidentId: 42,
          authorName: 'Operador Triaje',
          comment: 'Se ha enviado una brigada de mantenimiento.',
          isInternal: false,
          createdAt: '2026-11-06T10:00:00Z',
        },
      ];

      api.get.mockResolvedValueOnce({ data: mockComments });

      const result = await getIncidentPublicComments(42);

      expect(api.get).toHaveBeenCalledTimes(1);
      expect(api.get).toHaveBeenCalledWith('/api/v2/incidents/42/comments');
      expect(result).toEqual(mockComments);
    });

    it('está accesible mediante la exportación default incidentService.getIncidentPublicComments', async () => {
      api.get.mockResolvedValueOnce({ data: [] });

      const result = await incidentService.getIncidentPublicComments(7);
      expect(api.get).toHaveBeenCalledWith('/api/v2/incidents/7/comments');
      expect(result).toEqual([]);
    });
  });
});
