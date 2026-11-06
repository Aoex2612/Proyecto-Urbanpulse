import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import ResolveIncidentModal from './ResolveIncidentModal';
import triageService from '../../services/triageService';

vi.mock('../../services/triageService', () => ({
  default: {
    resolveIncident: vi.fn(),
  },
}));

describe('ResolveIncidentModal', () => {
  const mockMediumIncident = {
    id: 15,
    title: 'Semáforo averiado en cruce principal',
    category: 'Tráfico',
    priority: 'MEDIUM',
    status: 'IN_PROGRESS',
  };

  const mockLowIncident = {
    id: 16,
    title: 'Papelera deteriorada en paseo marítimo',
    category: 'Mobiliario Urbano',
    priority: 'LOW',
    status: 'IN_PROGRESS',
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('no renderiza nada si isOpen es falso o la incidencia es nula', () => {
    const { container: container1 } = render(
      <ResolveIncidentModal isOpen={false} incident={mockMediumIncident} onClose={vi.fn()} />
    );
    expect(container1.firstChild).toBeNull();

    const { container: container2 } = render(
      <ResolveIncidentModal isOpen={true} incident={null} onClose={vi.fn()} />
    );
    expect(container2.firstChild).toBeNull();
  });

  it('muestra el título, prioridad y aviso de obligatoriedad para prioridad MEDIUM', () => {
    render(
      <ResolveIncidentModal
        isOpen={true}
        incident={mockMediumIncident}
        onClose={vi.fn()}
      />
    );

    expect(screen.getByText(/Resolución Técnica • Incidencia #15/i)).toBeInTheDocument();
    expect(screen.getByText('Semáforo averiado en cruce principal')).toBeInTheDocument();
    expect(screen.getByText(/Prioridad: Media/i)).toBeInTheDocument();
    expect(screen.getByText(/es obligatorio detallar las actuaciones técnicas/i)).toBeInTheDocument();
  });

  it('valida que la descripción de actuaciones tenga al menos 10 caracteres para prioridad no menor', async () => {
    render(
      <ResolveIncidentModal
        isOpen={true}
        incident={mockMediumIncident}
        onClose={vi.fn()}
      />
    );

    const submitBtn = screen.getByRole('button', { name: /Confirmar Resolución/i });
    const textarea = screen.getByLabelText(/Memoria de Actuaciones Realizadas/i);

    // Inicialmente deshabilitado porque no alcanza 10 caracteres
    expect(submitBtn).toBeDisabled();

    // Escribir 6 caracteres (insuficiente)
    fireEvent.change(textarea, { target: { value: 'Arreglo' } });
    expect(submitBtn).toBeDisabled();
    expect(screen.getByText(/7 \/ 10 caracteres mín\./i)).toBeInTheDocument();

    // Escribir 12 caracteres (válido)
    fireEvent.change(textarea, {
      target: { value: 'Sustituida placa de control y cableado' },
    });
    expect(submitBtn).toBeEnabled();
  });

  it('permite resolver sin memoria de actuaciones para incidencias de prioridad LOW', () => {
    render(
      <ResolveIncidentModal
        isOpen={true}
        incident={mockLowIncident}
        onClose={vi.fn()}
      />
    );

    const submitBtn = screen.getByRole('button', { name: /Confirmar Resolución/i });
    expect(submitBtn).toBeEnabled();

    expect(screen.getByText(/la descripción de actuaciones es opcional/i)).toBeInTheDocument();
  });

  it('llama a triageService.resolveIncident y emite onIncidentResolved al confirmar', async () => {
    const handleResolved = vi.fn();
    const handleClose = vi.fn();

    const resolvedData = {
      ...mockMediumIncident,
      status: 'RESOLVED',
    };

    triageService.resolveIncident.mockResolvedValueOnce(resolvedData);

    render(
      <ResolveIncidentModal
        isOpen={true}
        incident={mockMediumIncident}
        onClose={handleClose}
        onIncidentResolved={handleResolved}
      />
    );

    const textarea = screen.getByLabelText(/Memoria de Actuaciones Realizadas/i);
    const submitBtn = screen.getByRole('button', { name: /Confirmar Resolución/i });

    fireEvent.change(textarea, {
      target: { value: 'Sustituida luminaria LED y comprobada tensión nominal' },
    });

    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(triageService.resolveIncident).toHaveBeenCalledWith(15, {
        resolutionNotes: 'Sustituida luminaria LED y comprobada tensión nominal',
      });
    });

    expect(handleResolved).toHaveBeenCalledWith(resolvedData);
    expect(handleClose).toHaveBeenCalledTimes(1);
  });

  it('muestra un mensaje de error si falla la llamada al servicio de resolución', async () => {
    triageService.resolveIncident.mockRejectedValueOnce({
      response: {
        data: {
          message: 'Solo el técnico asignado activamente o un administrador puede resolver la incidencia',
        },
      },
    });

    render(
      <ResolveIncidentModal
        isOpen={true}
        incident={mockMediumIncident}
        onClose={vi.fn()}
      />
    );

    const textarea = screen.getByLabelText(/Memoria de Actuaciones Realizadas/i);
    const submitBtn = screen.getByRole('button', { name: /Confirmar Resolución/i });

    fireEvent.change(textarea, {
      target: { value: 'Inspección y ajuste de fusibles completada' },
    });

    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(
        screen.getByText(/Solo el técnico asignado activamente o un administrador puede resolver la incidencia/i)
      ).toBeInTheDocument();
    });
  });

  it('cierra el modal al hacer clic en Cancelar o pulsar la tecla Escape', () => {
    const handleClose = vi.fn();

    render(
      <ResolveIncidentModal
        isOpen={true}
        incident={mockMediumIncident}
        onClose={handleClose}
      />
    );

    const cancelBtn = screen.getByRole('button', { name: /Cancelar/i });
    fireEvent.click(cancelBtn);
    expect(handleClose).toHaveBeenCalledTimes(1);

    fireEvent.keyDown(window, { key: 'Escape' });
    expect(handleClose).toHaveBeenCalledTimes(2);
  });
});
