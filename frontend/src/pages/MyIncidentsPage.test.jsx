import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import MyIncidentsPage from './MyIncidentsPage';
import incidentService from '../services/incidentService';
import authService from '../services/authService';
import triageService from '../services/triageService';

vi.mock('../services/incidentService', () => ({
  default: {
    getMyIncidents: vi.fn(),
    getIncidentHistory: vi.fn(),
    getIncidentAttachments: vi.fn(),
  },
}));

vi.mock('../services/authService', () => ({
  default: {
    getUser: vi.fn(() => ({ id: 1, fullName: 'Ciudadano Prueba', email: 'ciudadano@malaga.es', role: 'CITIZEN' })),
    getCurrentUser: vi.fn(() => ({ id: 1, fullName: 'Ciudadano Prueba', email: 'ciudadano@malaga.es', role: 'CITIZEN' })),
    hasAnyRole: vi.fn(() => false),
    logout: vi.fn(),
  },
}));

vi.mock('../services/triageService', () => ({
  default: {
    getIncidentComments: vi.fn(),
    addIncidentComment: vi.fn(),
    updateIncidentComment: vi.fn(),
  },
}));

describe('MyIncidentsPage', () => {
  const sampleIncidents = [
    {
      id: 1,
      title: 'Alcorque roto en acera',
      description: 'Baldosa levantada con peligro de tropiezo.',
      category: 'Vías Públicas',
      status: 'REPORTED',
      priority: 'MEDIUM',
      address: 'Calle Marqués de Larios 10',
      district: 'Centro',
      latitude: 36.7215,
      longitude: -4.4214,
      createdAt: '2026-10-15T10:00:00Z',
      updatedAt: '2026-10-15T12:00:00Z',
    },
    {
      id: 2,
      title: 'Farola apagada',
      description: 'Punto de luz fundido en vía principal.',
      category: 'Alumbrado',
      status: 'IN_PROGRESS',
      priority: 'HIGH',
      address: null,
      district: null,
      latitude: 36.715,
      longitude: -4.43,
      createdAt: '2026-10-14T09:30:00Z',
      updatedAt: '2026-10-14T11:00:00Z',
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
    incidentService.getIncidentHistory.mockResolvedValue([]);
    incidentService.getIncidentAttachments.mockResolvedValue([]);
    triageService.getIncidentComments.mockResolvedValue([]);
  });

  it('renderiza la lista de incidencias con sus datos, dirección, distrito y badges', async () => {
    incidentService.getMyIncidents.mockResolvedValueOnce({
      content: sampleIncidents,
      totalElements: 2,
      totalPages: 1,
      page: 0,
      size: 10,
    });

    render(
      <MemoryRouter>
        <MyIncidentsPage />
      </MemoryRouter>
    );

    // Estado de carga inicial visible
    expect(screen.getByText(/Cargando tus incidencias.../i)).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('Alcorque roto en acera')).toBeInTheDocument();
    });

    // Título y categoría
    expect(screen.getByText('Farola apagada')).toBeInTheDocument();
    expect(screen.getByText('Vías Públicas')).toBeInTheDocument();
    expect(screen.getByText('Alumbrado')).toBeInTheDocument();

    // Badges de estado oficiales en la tabla
    const reportedElements = screen.getAllByText('Reportada');
    expect(reportedElements.some((el) => el.classList.contains('status-badge'))).toBe(true);

    const inProgressElements = screen.getAllByText('En progreso');
    expect(inProgressElements.some((el) => el.classList.contains('status-badge'))).toBe(true);

    // Dirección con valor y con fallback "Sin asignar"
    expect(screen.getByText('Calle Marqués de Larios 10')).toBeInTheDocument();
    const unassignedElements = screen.getAllByText('Sin asignar');
    expect(unassignedElements.length).toBeGreaterThanOrEqual(2); // Dirección y distrito nulos de la 2ª incidencia
  });

  it('permite filtrar incidencias por estado y restablece la página', async () => {
    incidentService.getMyIncidents.mockResolvedValueOnce({
      content: sampleIncidents,
      totalElements: 2,
      totalPages: 1,
      page: 0,
      size: 10,
    });

    render(
      <MemoryRouter>
        <MyIncidentsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Alcorque roto en acera')).toBeInTheDocument();
    });

    // Mock segunda llamada al cambiar filtro
    incidentService.getMyIncidents.mockResolvedValueOnce({
      content: [sampleIncidents[1]],
      totalElements: 1,
      totalPages: 1,
      page: 0,
      size: 10,
    });

    const filterSelect = screen.getByLabelText(/Filtrar por estado/i);
    fireEvent.change(filterSelect, { target: { value: 'IN_PROGRESS' } });

    await waitFor(() => {
      expect(incidentService.getMyIncidents).toHaveBeenCalledWith({
        page: 0,
        size: 10,
        status: 'IN_PROGRESS',
      });
    });
  });

  it('gestiona la paginación avanzando y retrocediendo entre páginas', async () => {
    incidentService.getMyIncidents.mockResolvedValueOnce({
      content: sampleIncidents,
      totalElements: 25,
      totalPages: 3,
      page: 0,
      size: 10,
    });

    render(
      <MemoryRouter>
        <MyIncidentsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText(/Página 1 de 3/i)).toBeInTheDocument();
    });

    const nextBtn = screen.getByRole('button', { name: /Página siguiente/i });
    const prevBtn = screen.getByRole('button', { name: /Página anterior/i });

    expect(prevBtn).toBeDisabled();
    expect(nextBtn).toBeEnabled();

    // Mock respuesta para la página 1 (2ª página)
    incidentService.getMyIncidents.mockResolvedValueOnce({
      content: [sampleIncidents[0]],
      totalElements: 25,
      totalPages: 3,
      page: 1,
      size: 10,
    });

    fireEvent.click(nextBtn);

    await waitFor(() => {
      expect(incidentService.getMyIncidents).toHaveBeenCalledWith({
        page: 1,
        size: 10,
        status: 'TODOS',
      });
    });
  });

  it('muestra mensaje informativo de estado vacío cuando no hay incidencias', async () => {
    incidentService.getMyIncidents.mockResolvedValueOnce({
      content: [],
      totalElements: 0,
      totalPages: 0,
      page: 0,
      size: 10,
    });

    render(
      <MemoryRouter>
        <MyIncidentsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(
        screen.getByText('No tienes incidencias registradas todavía')
      ).toBeInTheDocument();
    });

    const reportLink = screen.getByRole('link', {
      name: /Reportar incidencia en el mapa/i,
    });
    expect(reportLink).toBeInTheDocument();
    expect(reportLink.getAttribute('href')).toBe('/map');
  });

  it('muestra mensaje informativo de estado vacío al filtrar por un estado sin resultados', async () => {
    incidentService.getMyIncidents.mockResolvedValueOnce({
      content: sampleIncidents,
      totalElements: 2,
      totalPages: 1,
      page: 0,
      size: 10,
    });

    render(
      <MemoryRouter>
        <MyIncidentsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Alcorque roto en acera')).toBeInTheDocument();
    });

    // Mock respuesta vacía con filtro REJECTED
    incidentService.getMyIncidents.mockResolvedValueOnce({
      content: [],
      totalElements: 0,
      totalPages: 0,
      page: 0,
      size: 10,
    });

    const filterSelect = screen.getByLabelText(/Filtrar por estado/i);
    fireEvent.change(filterSelect, { target: { value: 'REJECTED' } });

    await waitFor(() => {
      expect(
        screen.getByText(
          'No se encontraron incidencias con el estado seleccionado'
        )
      ).toBeInTheDocument();
    });

    const clearButton = screen.getByRole('button', {
      name: /Mostrar todas las incidencias/i,
    });
    expect(clearButton).toBeInTheDocument();

    // Mock al limpiar filtro
    incidentService.getMyIncidents.mockResolvedValueOnce({
      content: sampleIncidents,
      totalElements: 2,
      totalPages: 1,
      page: 0,
      size: 10,
    });

    fireEvent.click(clearButton);

    await waitFor(() => {
      expect(incidentService.getMyIncidents).toHaveBeenCalledWith({
        page: 0,
        size: 10,
        status: 'TODOS',
      });
    });
  });

  it('abre y cierra el modal IncidentDetailModal al pulsar "Ver detalle" visualizando comentarios públicos', async () => {
    incidentService.getMyIncidents.mockResolvedValueOnce({
      content: sampleIncidents,
      totalElements: 2,
      totalPages: 1,
      page: 0,
      size: 10,
    });

    triageService.getIncidentComments.mockResolvedValueOnce([
      {
        id: 101,
        authorName: 'Operador Triaje',
        authorRole: 'OPERATOR',
        comment: 'Se ha enviado un equipo de inspección de calzada.',
        isInternal: false,
        createdAt: '2026-10-15T10:30:00Z',
      },
    ]);

    render(
      <MemoryRouter>
        <MyIncidentsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Alcorque roto en acera')).toBeInTheDocument();
    });

    const detailButtons = screen.getAllByRole('button', { name: /Ver detalle/i });
    expect(detailButtons.length).toBe(2);

    // Abrir detalle de la primera incidencia
    fireEvent.click(detailButtons[0]);

    await waitFor(() => {
      expect(
        screen.getByText(/Detalle de Incidencia Urbana #1/i)
      ).toBeInTheDocument();
      expect(screen.getByText('Información del Reporte')).toBeInTheDocument();
    });

    // Comprobar que en el modal se visualiza la descripción y los comentarios públicos
    expect(
      screen.getByText('Baldosa levantada con peligro de tropiezo.')
    ).toBeInTheDocument();

    await waitFor(() => {
      expect(
        screen.getByText('Se ha enviado un equipo de inspección de calzada.')
      ).toBeInTheDocument();
    });

    // Cerrar modal
    const closeBtn = screen.getByRole('button', {
      name: /Cerrar modal de detalle/i,
    });
    fireEvent.click(closeBtn);

    await waitFor(() => {
      expect(
        screen.queryByText(/Detalle de Incidencia Urbana #1/i)
      ).not.toBeInTheDocument();
    });
  });

  it('muestra mensaje de error accesible y permite reintentar la carga', async () => {
    incidentService.getMyIncidents.mockRejectedValueOnce(
      new Error('Fallo de conexión con el servidor')
    );

    render(
      <MemoryRouter>
        <MyIncidentsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(
        screen.getByText(/No se pudieron cargar tus incidencias/i)
      ).toBeInTheDocument();
    });

    const retryButton = screen.getByRole('button', { name: /Reintentar/i });
    expect(retryButton).toBeInTheDocument();

    // Mock respuesta exitosa en el reintento
    incidentService.getMyIncidents.mockResolvedValueOnce({
      content: sampleIncidents,
      totalElements: 2,
      totalPages: 1,
      page: 0,
      size: 10,
    });

    fireEvent.click(retryButton);

    await waitFor(() => {
      expect(screen.getByText('Alcorque roto en acera')).toBeInTheDocument();
    });
  });

  it('permite cerrar sesión desde la barra superior de navegación', async () => {
    incidentService.getMyIncidents.mockResolvedValueOnce({
      content: [],
      totalElements: 0,
      totalPages: 0,
      page: 0,
      size: 10,
    });

    render(
      <MemoryRouter>
        <MyIncidentsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(
        screen.getByText('No tienes incidencias registradas todavía')
      ).toBeInTheDocument();
    });

    const logoutBtn = screen.getByRole('button', { name: /Cerrar sesión/i });
    fireEvent.click(logoutBtn);

    expect(authService.logout).toHaveBeenCalledTimes(1);
  });
});
