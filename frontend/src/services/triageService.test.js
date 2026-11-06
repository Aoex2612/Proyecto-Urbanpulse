import { describe, it, expect, vi, beforeEach } from 'vitest';
import triageService, {
  calculatePriority,
  updatePriority,
  validateIncident,
  rejectIncident,
  reevaluateIncident,
  startWork,
  resolveIncident,
  closeIncident,
  reopenIncident,
  assignDepartmentsAndTechnicians,
  deactivateAssignments,
  getIncidentComments,
  addIncidentComment,
  updateIncidentComment,
  getDepartments,
  getTechniciansByDepartment,
} from './triageService';
import api from './api';

vi.mock('./api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}));

describe('triageService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('calculatePriority', () => {
    it('llama a POST /api/incidents/:id/priority/calculate con los factores', async () => {
      const mockResponse = {
        suggestedPriority: 'CRITICAL',
        priorityReason: 'Prioridad CRITICAL: corte de vía pública con riesgo alto de seguridad.',
        streetCut: true,
        safetyRisk: 'HIGH',
      };
      api.post.mockResolvedValueOnce({ data: mockResponse });

      const result = await calculatePriority(10, { streetCut: true, safetyRisk: 'HIGH' });

      expect(api.post).toHaveBeenCalledWith('/api/incidents/10/priority/calculate', {
        streetCut: true,
        safetyRisk: 'HIGH',
      });
      expect(result).toEqual(mockResponse);
    });
  });

  describe('updatePriority', () => {
    it('llama a PUT /api/incidents/:id/priority con la prioridad y justificación', async () => {
      const mockUpdated = { id: 10, priority: 'HIGH', priorityReason: 'Tráfico denso' };
      api.put.mockResolvedValueOnce({ data: mockUpdated });

      const result = await updatePriority(10, {
        priority: 'HIGH',
        priorityReason: 'Tráfico denso',
      });

      expect(api.put).toHaveBeenCalledWith('/api/incidents/10/priority', {
        priority: 'HIGH',
        priorityReason: 'Tráfico denso',
      });
      expect(result).toEqual(mockUpdated);
    });
  });

  describe('validateIncident', () => {
    it('llama a POST /api/incidents/:id/validate con motivo opcional', async () => {
      const mockResult = { id: 10, status: 'VALIDATED' };
      api.post.mockResolvedValueOnce({ data: mockResult });

      const result = await validateIncident(10, { reason: 'Datos contrastados' });

      expect(api.post).toHaveBeenCalledWith('/api/incidents/10/validate', {
        reason: 'Datos contrastados',
      });
      expect(result).toEqual(mockResult);
    });
  });

  describe('rejectIncident', () => {
    it('llama a POST /api/incidents/:id/reject con motivo justificado', async () => {
      const mockResult = { id: 10, status: 'REJECTED' };
      api.post.mockResolvedValueOnce({ data: mockResult });

      const result = await rejectIncident(10, { reason: 'Incidencia duplicada' });

      expect(api.post).toHaveBeenCalledWith('/api/incidents/10/reject', {
        reason: 'Incidencia duplicada',
      });
      expect(result).toEqual(mockResult);
    });
  });

  describe('reevaluateIncident', () => {
    it('llama a POST /api/incidents/:id/reevaluate con targetStatus y motivo', async () => {
      const mockResult = { id: 10, status: 'VALIDATED' };
      api.post.mockResolvedValueOnce({ data: mockResult });

      const result = await reevaluateIncident(10, {
        targetStatus: 'VALIDATED',
        reason: 'Se aportan nuevas pruebas fotográficas',
      });

      expect(api.post).toHaveBeenCalledWith('/api/incidents/10/reevaluate', {
        targetStatus: 'VALIDATED',
        reason: 'Se aportan nuevas pruebas fotográficas',
      });
      expect(result).toEqual(mockResult);
    });
  });

  describe('startWork', () => {
    it('llama a POST /api/incidents/:id/start-work', async () => {
      const mockResult = { id: 10, status: 'IN_PROGRESS' };
      api.post.mockResolvedValueOnce({ data: mockResult });

      const result = await startWork(10);

      expect(api.post).toHaveBeenCalledWith('/api/incidents/10/start-work');
      expect(result).toEqual(mockResult);
    });
  });

  describe('resolveIncident', () => {
    it('llama a POST /api/incidents/:id/resolve con notas de resolución', async () => {
      const mockResult = { id: 10, status: 'RESOLVED' };
      api.post.mockResolvedValueOnce({ data: mockResult });

      const result = await resolveIncident(10, {
        resolutionNotes: 'Sustitución de luminaria completada',
      });

      expect(api.post).toHaveBeenCalledWith('/api/incidents/10/resolve', {
        resolutionNotes: 'Sustitución de luminaria completada',
      });
      expect(result).toEqual(mockResult);
    });
  });

  describe('closeIncident', () => {
    it('llama a POST /api/incidents/:id/close', async () => {
      const mockResult = { id: 10, status: 'CLOSED' };
      api.post.mockResolvedValueOnce({ data: mockResult });

      const result = await closeIncident(10);

      expect(api.post).toHaveBeenCalledWith('/api/incidents/10/close');
      expect(result).toEqual(mockResult);
    });
  });

  describe('reopenIncident', () => {
    it('llama a POST /api/incidents/:id/reopen con motivo de reapertura', async () => {
      const mockResult = { id: 10, status: 'REOPENED' };
      api.post.mockResolvedValueOnce({ data: mockResult });

      const result = await reopenIncident(10, { reason: 'La luminaria vuelve a fallar' });

      expect(api.post).toHaveBeenCalledWith('/api/incidents/10/reopen', {
        reason: 'La luminaria vuelve a fallar',
      });
      expect(result).toEqual(mockResult);
    });
  });

  describe('assignDepartmentsAndTechnicians', () => {
    it('llama a POST /api/incidents/:id/assignments con IDs de departamentos y técnicos', async () => {
      const mockAssignments = [{ id: 1, departmentId: 2, technicianId: 5, isActive: true }];
      api.post.mockResolvedValueOnce({ data: mockAssignments });

      const result = await assignDepartmentsAndTechnicians(10, {
        departmentIds: [2],
        technicianIds: [5],
      });

      expect(api.post).toHaveBeenCalledWith('/api/incidents/10/assignments', {
        departmentIds: [2],
        technicianIds: [5],
      });
      expect(result).toEqual(mockAssignments);
    });
  });

  describe('deactivateAssignments', () => {
    it('llama a POST /api/incidents/:id/assignments/deactivate con los IDs a desactivar', async () => {
      const mockResult = [];
      api.post.mockResolvedValueOnce({ data: mockResult });

      const result = await deactivateAssignments(10, {
        departmentIds: [2],
        technicianIds: [5],
      });

      expect(api.post).toHaveBeenCalledWith('/api/incidents/10/assignments/deactivate', {
        departmentIds: [2],
        technicianIds: [5],
      });
      expect(result).toEqual(mockResult);
    });
  });

  describe('getIncidentComments', () => {
    it('llama a GET /api/incidents/:id/comments', async () => {
      const mockComments = [{ id: 1, comment: 'Nota de seguimiento', isInternal: true }];
      api.get.mockResolvedValueOnce({ data: mockComments });

      const result = await getIncidentComments(10);

      expect(api.get).toHaveBeenCalledWith('/api/incidents/10/comments');
      expect(result).toEqual(mockComments);
    });
  });

  describe('addIncidentComment', () => {
    it('llama a POST /api/incidents/:id/comments con comment e isInternal', async () => {
      const mockComment = { id: 1, comment: 'Requiere grúa', isInternal: true };
      api.post.mockResolvedValueOnce({ data: mockComment });

      const result = await addIncidentComment(10, {
        comment: 'Requiere grúa',
        isInternal: true,
      });

      expect(api.post).toHaveBeenCalledWith('/api/incidents/10/comments', {
        comment: 'Requiere grúa',
        isInternal: true,
      });
      expect(result).toEqual(mockComment);
    });
  });

  describe('updateIncidentComment', () => {
    it('llama a PUT /api/incidents/:id/comments/:commentId con el texto actualizado', async () => {
      const mockUpdated = { id: 1, comment: 'Requiere grúa pluma' };
      api.put.mockResolvedValueOnce({ data: mockUpdated });

      const result = await updateIncidentComment(10, 1, { comment: 'Requiere grúa pluma' });

      expect(api.put).toHaveBeenCalledWith('/api/incidents/10/comments/1', {
        comment: 'Requiere grúa pluma',
      });
      expect(result).toEqual(mockUpdated);
    });
  });

  describe('getDepartments', () => {
    it('llama a GET /api/departments', async () => {
      const mockDepartments = [
        { id: 1, name: 'Alumbrado Público' },
        { id: 2, name: 'Parques y Jardines' },
      ];
      api.get.mockResolvedValueOnce({ data: mockDepartments });

      const result = await getDepartments();

      expect(api.get).toHaveBeenCalledWith('/api/departments');
      expect(result).toEqual(mockDepartments);
    });
  });

  describe('getTechniciansByDepartment', () => {
    it('llama a GET /api/departments/:id/technicians', async () => {
      const mockTechnicians = [{ id: 3, fullName: 'Técnico Pérez', email: 'perez@malaga.es' }];
      api.get.mockResolvedValueOnce({ data: mockTechnicians });

      const result = await getTechniciansByDepartment(2);

      expect(api.get).toHaveBeenCalledWith('/api/departments/2/technicians');
      expect(result).toEqual(mockTechnicians);
    });
  });

  describe('triageService default export', () => {
    it('contiene todas las funciones operativas', () => {
      expect(triageService.calculatePriority).toBe(calculatePriority);
      expect(triageService.updatePriority).toBe(updatePriority);
      expect(triageService.validateIncident).toBe(validateIncident);
      expect(triageService.rejectIncident).toBe(rejectIncident);
      expect(triageService.reevaluateIncident).toBe(reevaluateIncident);
      expect(triageService.startWork).toBe(startWork);
      expect(triageService.resolveIncident).toBe(resolveIncident);
      expect(triageService.closeIncident).toBe(closeIncident);
      expect(triageService.reopenIncident).toBe(reopenIncident);
      expect(triageService.assignDepartmentsAndTechnicians).toBe(assignDepartmentsAndTechnicians);
      expect(triageService.deactivateAssignments).toBe(deactivateAssignments);
      expect(triageService.getIncidentComments).toBe(getIncidentComments);
      expect(triageService.addIncidentComment).toBe(addIncidentComment);
      expect(triageService.updateIncidentComment).toBe(updateIncidentComment);
      expect(triageService.getDepartments).toBe(getDepartments);
      expect(triageService.getTechniciansByDepartment).toBe(getTechniciansByDepartment);
    });
  });
});
