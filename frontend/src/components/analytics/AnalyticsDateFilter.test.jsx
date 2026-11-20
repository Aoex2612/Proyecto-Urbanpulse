import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import AnalyticsDateFilter from './AnalyticsDateFilter';
import { MALAGA_DISTRICTS } from '../../utils/analyticsConstants';

describe('AnalyticsDateFilter', () => {
  const defaultFilters = {
    preset: 'LAST_30_DAYS',
    year: 2026,
    district: '',
    neighborhood: '',
  };

  it('renderiza todos los botones de presets rápidos y marca como activo LAST_30_DAYS por defecto', () => {
    const handleFilterChange = vi.fn();
    render(<AnalyticsDateFilter filters={defaultFilters} onFilterChange={handleFilterChange} />);

    expect(screen.getByTestId('preset-btn-TODAY')).toBeInTheDocument();
    expect(screen.getByTestId('preset-btn-LAST_7_DAYS')).toBeInTheDocument();
    const btn30d = screen.getByTestId('preset-btn-LAST_30_DAYS');
    expect(btn30d).toBeInTheDocument();
    expect(btn30d).toHaveClass('is-active');
    expect(btn30d).toHaveAttribute('aria-pressed', 'true');

    expect(screen.getByTestId('preset-btn-LAST_3_MONTHS')).toBeInTheDocument();
    expect(screen.getByTestId('preset-btn-LAST_6_MONTHS')).toBeInTheDocument();
    expect(screen.getByTestId('preset-btn-LAST_YEAR')).toBeInTheDocument();
    expect(screen.getByTestId('preset-btn-SPECIFIC_YEAR')).toBeInTheDocument();
  });

  it('notifica al padre cuando se pulsa un preset diferente', () => {
    const handleFilterChange = vi.fn();
    render(<AnalyticsDateFilter filters={defaultFilters} onFilterChange={handleFilterChange} />);

    fireEvent.click(screen.getByTestId('preset-btn-LAST_7_DAYS'));

    expect(handleFilterChange).toHaveBeenCalledTimes(1);
    expect(handleFilterChange).toHaveBeenCalledWith(
      expect.objectContaining({
        preset: 'LAST_7_DAYS',
      })
    );
  });

  it('muestra el selector de año cuando el preset activo es SPECIFIC_YEAR', () => {
    const handleFilterChange = vi.fn();
    render(
      <AnalyticsDateFilter
        filters={{ ...defaultFilters, preset: 'SPECIFIC_YEAR', year: 2025 }}
        onFilterChange={handleFilterChange}
      />
    );

    const yearSelect = screen.getByLabelText(/seleccionar año específico/i);
    expect(yearSelect).toBeInTheDocument();
    expect(yearSelect.value).toBe('2025');

    fireEvent.change(yearSelect, { target: { value: '2024' } });
    expect(handleFilterChange).toHaveBeenCalledWith(
      expect.objectContaining({
        preset: 'SPECIFIC_YEAR',
        year: 2024,
      })
    );
  });

  it('contiene la lista completa de distritos oficiales de Málaga y actualiza al seleccionar uno', () => {
    const handleFilterChange = vi.fn();
    render(<AnalyticsDateFilter filters={defaultFilters} onFilterChange={handleFilterChange} />);

    const districtSelect = screen.getByLabelText(/filtrar por distrito municipal/i);
    expect(districtSelect).toBeInTheDocument();

    // Comprobar que todos los distritos oficiales están como opciones
    MALAGA_DISTRICTS.forEach((d) => {
      expect(screen.getByRole('option', { name: d })).toBeInTheDocument();
    });

    fireEvent.change(districtSelect, { target: { value: 'Centro' } });

    expect(handleFilterChange).toHaveBeenCalledWith(
      expect.objectContaining({
        district: 'Centro',
        neighborhood: '',
      })
    );
  });

  it('muestra barrios correspondientes al distrito seleccionado y permite filtrar por barrio', () => {
    const handleFilterChange = vi.fn();
    render(
      <AnalyticsDateFilter
        filters={{ ...defaultFilters, district: 'Centro' }}
        onFilterChange={handleFilterChange}
      />
    );

    const neighborhoodSelect = screen.getByLabelText(/filtrar por barrio/i);
    expect(neighborhoodSelect).not.toBeDisabled();
    expect(screen.getByRole('option', { name: 'Centro Histórico' })).toBeInTheDocument();

    fireEvent.change(neighborhoodSelect, { target: { value: 'Centro Histórico' } });

    expect(handleFilterChange).toHaveBeenCalledWith(
      expect.objectContaining({
        district: 'Centro',
        neighborhood: 'Centro Histórico',
      })
    );
  });

  it('permite restablecer los filtros cuando hay filtros personalizados activos', () => {
    const handleFilterChange = vi.fn();
    const handleReset = vi.fn();
    render(
      <AnalyticsDateFilter
        filters={{ ...defaultFilters, preset: 'LAST_7_DAYS', district: 'Centro' }}
        onFilterChange={handleFilterChange}
        onReset={handleReset}
      />
    );

    const resetBtn = screen.getByRole('button', {
      name: /restablecer filtros a valores predeterminados/i,
    });
    expect(resetBtn).toBeInTheDocument();

    fireEvent.click(resetBtn);
    expect(handleReset).toHaveBeenCalledTimes(1);
  });
});
