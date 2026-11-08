import { describe, it, expect } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import SlaComplianceChart from './SlaComplianceChart';

describe('SlaComplianceChart', () => {
  const mockDepartmentData = [
    {
      departmentId: 1,
      departmentName: 'Medio Ambiente y Limpieza',
      totalAssigned: 20,
      resolvedCount: 18,
      avgResolutionHours: 14.5,
      slaComplianceRate: 90.0,
    },
    {
      departmentId: 2,
      departmentName: 'Vías y Obras',
      totalAssigned: 15,
      resolvedCount: 10,
      avgResolutionHours: 32.0,
      slaComplianceRate: 66.7,
    },
    {
      departmentId: 3,
      departmentName: 'Alumbrado Público',
      totalAssigned: 10,
      resolvedCount: 4,
      avgResolutionHours: 56.2,
      slaComplianceRate: 40.0,
    },
  ];

  it('renderiza la estructura SVG accesible con departamentos y porcentajes de SLA', () => {
    render(<SlaComplianceChart data={mockDepartmentData} />);

    expect(screen.getByTestId('sla-chart-card')).toBeInTheDocument();
    expect(
      screen.getByRole('img', {
        name: /gráfico de cumplimiento de sla por departamento municipal/i,
      })
    ).toBeInTheDocument();

    // Departamentos
    expect(screen.getByText('Medio Ambiente y Limpieza')).toBeInTheDocument();
    expect(screen.getByText('Vías y Obras')).toBeInTheDocument();
    expect(screen.getByText('Alumbrado Público')).toBeInTheDocument();

    // Porcentajes
    expect(screen.getByText('90.0%')).toBeInTheDocument();
    expect(screen.getByText('66.7%')).toBeInTheDocument();
    expect(screen.getByText('40.0%')).toBeInTheDocument();

    // Barras con colores según umbrales de SLA
    const barMedioAmbiente = screen.getByTestId('sla-bar-medio-ambiente-y-limpieza');
    expect(barMedioAmbiente).toHaveAttribute('fill', '#10b981'); // Verde >= 80%

    const barViasObras = screen.getByTestId('sla-bar-vías-y-obras');
    expect(barViasObras).toHaveAttribute('fill', '#f59e0b'); // Ámbar 50-79%

    const barAlumbrado = screen.getByTestId('sla-bar-alumbrado-público');
    expect(barAlumbrado).toHaveAttribute('fill', '#ef4444'); // Rojo < 50%
  });

  it('muestra el tooltip interactivo al hacer hover sobre un departamento', () => {
    render(<SlaComplianceChart data={mockDepartmentData} />);

    const deptRow = screen.getByRole('graphics-symbol', {
      name: /medio ambiente y limpieza: 90.0% cumplimiento de sla/i,
    });

    fireEvent.mouseEnter(deptRow);

    expect(screen.getByRole('status')).toBeInTheDocument();
    expect(screen.getByText(/14.5 h/i)).toBeInTheDocument();
    expect(screen.getByText(/asignadas:/i)).toBeInTheDocument();

    fireEvent.mouseLeave(deptRow);
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  it('muestra estado vacío amigable cuando no hay datos departamentales', () => {
    render(<SlaComplianceChart data={[]} />);

    expect(screen.getByTestId('sla-chart-empty')).toBeInTheDocument();
    expect(
      screen.getByText(/no hay métricas departamentales disponibles para el período actual/i)
    ).toBeInTheDocument();
  });
});
