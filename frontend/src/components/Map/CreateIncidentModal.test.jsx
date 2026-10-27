import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import CreateIncidentModal from './CreateIncidentModal'
import incidentService from '../../services/incidentService'

vi.mock('../../services/incidentService', () => ({
  default: {
    createIncident: vi.fn(),
    uploadIncidentAttachment: vi.fn(),
  },
}))

describe('CreateIncidentModal', () => {
  const mockLocation = {
    latitude: 36.7213,
    longitude: -4.4214,
  }

  beforeEach(() => {
    vi.clearAllMocks()
    globalThis.URL.createObjectURL = vi.fn(() => 'blob:http://localhost/fake-preview')
    globalThis.URL.revokeObjectURL = vi.fn()
  })

  it('permite crear una incidencia sin adjuntar ninguna imagen (0 adjuntos)', async () => {
    const handleCreated = vi.fn()
    const handleClose = vi.fn()

    incidentService.createIncident.mockResolvedValueOnce({
      id: 99,
      title: 'Semáforo averiado',
      description: 'El semáforo parpadea constantemente en ámbar.',
      category: 'Alumbrado',
      status: 'REPORTED',
      priority: 'MEDIUM',
    })

    render(
      <CreateIncidentModal
        isOpen={true}
        selectedLocation={mockLocation}
        onCreated={handleCreated}
        onClose={handleClose}
      />
    )

    fireEvent.change(screen.getByLabelText(/Título/i), {
      target: { value: 'Semáforo averiado' },
    })

    fireEvent.change(screen.getByLabelText(/Categoría/i), {
      target: { value: 'Alumbrado' },
    })

    fireEvent.change(screen.getByLabelText(/Descripción/i), {
      target: { value: 'El semáforo parpadea constantemente en ámbar.' },
    })

    const submitBtn = screen.getByRole('button', { name: /Guardar incidencia/i })
    fireEvent.click(submitBtn)

    await waitFor(() => {
      expect(incidentService.createIncident).toHaveBeenCalledTimes(1)
      expect(incidentService.createIncident).toHaveBeenCalledWith({
        title: 'Semáforo averiado',
        description: 'El semáforo parpadea constantemente en ámbar.',
        category: 'Alumbrado',
        latitude: 36.7213,
        longitude: -4.4214,
      })
      // No debe haberse llamado a uploadIncidentAttachment
      expect(incidentService.uploadIncidentAttachment).not.toHaveBeenCalled()
      expect(handleCreated).toHaveBeenCalledTimes(1)
    })
  })

  it('muestra un error si el usuario selecciona un archivo con formato no permitido', async () => {
    render(
      <CreateIncidentModal
        isOpen={true}
        selectedLocation={mockLocation}
      />
    )

    const fileInput = screen.getByLabelText(/Seleccionar imagen adjunta/i)
    const invalidFile = new File(['dummy content'], 'report.pdf', { type: 'application/pdf' })

    fireEvent.change(fileInput, { target: { files: [invalidFile] } })

    expect(
      await screen.findByText(/Solo se aceptan imágenes JPEG y PNG/i)
    ).toBeInTheDocument()
  })

  it('permite adjuntar imagen válida, previsualizarla y enviarla tras crear la incidencia', async () => {
    const handleCreated = vi.fn()

    incidentService.createIncident.mockResolvedValueOnce({
      id: 101,
      title: 'Bache con fotografía',
      description: 'Gran socavón en el asfalto junto a la acera.',
      category: 'Vías Públicas',
    })

    incidentService.uploadIncidentAttachment.mockResolvedValueOnce({
      id: 5,
      incidentId: 101,
      fileName: 'bache.jpg',
      fileUrl: '/uploads/uuid-bache.jpg',
      mimeType: 'image/jpeg',
    })

    render(
      <CreateIncidentModal
        isOpen={true}
        selectedLocation={mockLocation}
        onCreated={handleCreated}
      />
    )

    fireEvent.change(screen.getByLabelText(/Título/i), {
      target: { value: 'Bache con fotografía' },
    })

    fireEvent.change(screen.getByLabelText(/Categoría/i), {
      target: { value: 'Vías Públicas' },
    })

    fireEvent.change(screen.getByLabelText(/Descripción/i), {
      target: { value: 'Gran socavón en el asfalto junto a la acera.' },
    })

    const fileInput = screen.getByLabelText(/Seleccionar imagen adjunta/i)
    const validFile = new File(['fake image data'], 'bache.jpg', { type: 'image/jpeg' })

    fireEvent.change(fileInput, { target: { files: [validFile] } })

    // Debe mostrar la vista previa
    expect(screen.getByAltText(/Vista previa de la fotografía adjunta/i)).toBeInTheDocument()
    expect(screen.getByText('bache.jpg')).toBeInTheDocument()

    const submitBtn = screen.getByRole('button', { name: /Guardar incidencia/i })
    fireEvent.click(submitBtn)

    await waitFor(() => {
      expect(incidentService.createIncident).toHaveBeenCalledTimes(1)
      expect(incidentService.uploadIncidentAttachment).toHaveBeenCalledTimes(1)
      expect(incidentService.uploadIncidentAttachment).toHaveBeenCalledWith(101, validFile)
      expect(handleCreated).toHaveBeenCalledTimes(1)
    })
  })
})
