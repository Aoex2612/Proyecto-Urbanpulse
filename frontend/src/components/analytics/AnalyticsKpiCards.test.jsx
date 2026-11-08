import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import AnalyticsKpiCards from './AnalyticsKpiCards';

describe('AnalyticsKpiCards', () => {
  const mockOperationalMetrics = {
    totalIncidents: 42,
    resolvedIncidents: 30,
    pendingIncidents: 12,
    mtttHours: 3.5,
    mttrHours: 24.2,
    slaComplianceRate: 85.7,
    totalSlaCompliant: 36,
    totalSlaBreached: 6,
  };

  const mockAssistantPerformance = {
    totalRecommendations: 50,
    acceptedRecommendations: 45,
    adoptionRate: 90.0,
    averageConfidence: 82.4,
  };

  it('renderiza correctamente las 5 tarjetas de KPIs con sus valores formateados', () => {
    render(
      <AnalyticsKpiCards
        operationalMetrics={mockOperationalMetrics}
        assistantPerformance={mockAssistantPerformance}
      />
    );

    // 1. Total de Incidencias
    expect(screen.getByTestId('kpi-card-total')).toBeInTheDocument();
    expect(screen.getByText('42')).toBeInTheDocument();
    expect(screen.getByText(/30 resueltas/i)).toBeInTheDocument();
    expect(screen.getByText(/12 pendientes/i)).toBeInTheDocument();

    // 2. MTTT
    expect(screen.getByTestId('kpi-card-mttt')).toBeInTheDocument();
    expect(screen.getByText('3.5 h')).toBeInTheDocument();
    expect(screen.getByText(/validación y clasificación técnica inicial/i)).toBeInTheDocument();

    // 3. MTTR
    expect(screen.getByTestId('kpi-card-mttr')).toBeInTheDocument();
    expect(screen.getByText('24.2 h')).toBeInTheDocument();
    expect(screen.getByText(/cierre resolutivo por servicios operativos/i)).toBeInTheDocument();

    // 4. Cumplimiento SLA
    expect(screen.getByTestId('kpi-card-sla')).toBeInTheDocument();
    expect(screen.getByText('85.7%')).toBeInTheDocument();
    expect(screen.getByText(/cumplimiento óptimo/i)).toBeInTheDocument();
    expect(screen.getByText(/36 en plazo/i)).toBeInTheDocument();
    expect(screen.getByText(/6 fuera de plazo/i)).toBeInTheDocument();

    // 5. Asistente Inteligente
    expect(screen.getByTestId('kpi-card-assistant')).toBeInTheDocument();
    expect(screen.getByText('90.0%')).toBeInTheDocument();
    expect(screen.getByText(/82.4%/i)).toBeInTheDocument();
    expect(screen.getByText(/45 aceptadas de 50 sugeridas/i)).toBeInTheDocument();
  });

  it('acepta los datos pasados a través del prop summaryData consolidado', () => {
    const summaryData = {
      operationalMetrics: mockOperationalMetrics,
      assistantPerformance: mockAssistantPerformance,
    };

    render(<AnalyticsKpiCards summaryData={summaryData} />);

    expect(screen.getByText('42')).toBeInTheDocument();
    expect(screen.getByText('3.5 h')).toBeInTheDocument();
    expect(screen.getByText('24.2 h')).toBeInTheDocument();
    expect(screen.getByText('85.7%')).toBeInTheDocument();
    expect(screen.getByText('90.0%')).toBeInTheDocument();
  });

  it('es completamente resiliente ante valores nulos o 0 sin arrojar excepciones (RF-11)', () => {
    render(<AnalyticsKpiCards operationalMetrics={null} assistantPerformance={null} />);

    expect(screen.getByTestId('kpi-card-total')).toBeInTheDocument();
    expect(screen.getByText('0')).toBeInTheDocument();
    expect(screen.getByText(/0 resueltas/i)).toBeInTheDocument();
    expect(screen.getByText(/0 pendientes/i)).toBeInTheDocument();

    // N/A para horas cuando son nulas
    const naElements = screen.getAllByText('N/A');
    expect(naElements.length).toBeGreaterThanOrEqual(2); // MTTT y MTTR

    // Porcentajes a 0.0%
    const zeroPercentElements = screen.getAllByText('0.0%');
    expect(zeroPercentElements.length).toBeGreaterThanOrEqual(2); // SLA y adopción
  });

  it('asigna la clase e indicador semántico correspondiente según el porcentaje de SLA', () => {
    const { rerender } = render(
      <AnalyticsKpiCards
        operationalMetrics={{
          ...mockOperationalMetrics,
          slaComplianceRate: 65.0,
        }}
      />
    );

    expect(screen.getByText('En seguimiento')).toBeInTheDocument();
    expect(screen.getByTestId('kpi-card-sla')).toHaveClass('kpi-sla-warning');

    rerender(
      <AnalyticsKpiCards
        operationalMetrics={{
          ...mockOperationalMetrics,
          slaComplianceRate: 40.0,
        }}
      />
    );

    expect(screen.getByText('Atención prioritaria')).toBeInTheDocument();
    expect(screen.getByTestId('kpi-card-sla')).toHaveClass('kpi-sla-danger');
  });
});
