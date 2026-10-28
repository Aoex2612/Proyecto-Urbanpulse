import { describe, it, expect, vi, beforeEach } from 'vitest';
import incidentService, {
  getMyIncidents,
  reverseGeocode,
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

  describe('getMyIncidents', () => {
    it('realiza una petición GET a /api/incidents/my-incidents con los parámetros adecuados', async () => {
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
      expect(api.get).toHaveBeenCalledWith('/api/incidents/my-incidents', { params });
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
      expect(api.get).toHaveBeenCalledWith('/api/incidents/geocode/reverse', {
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
});
