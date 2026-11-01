import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import IncidentDetailModal from './IncidentDetailModal'
import incidentService from '../../services/incidentService'
import authService from '../../services/authService'

vi.mock('../../services/incidentService', () => ({
  default: {
    getIncidentAttachments: vi.fn(),
    getIncidentHistory: vi.fn(),
  },
}))

vi.mock('../../services/authService', () => ({
  default: {
    getCurrentUser: vi.fn(),
    getUser: vi.fn(),
  },
}))

describe('IncidentDetailModal', () => {
  const mockIncident = {
    id: 42,
    title: 'Farola con cables expuestos',
    description: 'En el cruce de calle Larios con Alameda.',
    category: 'Alumbrado',
    status: 'REPORTED',
    priority: 'HIGH',
    latitude: 36.7213,
    longitude: -4.4214,
    createdAt: '2026-10-31T09:00:00Z',
    updatedAt: '2026-10-31T09:30:00Z',
    address: 'Calle Larios, 10',
    district: 'Centro',
    citizenName: 'Juan Pérez',
  }

  beforeEach(() => {
    vi.clearAllMocks()
    incidentService.getIncidentAttachments.mockResolvedValue([])
    incidentService.getIncidentHistory.mockResolvedValue([])
    authService.getCurrentUser.mockReturnValue({ role: 'CITIZEN' })
  })

  it('no renderiza nada si isOpen es falso o la incidencia es nula', () => {
    const { container: container1 } = render(
      <IncidentDetailModal isOpen={false} incident={mockIncident} />
    )
    expect(container1.firstChild).toBeNull()

    const { container: container2 } = render(
      <IncidentDetailModal isOpen={true} incident={null} />
    )
    expect(container2.firstChild).toBeNull()
  })

  it('muestra la información y metadatos básicos de la incidencia', async () => {
    render(<IncidentDetailModal isOpen={true} incident={mockIncident} />)

    await waitFor(() => {
      expect(screen.getByText('Farola con cables expuestos')).toBeInTheDocument()
    })
    expect(screen.getByText('En el cruce de calle Larios con Alameda.')).toBeInTheDocument()
    expect(screen.getByText('Alumbrado')).toBeInTheDocument()
    expect(screen.getByText('Calle Larios, 10')).toBeInTheDocument()
    expect(screen.getByText('Juan Pérez')).toBeInTheDocument()
  })

  it('no muestra el historial de auditoría si el usuario tiene rol CITIZEN (RBAC)', async () => {
    render(
      <IncidentDetailModal
        isOpen={true}
        incident={mockIncident}
        currentUser={{ role: 'CITIZEN' }}
      />
    )

    await waitFor(() => {
      expect(screen.getByText('Farola con cables expuestos')).toBeInTheDocument()
    })
    expect(screen.queryByText(/Historial de Auditoría/i)).not.toBeInTheDocument()
    expect(incidentService.getIncidentHistory).not.toHaveBeenCalled()
  })

  it('muestra el historial de auditoría y consulta el servicio si el usuario es ADMIN (RBAC)', async () => {
    render(
      <IncidentDetailModal
        isOpen={true}
        incident={mockIncident}
        currentUser={{ role: 'ADMIN' }}
      />
    )

    await waitFor(() => {
      expect(screen.getByText(/Historial de Auditoría/i)).toBeInTheDocument()
    })
    expect(screen.getByText('Vista Administrador')).toBeInTheDocument()
    expect(incidentService.getIncidentHistory).toHaveBeenCalledWith(42)
  })

  it('llama a la función onClose al hacer clic en el botón de cerrar', async () => {
    const handleClose = vi.fn()

    render(
      <IncidentDetailModal
        isOpen={true}
        incident={mockIncident}
        onClose={handleClose}
      />
    )

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Cerrar modal de detalle/i })).toBeInTheDocument()
    })

    const closeBtn = screen.getByRole('button', { name: /Cerrar modal de detalle/i })
    fireEvent.click(closeBtn)

    expect(handleClose).toHaveBeenCalledTimes(1)
  })

  it('llama a onClose al pulsar la tecla Escape', async () => {
    const handleClose = vi.fn()

    render(
      <IncidentDetailModal
        isOpen={true}
        incident={mockIncident}
        onClose={handleClose}
      />
    )

    await waitFor(() => {
      expect(screen.getByText('Farola con cables expuestos')).toBeInTheDocument()
    })

    fireEvent.keyDown(window, { key: 'Escape' })

    expect(handleClose).toHaveBeenCalledTimes(1)
  })
})
