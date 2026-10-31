import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import StatusTimeline from './StatusTimeline'

describe('StatusTimeline', () => {
  it('muestra el indicador de carga cuando loading es true', () => {
    render(<StatusTimeline loading={true} />)

    expect(screen.getByRole('status')).toBeInTheDocument()
    expect(
      screen.getByText('Cargando historial de cambios de estado...')
    ).toBeInTheDocument()
  })

  it('muestra el mensaje de alerta cuando ocurre un error', () => {
    render(<StatusTimeline error="Error al conectar con la base de datos" />)

    expect(screen.getByRole('alert')).toBeInTheDocument()
    expect(screen.getByText('Error al consultar auditoría')).toBeInTheDocument()
    expect(
      screen.getByText('Error al conectar con la base de datos')
    ).toBeInTheDocument()
  })

  it('muestra el mensaje de estado inicial cuando el historial está vacío', () => {
    render(<StatusTimeline history={[]} />)

    expect(
      screen.getByText('Sin cambios de estado posteriores')
    ).toBeInTheDocument()
    expect(
      screen.getByText(/permanece en su estado inicial de reporte/i)
    ).toBeInTheDocument()
  })

  it('renderiza la lista de transiciones cronológicas con sus datos', () => {
    const mockHistory = [
      {
        id: 1,
        fromStatus: 'REPORTED',
        toStatus: 'VALIDATED',
        changedByUserName: 'Operador Triaje',
        createdAt: '2026-10-31T10:00:00Z',
        reason: 'Verificado reporte ciudadano con foto adjunta',
      },
    ]

    render(<StatusTimeline history={mockHistory} />)

    expect(
      screen.getByLabelText('Línea temporal de cambios de estado')
    ).toBeInTheDocument()
    expect(screen.getByText('Reportada')).toBeInTheDocument()
    expect(screen.getByText('Validada')).toBeInTheDocument()
    expect(screen.getByText('Operador Triaje')).toBeInTheDocument()
    expect(
      screen.getByText('Verificado reporte ciudadano con foto adjunta')
    ).toBeInTheDocument()
  })

  it('muestra texto por defecto si una transición no contiene motivo justificado', () => {
    const mockHistory = [
      {
        id: 2,
        fromStatus: 'VALIDATED',
        toStatus: 'IN_PROGRESS',
        changedByUserName: 'Tecnico Municipal',
        createdAt: '2026-10-31T11:00:00Z',
        reason: null,
      },
    ]

    render(<StatusTimeline history={mockHistory} />)

    expect(
      screen.getByText('Sin motivo justificado especificado.')
    ).toBeInTheDocument()
  })
})
