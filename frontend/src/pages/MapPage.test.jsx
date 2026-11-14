/**
 * @fileoverview Pruebas de integración de la vista del Mapa de Incidencias (`MapPage`).
 * Valida el RBAC de los filtros avanzados, la busqueda multicriterio con debounce,
 * el aviso de ausencia de resultados (RF-14) y la conservacion de filtros ante modales (RF-15).
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import MapPage from './MapPage';
import incidentService from '../services/incidentService';
import triageService from '../services/triageService';

vi.mock('../components/Map', () => ({
  IncidentsMap: ({ incidents, onSelectIncident }) => (
    <div>
      <button type="button" onClick={() => onSelectIncident(incidents[0])}>
        Abrir detalle
      </button>
    </div>
  ),
  CreateIncidentModal: () => null,
  MapDateFilter: () => null,
  IncidentDetailModal: ({ incident, onClose }) => (
    <div role="dialog" aria-label="Detalle de incidencia">
      <span>{incident?.title}</span>
      <button type="button" onClick={onClose}>
        Cerrar detalle
      </button>
    </div>
  ),
}));

vi.mock('../services/incidentService', () => ({
  default: {
    getAllIncidents: vi.fn(),
    searchIncidents: vi.fn(),
  },
}));

vi.mock('../services/triageService', () => ({
  default: {
    getDepartments: vi.fn(),
  },
}));

let mockUser = { role: 'CITIZEN', fullName: 'Ciudadano Test' };

vi.mock('../services/authService', () => ({
  default: {
    getCurrentUser: vi.fn(() => mockUser),
    getUser: vi.fn(() => mockUser),
    hasAnyRole: vi.fn(
      (roles) => Boolean(mockUser?.role) && roles.includes(mockUser.role)
    ),
    logout: vi.fn(),
  },
}));

describe('MapPage — filtros avanzados y RBAC', () => {
  const recent = new Date().toISOString();

  const mockIncidents = [
    {
      id: 'inc-centro',
      title: 'Bache en el Centro',
      description: 'Hoyo en calzada del distrito Centro.',
      category: 'Vías Públicas',
      priority: 'HIGH',
      status: 'REPORTED',
      district: 'Centro',
      latitude: 36.7213,
      longitude: -4.4214,
      createdAt: recent,
      activeAssignments: [],
    },
    {
      id: 'inc-este',
      title: 'Farola fundida en el Este',
      description: 'Luminaria apagada en el distrito Este.',
      category: 'Alumbrado',
      priority: 'LOW',
      status: 'VALIDATED',
      district: 'Este',
      latitude: 36.73,
      longitude: -4.4,
      createdAt: recent,
      activeAssignments: [
        {
          id: 'asig-1',
          departmentId: 'uuid-limpieza',
          departmentName: 'Limpieza Urbana',
          isActive: true,
        },
      ],
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
    mockUser = { role: 'CITIZEN', fullName: 'Ciudadano Test' };
    incidentService.getAllIncidents.mockResolvedValue(mockIncidents);
    incidentService.searchIncidents.mockResolvedValue(mockIncidents);
    triageService.getDepartments.mockResolvedValue([]);
  });

  const renderMap = () =>
    render(
      <MemoryRouter>
        <MapPage />
      </MemoryRouter>
    );

  it('para CITIZEN solo muestra el filtro de distrito y no carga departamentos', async () => {
    renderMap();

    expect(await screen.findByLabelText(/Distrito:/i)).toBeInTheDocument();
    expect(screen.queryByLabelText(/Prioridad:/i)).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/Departamento:/i)).not.toBeInTheDocument();
    expect(triageService.getDepartments).not.toHaveBeenCalled();
  });

  it('para OPERATOR muestra distrito, prioridad y departamento con el catalogo cargado', async () => {
    mockUser = { role: 'OPERATOR', fullName: 'Operador Test' };
    triageService.getDepartments.mockResolvedValue([
      { id: 'uuid-limpieza', name: 'Limpieza Urbana' },
    ]);

    renderMap();

    expect(await screen.findByLabelText(/Distrito:/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Prioridad:/i)).toBeInTheDocument();
    const departmentSelect = screen.getByLabelText(/Departamento:/i);

    await waitFor(() => {
      expect(triageService.getDepartments).toHaveBeenCalled();
      expect(departmentSelect).toContainElement(
        screen.getByRole('option', { name: 'Limpieza Urbana' })
      );
    });
  });

  it('invoca searchIncidents con distrito y unassignedDepartment tras el debounce', async () => {
    mockUser = { role: 'OPERATOR', fullName: 'Operador Test' };

    renderMap();

    await screen.findByLabelText(/Distrito:/i);

    fireEvent.change(screen.getByLabelText(/Distrito:/i), { target: { value: 'Centro' } });
    fireEvent.change(screen.getByLabelText(/Departamento:/i), {
      target: { value: 'unassigned' },
    });

    await waitFor(
      () => {
        expect(incidentService.searchIncidents).toHaveBeenCalledWith({
          text: undefined,
          category: undefined,
          status: undefined,
          district: 'Centro',
          priority: undefined,
          departmentId: undefined,
          unassignedDepartment: true,
        });
      },
      { timeout: 3000 }
    );
  });

  it('muestra el aviso de RF-14 cuando la busqueda no devuelve resultados', async () => {
    incidentService.searchIncidents.mockResolvedValue([]);

    renderMap();

    await screen.findByLabelText(/Distrito:/i);
    fireEvent.change(screen.getByLabelText(/Distrito:/i), { target: { value: 'Centro' } });

    expect(
      await screen.findByText(/No se encontraron incidencias con los filtros aplicados/i)
    ).toBeInTheDocument();
  });

  it('conserva el distrito seleccionado al abrir y cerrar el modal de detalle (RF-15)', async () => {
    mockUser = { role: 'OPERATOR', fullName: 'Operador Test' };

    renderMap();

    await screen.findByLabelText(/Distrito:/i);
    const districtSelect = screen.getByLabelText(/Distrito:/i);
    fireEvent.change(districtSelect, { target: { value: 'Centro' } });
    expect(districtSelect).toHaveValue('Centro');

    fireEvent.click(screen.getByRole('button', { name: 'Abrir detalle' }));
    expect(screen.getByRole('dialog', { name: 'Detalle de incidencia' })).toBeInTheDocument();
    expect(screen.getByText('Bache en el Centro')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Cerrar detalle' }));

    expect(screen.queryByRole('dialog', { name: 'Detalle de incidencia' })).not.toBeInTheDocument();
    expect(screen.getByLabelText(/Distrito:/i)).toHaveValue('Centro');
  });
});
