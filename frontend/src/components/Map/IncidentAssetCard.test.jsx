import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import IncidentAssetCard from './IncidentAssetCard';
import incidentService from '../../services/incidentService';

vi.mock('../../services/incidentService', () => ({
  default: {
    confirmIncidentAsset: vi.fn(),
    removeIncidentAsset: vi.fn(),
  },
}));

describe('IncidentAssetCard', () => {
  const mockAssets = [
    {
      assetId: 101,
      assetCode: 'SEM-001',
      name: 'Semáforo Cruce Larios',
      type: 'SEMAFORO',
      distanceMeters: 12.4,
      confidenceScore: 0.752,
      isConfirmed: false,
    },
    {
      assetId: 102,
      assetCode: 'FUE-002',
      name: 'Fuente Plaza Constitución',
      type: 'FUENTE',
      distanceMeters: 45.0,
      confidenceScore: 0.1,
      isConfirmed: true,
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('muestra mensaje informativo cuando la lista de activos está vacía', () => {
    render(<IncidentAssetCard incidentId={1} assets={[]} canManage={false} />);

    expect(screen.getByText(/Activos Urbanos Cercanos/i)).toBeInTheDocument();
    expect(
      screen.getByText(/No se han detectado elementos de mobiliario ni infraestructura urbana/i)
    ).toBeInTheDocument();
  });

  it('muestra activos vinculados con sus detalles de distancia, confianza y tipo', () => {
    render(<IncidentAssetCard incidentId={1} assets={mockAssets} canManage={false} />);

    expect(screen.getByText('Semáforo Cruce Larios')).toBeInTheDocument();
    expect(screen.getByText('SEM-001')).toBeInTheDocument();
    expect(screen.getByText('Tipo: SEMAFORO')).toBeInTheDocument();
    expect(screen.getByText('📍 12.4 m')).toBeInTheDocument();
    expect(screen.getByText('Confianza: 75%')).toBeInTheDocument();
    expect(screen.getByText('Sugerido')).toBeInTheDocument();

    expect(screen.getByText('Fuente Plaza Constitución')).toBeInTheDocument();
    expect(screen.getByText('FUE-002')).toBeInTheDocument();
    expect(screen.getByText('✓ Confirmado')).toBeInTheDocument();
  });

  it('no muestra botones de gestión (confirmar/desvincular) a usuarios no autorizados (canManage=false)', () => {
    render(<IncidentAssetCard incidentId={1} assets={mockAssets} canManage={false} />);

    expect(screen.queryByRole('button', { name: /confirmar/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /desvincular/i })).not.toBeInTheDocument();
    expect(screen.queryByText('Gestión de Operador')).not.toBeInTheDocument();
  });

  it('muestra badge de gestión y botones de confirmación/desvinculación a operadores (canManage=true)', () => {
    render(<IncidentAssetCard incidentId={1} assets={mockAssets} canManage={true} />);

    expect(screen.getByText('Gestión de Operador')).toBeInTheDocument();
    // Activo 101 no está confirmado -> botón de confirmar disponible
    expect(screen.getByRole('button', { name: /confirmar activo semáforo cruce larios/i })).toBeInTheDocument();
    // Ambos activos tienen opción de desvincular
    const removeButtons = screen.getAllByRole('button', { name: /desvincular activo/i });
    expect(removeButtons).toHaveLength(2);
  });

  it('permite a un operador confirmar un activo con feedback satisfactorio', async () => {
    incidentService.confirmIncidentAsset.mockResolvedValueOnce({
      assetId: 101,
      isConfirmed: true,
    });
    const handleUpdate = vi.fn();

    render(
      <IncidentAssetCard
        incidentId={42}
        assets={mockAssets}
        canManage={true}
        onAssetUpdated={handleUpdate}
      />
    );

    const confirmBtn = screen.getByRole('button', { name: /confirmar activo semáforo cruce larios/i });
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(incidentService.confirmIncidentAsset).toHaveBeenCalledWith(42, 101);
      expect(screen.getByText(/Activo urbano confirmado correctamente/i)).toBeInTheDocument();
      expect(handleUpdate).toHaveBeenCalledTimes(1);
    });
  });

  it('permite a un operador desvincular un activo con feedback satisfactorio', async () => {
    incidentService.removeIncidentAsset.mockResolvedValueOnce({});
    const handleUpdate = vi.fn();

    render(
      <IncidentAssetCard
        incidentId={42}
        assets={mockAssets}
        canManage={true}
        onAssetUpdated={handleUpdate}
      />
    );

    const removeBtn = screen.getByRole('button', { name: /desvincular activo semáforo cruce larios/i });
    fireEvent.click(removeBtn);

    await waitFor(() => {
      expect(incidentService.removeIncidentAsset).toHaveBeenCalledWith(42, 101);
      expect(screen.getByText(/Asociación de activo eliminada correctamente/i)).toBeInTheDocument();
      expect(handleUpdate).toHaveBeenCalledTimes(1);
    });
  });

  it('muestra mensaje de error cuando falla la confirmación', async () => {
    incidentService.confirmIncidentAsset.mockRejectedValueOnce({
      response: { data: { message: 'Asociación no encontrada' } },
    });

    render(
      <IncidentAssetCard
        incidentId={42}
        assets={mockAssets}
        canManage={true}
      />
    );

    const confirmBtn = screen.getByRole('button', { name: /confirmar activo semáforo cruce larios/i });
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(screen.getByText('Asociación no encontrada')).toBeInTheDocument();
    });
  });
});
