import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import PriorityAssistantModal from './PriorityAssistantModal';
import triageService from '../../services/triageService';

vi.mock('../../services/triageService', () => ({
  default: {
    calculatePriority: vi.fn(),
    updatePriority: vi.fn(),
  },
}));

describe('PriorityAssistantModal', () => {
  const mockIncident = {
    id: 15,
    title: 'Hundimiento en calzada',
    priority: 'MEDIUM',
    priorityReason: 'Prioridad inicial asignada por defecto.',
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('no renderiza nada si isOpen es falso o incident es nulo', () => {
    const { container: c1 } = render(
      <PriorityAssistantModal isOpen={false} incident={mockIncident} onClose={vi.fn()} />
    );
    expect(c1.firstChild).toBeNull();

    const { container: c2 } = render(
      <PriorityAssistantModal isOpen={true} incident={null} onClose={vi.fn()} />
    );
    expect(c2.firstChild).toBeNull();
  });

  it('renderiza el modal accesible con la información del triaje', () => {
    render(
      <PriorityAssistantModal isOpen={true} incident={mockIncident} onClose={vi.fn()} />
    );

    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByText(/Triaje Operativo • Incidencia #15/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Corte de vía o calle cortada/i)).not.toBeChecked();
    expect(screen.getByLabelText(/Riesgo para la seguridad física o peatonal/i)).toHaveValue('NONE');
  });

  it('calcula la prioridad sugerida al hacer clic en "Calcular prioridad sugerida"', async () => {
    triageService.calculatePriority.mockResolvedValueOnce({
      suggestedPriority: 'CRITICAL',
      priorityReason: 'Prioridad CRITICAL: corte de vía pública con riesgo alto de seguridad.',
      streetCut: true,
      safetyRisk: 'HIGH',
    });

    render(
      <PriorityAssistantModal isOpen={true} incident={mockIncident} onClose={vi.fn()} />
    );

    // Marcar corte de vía
    const streetCutCheckbox = screen.getByLabelText(/Corte de vía o calle cortada/i);
    fireEvent.click(streetCutCheckbox);
    expect(streetCutCheckbox).toBeChecked();

    // Seleccionar riesgo HIGH
    const riskSelect = screen.getByLabelText(/Riesgo para la seguridad física o peatonal/i);
    fireEvent.change(riskSelect, { target: { value: 'HIGH' } });
    expect(riskSelect).toHaveValue('HIGH');

    // Pulsar calcular
    const calcButton = screen.getByRole('button', { name: /Calcular prioridad sugerida/i });
    fireEvent.click(calcButton);

    await waitFor(() => {
      expect(triageService.calculatePriority).toHaveBeenCalledTimes(1);
      expect(triageService.calculatePriority).toHaveBeenCalledWith(15, {
        streetCut: true,
        safetyRisk: 'HIGH',
      });
    });

    // Verificar visualización de la sugerencia en el badge y en el campo de texto
    await waitFor(() => {
      expect(screen.getByText('Crítica')).toBeInTheDocument();
      expect(
        screen.getAllByText(/Prioridad CRITICAL: corte de vía pública con riesgo alto de seguridad./i)[0]
      ).toBeInTheDocument();
    });

    // Comprobar que los campos de fijación definitiva se han actualizado con la sugerencia
    const finalPrioritySelect = screen.getByLabelText(/Prioridad a aplicar/i);
    expect(finalPrioritySelect).toHaveValue('CRITICAL');
    const finalReasonTextarea = screen.getByLabelText(/Justificación de la prioridad/i);
    expect(finalReasonTextarea).toHaveValue(
      'Prioridad CRITICAL: corte de vía pública con riesgo alto de seguridad.'
    );
  });

  it('permite al operador modificar la prioridad y justificación y confirmarla', async () => {
    const onPriorityUpdated = vi.fn();
    const onClose = vi.fn();

    const mockUpdatedIncident = {
      ...mockIncident,
      priority: 'HIGH',
      priorityReason: 'Vía con afluencia comercial importante.',
    };
    triageService.updatePriority.mockResolvedValueOnce(mockUpdatedIncident);

    render(
      <PriorityAssistantModal
        isOpen={true}
        incident={mockIncident}
        onPriorityUpdated={onPriorityUpdated}
        onClose={onClose}
      />
    );

    // Ajustar prioridad a HIGH
    const prioritySelect = screen.getByLabelText(/Prioridad a aplicar/i);
    fireEvent.change(prioritySelect, { target: { value: 'HIGH' } });

    // Modificar motivo
    const reasonTextarea = screen.getByLabelText(/Justificación de la prioridad/i);
    fireEvent.change(reasonTextarea, {
      target: { value: 'Vía con afluencia comercial importante.' },
    });

    // Confirmar
    const confirmButton = screen.getByRole('button', { name: /Confirmar Prioridad/i });
    fireEvent.click(confirmButton);

    await waitFor(() => {
      expect(triageService.updatePriority).toHaveBeenCalledWith(15, {
        priority: 'HIGH',
        priorityReason: 'Vía con afluencia comercial importante.',
      });
      expect(onPriorityUpdated).toHaveBeenCalledWith(mockUpdatedIncident);
      expect(onClose).toHaveBeenCalled();
    });
  });

  it('muestra un mensaje de error si falla el cálculo asistido', async () => {
    triageService.calculatePriority.mockRejectedValueOnce({
      response: { data: { message: 'Fallo interno en el cálculo de prioridad' } },
    });

    render(
      <PriorityAssistantModal isOpen={true} incident={mockIncident} onClose={vi.fn()} />
    );

    const calcButton = screen.getByRole('button', { name: /Calcular prioridad sugerida/i });
    fireEvent.click(calcButton);

    await waitFor(() => {
      expect(
        screen.getByText(/Fallo interno en el cálculo de prioridad/i)
      ).toBeInTheDocument();
    });
  });

  it('cierra el modal al pulsar el botón de cerrar o la tecla Escape', () => {
    const onClose = vi.fn();
    render(
      <PriorityAssistantModal isOpen={true} incident={mockIncident} onClose={onClose} />
    );

    const closeBtn = screen.getByRole('button', { name: /Cerrar asistente de prioridad/i });
    fireEvent.click(closeBtn);
    expect(onClose).toHaveBeenCalledTimes(1);

    fireEvent.keyDown(window, { key: 'Escape' });
    expect(onClose).toHaveBeenCalledTimes(2);
  });
});
