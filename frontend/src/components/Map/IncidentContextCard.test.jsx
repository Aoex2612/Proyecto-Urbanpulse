import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import IncidentContextCard from './IncidentContextCard';

describe('IncidentContextCard', () => {
  const mockObservations = [
    {
      id: 1,
      source: 'OPEN_METEO',
      metricType: 'TEMPERATURE_C',
      valueNum: 21.4,
      valueText: null,
      observedAt: '2026-11-08T14:30:00Z',
    },
    {
      id: 2,
      source: 'OPEN_METEO',
      metricType: 'PRECIPITATION_MM',
      valueNum: 0.0,
      valueText: null,
      observedAt: '2026-11-08T14:30:00Z',
    },
    {
      id: 3,
      source: 'OPEN_METEO',
      metricType: 'WIND_SPEED_KMH',
      valueNum: 15.2,
      valueText: null,
      observedAt: '2026-11-08T14:30:00Z',
    },
    {
      id: 4,
      source: 'MUNICIPAL_TRAFFIC',
      metricType: 'TRAFFIC_LEVEL',
      valueNum: null,
      valueText: 'FLUIDO',
      observedAt: '2026-11-08T14:30:00Z',
    },
  ];

  it('muestra mensaje informativo de degradación cuando no hay observaciones disponibles', () => {
    render(<IncidentContextCard observations={[]} />);

    expect(screen.getByText(/Contexto Ambiental y de Tráfico/i)).toBeInTheDocument();
    expect(
      screen.getByText(/No hay lecturas meteorológicas ni de tráfico registradas/i)
    ).toBeInTheDocument();
  });

  it('renderiza métricas de temperatura, precipitación, viento y tráfico correctamente', () => {
    render(<IncidentContextCard observations={mockObservations} />);

    expect(screen.getByText(/Contexto Ambiental y de Tráfico/i)).toBeInTheDocument();
    expect(screen.getByText('Temperatura')).toBeInTheDocument();
    expect(screen.getByText('21.4 °C')).toBeInTheDocument();

    expect(screen.getByText('Precipitación')).toBeInTheDocument();
    expect(screen.getByText('0.0 mm')).toBeInTheDocument();

    expect(screen.getByText('Viento')).toBeInTheDocument();
    expect(screen.getByText('15.2 km/h')).toBeInTheDocument();

    expect(screen.getByText('Tráfico')).toBeInTheDocument();
    expect(screen.getByText('FLUIDO')).toBeInTheDocument();
  });

  it('muestra la marca temporal de captura cuando está disponible', () => {
    render(<IncidentContextCard observations={mockObservations} />);

    expect(screen.getByText(/Capturado:/i)).toBeInTheDocument();
  });

  it('maneja observaciones parciales completando con valores por defecto', () => {
    const partial = [
      {
        metricType: 'TEMPERATURE_C',
        valueNum: 18.0,
      },
    ];

    render(<IncidentContextCard observations={partial} />);

    expect(screen.getByText('18.0 °C')).toBeInTheDocument();
    expect(screen.getByText('0.0 mm')).toBeInTheDocument();
    expect(screen.getByText('Normal')).toBeInTheDocument();
  });
});
