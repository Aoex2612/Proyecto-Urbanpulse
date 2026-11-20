import { describe, it, expect } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import TerritorialChart from './TerritorialChart';

describe('TerritorialChart', () => {
  const mockTerritorialData = [
    {
      territoryName: 'Centro',
      totalIncidents: 25,
      resolvedCount: 20,
      pendingCount: 5,
      slaComplianceRate: 90.0,
    },
    {
      territoryName: 'Carretera de Cádiz',
      totalIncidents: 15,
      resolvedCount: 10,
      pendingCount: 5,
      slaComplianceRate: 75.0,
    },
    {
      territoryName: 'Cruz de Humilladero',
      totalIncidents: 10,
      resolvedCount: 8,
      pendingCount: 2,
      slaComplianceRate: 80.0,
    },
  ];

  it('renderiza la estructura SVG accesible con etiquetas de territorio y conteo de incidencias', () => {
    render(<TerritorialChart data={mockTerritorialData} />);

    // Título y accesibilidad SVG
    expect(screen.getByTestId('territorial-chart-card')).toBeInTheDocument();
    expect(screen.getByRole('img', { name: /gráfico de distribución territorial de incidencias/i })).toBeInTheDocument();

    // Comprobar etiquetas de distritos
    expect(screen.getByText('Centro')).toBeInTheDocument();
    expect(screen.getByText('Carretera de Cádiz')).toBeInTheDocument();
    expect(screen.getByText('Cruz de Humilladero')).toBeInTheDocument();

    // Comprobar valores de las barras
    expect(screen.getByText('25')).toBeInTheDocument();
    expect(screen.getByText('15')).toBeInTheDocument();
    expect(screen.getByText('10')).toBeInTheDocument();

    // Comprobar elementos de barra
    expect(screen.getByTestId('territorial-bar-centro')).toBeInTheDocument();
    expect(screen.getByTestId('territorial-bar-carretera-de-cádiz')).toBeInTheDocument();
  });

  it('muestra el tooltip interactivo al pasar el ratón sobre una demarcación territorial', () => {
    render(<TerritorialChart data={mockTerritorialData} />);

    const centroRow = screen.getByRole('graphics-symbol', {
      name: /centro: 25 incidencias/i,
    });

    fireEvent.mouseEnter(centroRow);

    // Debe mostrarse el tooltip interactivo
    expect(screen.getByRole('status')).toBeInTheDocument();
    expect(screen.getByText(/total incidencias:/i)).toBeInTheDocument();
    expect(screen.getByText(/90.0%/i)).toBeInTheDocument();

    fireEvent.mouseLeave(centroRow);
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  it('muestra estado vacío informativo cuando data está vacía o es nula', () => {
    render(<TerritorialChart data={[]} />);

    expect(screen.getByTestId('territorial-chart-empty')).toBeInTheDocument();
    expect(
      screen.getByText(/no hay datos territoriales disponibles para el período seleccionado/i)
    ).toBeInTheDocument();
  });
});
