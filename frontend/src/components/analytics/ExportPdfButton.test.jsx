import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import ExportPdfButton from './ExportPdfButton';
import * as reportUtils from '../../utils/reportUtils';

describe('ExportPdfButton', () => {
  const mockUser = {
    id: 1,
    fullName: 'Elena Analista',
    email: 'elena@urbanpulse.es',
    role: 'ANALYST',
  };

  const mockSummaryWithData = {
    operationalMetrics: {
      totalIncidents: 1,
      resolvedIncidents: 1,
      mtttHours: 2.0,
      mttrHours: 24.0,
      slaComplianceRate: 100.0,
    },
    filteredIncidents: [
      {
        id: 1,
        title: 'Bache en Calle Larios',
        category: 'VIALIDAD',
        district: 'Centro',
        priority: 'HIGH',
        status: 'RESOLVED',
        createdAt: '2026-10-10T10:00:00Z',
      },
    ],
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('se renderiza deshabilitado con título explicativo cuando summaryData es nulo (RF-14)', () => {
    render(<ExportPdfButton summaryData={null} currentUser={mockUser} />);

    const button = screen.getByRole('button', {
      name: /Exportar informe ejecutivo en formato PDF/i,
    });

    expect(button).toBeDisabled();
    expect(button).toHaveAttribute(
      'title',
      'Se requieren datos para exportar el informe en PDF'
    );
  });

  it('se renderiza deshabilitado cuando filteredIncidents es una lista vacía (RF-14)', () => {
    render(
      <ExportPdfButton
        summaryData={{ filteredIncidents: [] }}
        currentUser={mockUser}
      />
    );

    const button = screen.getByRole('button', {
      name: /Exportar informe ejecutivo en formato PDF/i,
    });

    expect(button).toBeDisabled();
    expect(button).toHaveAttribute(
      'title',
      'Se requieren datos para exportar el informe en PDF'
    );
  });

  it('se renderiza habilitado cuando hay incidencias disponibles en summaryData', () => {
    render(
      <ExportPdfButton
        summaryData={mockSummaryWithData}
        currentUser={mockUser}
      />
    );

    const button = screen.getByRole('button', {
      name: /Exportar informe ejecutivo en formato PDF/i,
    });

    expect(button).toBeEnabled();
    expect(button).toHaveAttribute('title', 'Exportar informe en formato PDF');
    expect(screen.getByText('Exportar Informe PDF')).toBeInTheDocument();
  });

  it('respeta la prop disabled cuando se fuerza externamente a true', () => {
    render(
      <ExportPdfButton
        summaryData={mockSummaryWithData}
        currentUser={mockUser}
        disabled={true}
      />
    );

    const button = screen.getByRole('button', {
      name: /Exportar informe ejecutivo en formato PDF/i,
    });

    expect(button).toBeDisabled();
  });

  it('dispara la generación del PDF y los callbacks onSuccess al hacer clic', async () => {
    const mockDoc = { internal: { getNumberOfPages: () => 1 } };
    const generateSpy = vi
      .spyOn(reportUtils, 'generateAnalyticsPdf')
      .mockReturnValue(mockDoc);

    const handleSuccess = vi.fn();

    render(
      <ExportPdfButton
        summaryData={mockSummaryWithData}
        currentUser={mockUser}
        pdfOptions={{ filename: 'mi-informe.pdf' }}
        onSuccess={handleSuccess}
      />
    );

    const button = screen.getByRole('button', {
      name: /Exportar informe ejecutivo en formato PDF/i,
    });

    await act(async () => {
      fireEvent.click(button);
    });

    // Esperar a que se complete la pausa asíncrona y la invocación de generación
    await waitFor(() => {
      expect(generateSpy).toHaveBeenCalledWith(
        mockSummaryWithData,
        mockUser,
        { filename: 'mi-informe.pdf' }
      );
    });

    expect(handleSuccess).toHaveBeenCalledWith(mockDoc);
    expect(button).toBeEnabled();
    expect(screen.getByText('Exportar Informe PDF')).toBeInTheDocument();
  });

  it('muestra feedback de generación y spinner mientras procesa el PDF', async () => {
    // Retrasar artificialmente la generación para verificar el estado visual
    vi.spyOn(reportUtils, 'generateAnalyticsPdf').mockImplementation(() => {
      return { internal: { getNumberOfPages: () => 1 } };
    });

    render(
      <ExportPdfButton
        summaryData={mockSummaryWithData}
        currentUser={mockUser}
      />
    );

    const button = screen.getByRole('button');

    await act(async () => {
      fireEvent.click(button);
    });

    // Inmediatamente después del click y antes de resolver, debe estar en generación
    expect(button).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByText(/Generando PDF.../i)).toBeInTheDocument();

    await waitFor(() => {
      expect(button).toHaveAttribute('aria-busy', 'false');
      expect(screen.getByText('Exportar Informe PDF')).toBeInTheDocument();
    });
  });

  it('muestra mensaje de alerta accesible y llama onError si la generación falla', async () => {
    const mockError = new Error('Fallo crítico al compilar jspdf');
    vi.spyOn(reportUtils, 'generateAnalyticsPdf').mockImplementation(() => {
      throw mockError;
    });

    const handleError = vi.fn();

    render(
      <ExportPdfButton
        summaryData={mockSummaryWithData}
        currentUser={mockUser}
        onError={handleError}
      />
    );

    const button = screen.getByRole('button');

    await act(async () => {
      fireEvent.click(button);
    });

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(
        'Fallo crítico al compilar jspdf'
      );
    });

    expect(handleError).toHaveBeenCalledWith(mockError);
    expect(button).toBeEnabled();
  });
});
