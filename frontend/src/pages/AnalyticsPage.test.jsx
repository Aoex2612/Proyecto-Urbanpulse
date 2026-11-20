/**
 * @fileoverview Pruebas de integración para la página principal de Analítica Urbana (`AnalyticsPage`).
 * Valida la carga inicial de métricas, renderizado de KPIs, gráficos SVG y tabla paginada,
 * actualización reactiva ante cambios en los filtros, manejo de estado vacío (RF-11) y gestión de errores.
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import AnalyticsPage from './AnalyticsPage';
import analyticsService from '../services/analyticsService';

// Mocks de servicios
vi.mock('../services/analyticsService', () => ({
  default: {
    getAnalyticsSummary: vi.fn(),
    getDepartmentRecommendation: vi.fn(),
  },
}));

vi.mock('../services/authService', () => ({
  default: {
    getUser: vi.fn(() => ({
      id: 10,
      fullName: 'Carlos Analista',
      email: 'analista@malaga.es',
      role: 'ANALYST',
    })),
    logout: vi.fn(),
  },
}));

describe('AnalyticsPage — Panel de Analítica Urbana y Reportes', () => {
  const mockSummaryWithData = {
    operationalMetrics: {
      totalIncidents: 25,
      resolvedIncidents: 20,
      pendingIncidents: 5,
      mtttHours: 2.5,
      mttrHours: 28.0,
      slaComplianceRate: 88.0,
      totalSlaCompliant: 22,
      totalSlaBreached: 3,
    },
    territorialMetrics: [
      {
        territoryName: 'Centro',
        totalIncidents: 15,
        resolvedCount: 12,
        pendingCount: 3,
        slaComplianceRate: 92.0,
      },
      {
        territoryName: 'Este',
        totalIncidents: 10,
        resolvedCount: 8,
        pendingCount: 2,
        slaComplianceRate: 82.0,
      },
    ],
    departmentMetrics: [
      {
        departmentId: 1,
        departmentName: 'Medio Ambiente',
        totalAssigned: 15,
        resolvedCount: 12,
        avgResolutionHours: 20.0,
        slaComplianceRate: 90.0,
      },
    ],
    assistantPerformance: {
      totalRecommendations: 30,
      acceptedRecommendations: 27,
      adoptionRate: 90.0,
      averageConfidence: 85.0,
    },
    filteredIncidents: [
      {
        id: 101,
        title: 'Rotura de tubería en Calle Granada',
        district: 'Centro',
        neighborhood: 'Centro Histórico',
        category: 'AGUA',
        priority: 'CRITICAL',
        status: 'RESOLVED',
        createdAt: '2026-10-15T09:30:00Z',
      },
      {
        id: 102,
        title: 'Farola fundida en Paseo Marítimo',
        district: 'Este',
        neighborhood: 'Pedregalejo',
        category: 'ALUMBRADO',
        priority: 'MEDIUM',
        status: 'IN_PROGRESS',
        createdAt: '2026-10-16T11:00:00Z',
      },
    ],
  };

  const mockEmptySummary = {
    operationalMetrics: {
      totalIncidents: 0,
      resolvedIncidents: 0,
      pendingIncidents: 0,
      mtttHours: 0,
      mttrHours: 0,
      slaComplianceRate: 0,
      totalSlaCompliant: 0,
      totalSlaBreached: 0,
    },
    territorialMetrics: [],
    departmentMetrics: [],
    assistantPerformance: {
      totalRecommendations: 0,
      acceptedRecommendations: 0,
      adoptionRate: 0,
      averageConfidence: 0,
    },
    filteredIncidents: [],
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('muestra estado de carga inicial y luego renderiza KPIs, gráficos y tabla con datos', async () => {
    analyticsService.getAnalyticsSummary.mockResolvedValueOnce(mockSummaryWithData);

    render(
      <MemoryRouter>
        <AnalyticsPage />
      </MemoryRouter>
    );

    // Debe llamar a getAnalyticsSummary con el preset por defecto LAST_30_DAYS
    await waitFor(() => {
      expect(analyticsService.getAnalyticsSummary).toHaveBeenCalledWith({
        preset: 'LAST_30_DAYS',
      });
    });

    // Esperar a que se rendericen los datos
    await waitFor(() => {
      expect(screen.getByTestId('analytics-content')).toBeInTheDocument();
    });

    // Encabezado institucional
    expect(screen.getByText('Analítica Urbana y Rendimiento Operativo')).toBeInTheDocument();
    expect(screen.getByText(/Ayuntamiento de Málaga/i)).toBeInTheDocument();

    // KPIs
    expect(screen.getByTestId('kpi-card-total')).toBeInTheDocument();
    expect(screen.getByText('25')).toBeInTheDocument();
    expect(screen.getByText('2.5 h')).toBeInTheDocument();
    expect(screen.getByText('28.0 h')).toBeInTheDocument();
    expect(screen.getByText('88.0%')).toBeInTheDocument();

    // Gráficos SVG
    expect(screen.getByTestId('territorial-chart-card')).toBeInTheDocument();
    expect(screen.getByTestId('sla-chart-card')).toBeInTheDocument();

    // Tabla de incidencias
    expect(screen.getByTestId('analytics-incidents-table')).toBeInTheDocument();
    expect(screen.getByText('#101')).toBeInTheDocument();
    expect(screen.getByText('Rotura de tubería en Calle Granada')).toBeInTheDocument();
    expect(screen.getByText('#102')).toBeInTheDocument();
    expect(screen.getByText('Farola fundida en Paseo Marítimo')).toBeInTheDocument();

    // Botón de exportar PDF presente y habilitado
    const exportBtn = screen.getByRole('button', {
      name: /Exportar informe ejecutivo en formato PDF/i,
    });
    expect(exportBtn).toBeInTheDocument();
    expect(exportBtn).not.toBeDisabled();
  });

  it('renderiza el estado vacío cuando no hay incidencias registradas en el período (RF-11)', async () => {
    analyticsService.getAnalyticsSummary.mockResolvedValueOnce(mockEmptySummary);

    render(
      <MemoryRouter>
        <AnalyticsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('empty-analytics-state')).toBeInTheDocument();
    });

    expect(
      screen.getByText(/no se encontraron incidencias en el período seleccionado/i)
    ).toBeInTheDocument();

    // El botón de exportación PDF debe estar inhabilitado en estado vacío (RF-14)
    const exportBtn = screen.getByRole('button', {
      name: /Exportar informe ejecutivo en formato PDF/i,
    });
    expect(exportBtn).toBeDisabled();
  });

  it('permite cambiar el filtro de fecha e invoca de nuevo a la API con el nuevo preset', async () => {
    analyticsService.getAnalyticsSummary.mockResolvedValue(mockSummaryWithData);

    render(
      <MemoryRouter>
        <AnalyticsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('analytics-content')).toBeInTheDocument();
    });

    // Cambiar a preset "Últimos 7 días"
    const btn7d = screen.getByTestId('preset-btn-LAST_7_DAYS');
    fireEvent.click(btn7d);

    await waitFor(() => {
      expect(analyticsService.getAnalyticsSummary).toHaveBeenCalledWith({
        preset: 'LAST_7_DAYS',
      });
    });
  });

  it('permite filtrar por distrito de Málaga y envía el parámetro a la API', async () => {
    analyticsService.getAnalyticsSummary.mockResolvedValue(mockSummaryWithData);

    render(
      <MemoryRouter>
        <AnalyticsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('analytics-content')).toBeInTheDocument();
    });

    const districtSelect = screen.getByLabelText(/filtrar por distrito municipal/i);
    fireEvent.change(districtSelect, { target: { value: 'Centro' } });

    await waitFor(() => {
      expect(analyticsService.getAnalyticsSummary).toHaveBeenCalledWith({
        preset: 'LAST_30_DAYS',
        district: 'Centro',
      });
    });
  });

  it('muestra estado de error si la petición al servicio falla y permite reintentar', async () => {
    analyticsService.getAnalyticsSummary
      .mockRejectedValueOnce(new Error('Fallo de conexión'))
      .mockResolvedValueOnce(mockSummaryWithData);

    render(
      <MemoryRouter>
        <AnalyticsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('analytics-error')).toBeInTheDocument();
    });

    expect(
      screen.getByText(/no se pudieron cargar los datos analíticos/i)
    ).toBeInTheDocument();

    // Pulsar botón de reintento
    const retryBtn = screen.getByRole('button', { name: /reintentar consulta/i });
    fireEvent.click(retryBtn);

    await waitFor(() => {
      expect(screen.getByTestId('analytics-content')).toBeInTheDocument();
    });
  });

  it('restablece los filtros a LAST_30_DAYS al pulsar el botón de restablecer en EmptyAnalyticsState', async () => {
    // Primera llamada con filtro que retorna vacío
    analyticsService.getAnalyticsSummary.mockResolvedValueOnce(mockEmptySummary);

    render(
      <MemoryRouter>
        <AnalyticsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('empty-analytics-state')).toBeInTheDocument();
    });

    // Simulamos respuesta con datos al restablecer
    analyticsService.getAnalyticsSummary.mockResolvedValueOnce(mockSummaryWithData);

    const resetBtn = screen.getByRole('button', { name: /restablecer filtros/i });
    fireEvent.click(resetBtn);

    await waitFor(() => {
      expect(analyticsService.getAnalyticsSummary).toHaveBeenCalledWith({
        preset: 'LAST_30_DAYS',
      });
    });
  });
});
