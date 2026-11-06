/**
 * @fileoverview Pruebas de integración del Panel de Operador y Gestión de Triaje (`OperatorPanelPage`).
 * Valida la renderización del panel, cálculo de estadísticas, visualización de badges departamentales
 * y ejecución reactiva de acciones contextuales: validación, rechazo, prioridad asistida, asignación,
 * inicio de trabajo, resolución técnica, cierre, reapertura, reevaluación y muro de comentarios.
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import OperatorPanelPage from './OperatorPanelPage';
import incidentService from '../services/incidentService';
import triageService from '../services/triageService';
import authService from '../services/authService';

// Mock de servicios
vi.mock('../services/incidentService', () => ({
  default: {
    getAllIncidents: vi.fn(),
  },
}));

vi.mock('../services/authService', () => ({
  default: {
    getUser: vi.fn(() => ({
      id: 99,
      fullName: 'Administrador Test',
      email: 'admin@malaga.es',
      role: 'ADMIN',
    })),
    logout: vi.fn(),
  },
}));

vi.mock('../services/triageService', () => ({
  default: {
    calculatePriority: vi.fn(),
    updatePriority: vi.fn(),
    validateIncident: vi.fn(),
    rejectIncident: vi.fn(),
    reevaluateIncident: vi.fn(),
    startWork: vi.fn(),
    resolveIncident: vi.fn(),
    closeIncident: vi.fn(),
    reopenIncident: vi.fn(),
    assignIncident: vi.fn(),
    deactivateAssignment: vi.fn(),
    getIncidentComments: vi.fn().mockResolvedValue([]),
    createComment: vi.fn(),
    updateComment: vi.fn(),
    getDepartments: vi.fn().mockResolvedValue([]),
    getTechniciansByDepartment: vi.fn().mockResolvedValue([]),
    getIncidentAssignments: vi.fn().mockResolvedValue([]),
  },
}));

describe('OperatorPanelPage — Integración de Triaje Operativo', () => {
  const mockIncidents = [
    {
      id: 1,
      title: 'Bache profundo en calzada',
      description: 'Hoyo en asfalto que corta carril derecho.',
      category: 'Vías Públicas',
      priority: 'HIGH',
      status: 'REPORTED',
      createdAt: '2026-11-01T10:00:00Z',
      assignments: [],
    },
    {
      id: 2,
      title: 'Farola fundida en parque',
      description: 'Luminaria apagada en zona infantil.',
      category: 'Alumbrado',
      priority: 'MEDIUM',
      status: 'VALIDATED',
      createdAt: '2026-11-01T11:00:00Z',
      assignments: [],
    },
    {
      id: 3,
      title: 'Contenedor quemado',
      description: 'Restos de plástico quemado en acera.',
      category: 'Limpieza',
      priority: 'HIGH',
      status: 'ASSIGNED',
      createdAt: '2026-11-01T12:00:00Z',
      assignments: [
        {
          id: 501,
          departmentId: 10,
          departmentName: 'Limpieza Urbana',
          technicianId: 44,
          technicianName: 'Carlos Técnico',
          isActive: true,
        },
      ],
    },
    {
      id: 4,
      title: 'Fuga de agua en boca de riego',
      description: 'Pérdida continua de agua potable.',
      category: 'Vías Públicas',
      priority: 'CRITICAL',
      status: 'IN_PROGRESS',
      createdAt: '2026-11-01T13:00:00Z',
      assignments: [
        {
          id: 502,
          departmentId: 12,
          departmentName: 'Aguas de Málaga',
          technicianId: 45,
          technicianName: 'Marta Fontanera',
          isActive: true,
        },
      ],
    },
    {
      id: 5,
      title: 'Banco roto en plaza',
      description: 'Listón de madera suelto.',
      category: 'Mobiliario Urbano',
      priority: 'LOW',
      status: 'RESOLVED',
      createdAt: '2026-11-01T14:00:00Z',
      assignments: [],
    },
    {
      id: 6,
      title: 'Pintada en muro histórico',
      description: 'Grafiti en fachada protegida.',
      category: 'Limpieza',
      priority: 'LOW',
      status: 'REJECTED',
      createdAt: '2026-11-01T15:00:00Z',
      assignments: [],
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
    authService.getUser.mockReturnValue({
      id: 99,
      fullName: 'Administrador Test',
      email: 'admin@malaga.es',
      role: 'ADMIN',
    });
    incidentService.getAllIncidents.mockResolvedValue(mockIncidents);
  });

  const renderPanel = () =>
    render(
      <MemoryRouter>
        <OperatorPanelPage />
      </MemoryRouter>
    );

  it('renderiza la vista con el resumen general, estadísticas y la lista de incidencias', async () => {
    renderPanel();

    expect(await screen.findByText('Centro de Triaje y Operaciones Urbanas')).toBeInTheDocument();
    expect(screen.getByText('Bache profundo en calzada')).toBeInTheDocument();
    expect(screen.getByText('Farola fundida en parque')).toBeInTheDocument();
    expect(screen.getByText('Contenedor quemado')).toBeInTheDocument();

    // Verificación de estadísticas
    expect(screen.getByText('Pendientes de Triaje')).toBeInTheDocument();
    expect(screen.getByText('En Tramitación')).toBeInTheDocument();
    expect(screen.getByText('Resueltas / Cerradas')).toBeInTheDocument();
    expect(screen.getByText('Total en BD')).toBeInTheDocument();
  });

  it('muestra badges de departamentos y técnicos asignados o "Sin asignar"', async () => {
    renderPanel();

    await screen.findByText('Contenedor quemado');

    // Incidencia #3 tiene asignación a 'Limpieza Urbana' y técnico 'Carlos Técnico'
    expect(screen.getByText(/Limpieza Urbana/i)).toBeInTheDocument();
    expect(screen.getByText(/Carlos Técnico/i)).toBeInTheDocument();

    // Incidencias sin asignación muestran 'Sin asignar'
    const unassignedBadges = screen.getAllByText('Sin asignar');
    expect(unassignedBadges.length).toBeGreaterThan(0);
  });

  it('permite validar una incidencia en estado REPORTED y actualiza reactivamente a VALIDATED', async () => {
    triageService.validateIncident.mockResolvedValue({
      id: 1,
      status: 'VALIDATED',
    });

    renderPanel();

    const validateBtn = await screen.findByRole('button', { name: 'Validar incidencia #1' });
    fireEvent.click(validateBtn);

    await waitFor(() => {
      expect(triageService.validateIncident).toHaveBeenCalledWith(1);
      expect(
        screen.getByText(/Incidencia #1 validada formalmente en el circuito de operaciones/i)
      ).toBeInTheDocument();
    });
  });

  it('abre el modal de motivo al pulsar Rechazar en una incidencia REPORTED y ejecuta el rechazo', async () => {
    triageService.rejectIncident.mockResolvedValue({
      id: 1,
      status: 'REJECTED',
    });

    renderPanel();

    const rejectBtn = await screen.findByRole('button', { name: 'Rechazar incidencia #1' });
    fireEvent.click(rejectBtn);

    // Modal de motivo visible
    expect(screen.getByText('Rechazar Incidencia #1')).toBeInTheDocument();
    const textarea = screen.getByPlaceholderText(/Detalla el motivo del rechazo/i);

    // Escribir motivo >= 5 caracteres
    fireEvent.change(textarea, { target: { value: 'Reporte improcedente o fuera de término municipal' } });
    const confirmBtn = screen.getByRole('button', { name: 'Confirmar Rechazo' });
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(triageService.rejectIncident).toHaveBeenCalledWith(1, {
        reason: 'Reporte improcedente o fuera de término municipal',
      });
      expect(screen.getByText(/Incidencia #1 rechazada y asignaciones inactivadas/i)).toBeInTheDocument();
    });
  });

  it('abre el PriorityAssistantModal al pulsar el botón del asistente de prioridad', async () => {
    renderPanel();

    const priorityBtn = await screen.findByRole('button', {
      name: 'Prioridad asistida para incidencia #1',
    });
    fireEvent.click(priorityBtn);

    // Modal de cálculo asistido abierto
    expect(await screen.findByText('Asistente de Priorización')).toBeInTheDocument();
    expect(screen.getByText('Factores de Riesgo e Impacto')).toBeInTheDocument();
  });

  it('abre el AssignmentModal al pulsar Asignar Departamentos/Técnicos en estado VALIDATED', async () => {
    renderPanel();

    const assignBtn = await screen.findByRole('button', {
      name: 'Asignar departamentos a #2',
    });
    fireEvent.click(assignBtn);

    // Modal de asignación multidepartamental abierto
    expect(
      await screen.findByText('Asignación de Departamentos y Técnicos')
    ).toBeInTheDocument();
  });

  it('permite iniciar trabajo técnico en incidencias ASSIGNED transicionando a IN_PROGRESS', async () => {
    triageService.startWork.mockResolvedValue({
      id: 3,
      status: 'IN_PROGRESS',
    });

    renderPanel();

    const startBtn = await screen.findByRole('button', {
      name: 'Iniciar trabajo en incidencia #3',
    });
    fireEvent.click(startBtn);

    await waitFor(() => {
      expect(triageService.startWork).toHaveBeenCalledWith(3);
      expect(
        screen.getByText(/Trabajos de campo iniciados sobre la incidencia #3/i)
      ).toBeInTheDocument();
    });
  });

  it('abre el ResolveIncidentModal al pulsar Resolver en una incidencia IN_PROGRESS', async () => {
    renderPanel();

    const resolveBtn = await screen.findByRole('button', {
      name: 'Resolver incidencia #4',
    });
    fireEvent.click(resolveBtn);

    // Modal de resolución técnica abierto
    expect(await screen.findByText('Reportar Resolución de Incidencia')).toBeInTheDocument();
    expect(screen.getByText(/Memoria de Actuaciones Realizadas/i)).toBeInTheDocument();
  });

  it('permite cerrar una incidencia RESOLVED llamando al endpoint closeIncident', async () => {
    triageService.closeIncident.mockResolvedValue({
      id: 5,
      status: 'CLOSED',
    });

    renderPanel();

    const closeBtn = await screen.findByRole('button', {
      name: 'Cerrar incidencia #5',
    });
    fireEvent.click(closeBtn);

    await waitFor(() => {
      expect(triageService.closeIncident).toHaveBeenCalledWith(5);
      expect(screen.getByText(/Incidencia #5 cerrada formal y administrativamente/i)).toBeInTheDocument();
    });
  });

  it('permite reabrir una incidencia RESOLVED abriendo el ReasonModal y enviando la justificación', async () => {
    triageService.reopenIncident.mockResolvedValue({
      id: 5,
      status: 'REOPENED',
    });

    renderPanel();

    const reopenBtn = await screen.findByRole('button', {
      name: 'Reabrir incidencia #5',
    });
    fireEvent.click(reopenBtn);

    // Modal de reapertura abierto
    expect(screen.getByText('Reabrir Incidencia #5')).toBeInTheDocument();
    const textarea = screen.getByPlaceholderText(/Explica por qué la resolución fue insuficiente/i);

    fireEvent.change(textarea, { target: { value: 'La reparación se soltó a las pocas horas' } });
    const confirmBtn = screen.getByRole('button', { name: 'Confirmar Reapertura' });
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(triageService.reopenIncident).toHaveBeenCalledWith(5, {
        reason: 'La reparación se soltó a las pocas horas',
      });
      expect(screen.getByText(/Incidencia #5 reabierta para nuevas actuaciones/i)).toBeInTheDocument();
    });
  });

  it('permite reevaluar una incidencia REJECTED seleccionando targetStatus y enviando motivo', async () => {
    triageService.reevaluateIncident.mockResolvedValue({
      id: 6,
      status: 'VALIDATED',
    });

    renderPanel();

    const reevaluateBtn = await screen.findByRole('button', {
      name: 'Reevaluar incidencia rechazada #6',
    });
    fireEvent.click(reevaluateBtn);

    expect(screen.getByText('Reevaluar Incidencia Rechazada #6')).toBeInTheDocument();
    const textarea = screen.getByPlaceholderText(/Justifica la rectificación/i);
    fireEvent.change(textarea, { target: { value: 'Se aporta informe que demuestra competencia municipal' } });

    const confirmBtn = screen.getByRole('button', { name: 'Confirmar Reevaluación' });
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(triageService.reevaluateIncident).toHaveBeenCalledWith(6, {
        targetStatus: 'VALIDATED',
        reason: 'Se aporta informe que demuestra competencia municipal',
      });
      expect(screen.getByText(/Incidencia #6 reevaluada y reactivada en estado 'Validada'/i)).toBeInTheDocument();
    });
  });

  it('abre el modal de comentarios al pulsar el botón Comentarios', async () => {
    renderPanel();

    const commentsBtn = await screen.findByRole('button', {
      name: 'Ver y redactar comentarios de la incidencia #1',
    });
    fireEvent.click(commentsBtn);

    expect(screen.getByText(/Muro de Comentarios — Incidencia #1/i)).toBeInTheDocument();
  });

  it('para rol TECHNICIAN adapta el encabezado a Mis Asignaciones Técnicas y oculta acciones de triaje exclusivo', async () => {
    authService.getUser.mockReturnValue({
      id: 44,
      fullName: 'Carlos Técnico',
      email: 'carlos.tech@malaga.es',
      role: 'TECHNICIAN',
    });

    renderPanel();

    expect(await screen.findByText('Mis Asignaciones Técnicas')).toBeInTheDocument();
    expect(
      screen.getByText('Incidencias asignadas para intervención y resolución técnica en campo.')
    ).toBeInTheDocument();
    expect(screen.getByText('Técnico de Campo')).toBeInTheDocument();

    // Verificación de estadísticas específicas de técnico
    expect(screen.getByText('Pendientes de Inicio')).toBeInTheDocument();
    expect(screen.getByText('En Intervención')).toBeInTheDocument();
    expect(screen.getByText('Total Asignadas')).toBeInTheDocument();

    // Botones de técnico presentes para su incidencia asignada y en progreso
    expect(screen.getByRole('button', { name: 'Iniciar trabajo en incidencia #3' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Resolver incidencia #4' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Ver y redactar comentarios de la incidencia #3' })).toBeInTheDocument();

    // Acciones de operador ocultas para técnico
    expect(screen.queryByRole('button', { name: 'Validar incidencia #1' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Rechazar incidencia #1' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Asignar departamentos a #2' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Cerrar incidencia #5' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Reabrir incidencia #5' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Prioridad asistida para incidencia #1' })).not.toBeInTheDocument();
  });

  it('para rol OPERATOR oculta botones de campo (iniciar trabajo y resolver) mostrando únicamente triaje', async () => {
    authService.getUser.mockReturnValue({
      id: 20,
      fullName: 'Operador Municipal',
      email: 'operador@malaga.es',
      role: 'OPERATOR',
    });

    renderPanel();

    expect(await screen.findByText('Centro de Triaje y Operaciones Urbanas')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Validar incidencia #1' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Asignar departamentos a #2' })).toBeInTheDocument();

    // No debe mostrar Iniciar Trabajo ni Resolver a operadores
    expect(screen.queryByRole('button', { name: 'Iniciar trabajo en incidencia #3' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Resolver incidencia #4' })).not.toBeInTheDocument();
  });
});
