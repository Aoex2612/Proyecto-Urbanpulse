import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import LoginPage from './LoginPage'
import authService from '../services/authService'

vi.mock('../services/authService', () => ({
  default: {
    login: vi.fn(),
    isAuthenticated: vi.fn(),
    getUser: vi.fn(),
    getCurrentUser: vi.fn(),
  },
}))

const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  }
})

describe('LoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    authService.isAuthenticated.mockReturnValue(false)
  })

  it('renderiza correctamente el formulario con sus campos y botones principales', () => {
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    )

    expect(screen.getByRole('heading', { name: /Iniciar Sesión/i })).toBeInTheDocument()
    expect(screen.getByLabelText(/Correo electrónico/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/Contraseña/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Iniciar Sesión/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Regístrate aquí/i })).toBeInTheDocument()
  })

  it('muestra mensajes de error si se envía el formulario con los campos vacíos', async () => {
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    )

    const submitBtn = screen.getByRole('button', { name: /Iniciar Sesión/i })
    fireEvent.click(submitBtn)

    expect(await screen.findByText('El correo electrónico es obligatorio.')).toBeInTheDocument()
    expect(screen.getByText('La contraseña es obligatoria.')).toBeInTheDocument()
    expect(authService.login).not.toHaveBeenCalled()
  })

  it('valida que el formato del correo electrónico sea válido', async () => {
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    )

    fireEvent.change(screen.getByLabelText(/Correo electrónico/i), {
      target: { value: 'correo-no-valido' },
    })
    fireEvent.change(screen.getByLabelText(/Contraseña/i), {
      target: { value: 'Password123' },
    })

    fireEvent.click(screen.getByRole('button', { name: /Iniciar Sesión/i }))

    expect(
      await screen.findByText('Introduce un formato de correo electrónico válido.')
    ).toBeInTheDocument()
    expect(authService.login).not.toHaveBeenCalled()
  })

  it('muestra un mensaje de error si las credenciales son incorrectas (HTTP 401)', async () => {
    authService.login.mockRejectedValueOnce({
      response: { status: 401 },
    })

    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    )

    fireEvent.change(screen.getByLabelText(/Correo electrónico/i), {
      target: { value: 'ciudadano@urbanpulse.es' },
    })
    fireEvent.change(screen.getByLabelText(/Contraseña/i), {
      target: { value: 'ContrasenaIncorrecta' },
    })

    fireEvent.click(screen.getByRole('button', { name: /Iniciar Sesión/i }))

    expect(
      await screen.findByText('Correo electrónico o contraseña incorrectos.')
    ).toBeInTheDocument()
  })

  it('inicia sesión con éxito y redirige a la raíz (/)', async () => {
    authService.login.mockResolvedValueOnce({
      token: 'fake-jwt-token',
      user: { email: 'ciudadano@urbanpulse.es', role: 'CITIZEN' },
    })

    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    )

    fireEvent.change(screen.getByLabelText(/Correo electrónico/i), {
      target: { value: 'ciudadano@urbanpulse.es' },
    })
    fireEvent.change(screen.getByLabelText(/Contraseña/i), {
      target: { value: 'Citizen1234' },
    })

    fireEvent.click(screen.getByRole('button', { name: /Iniciar Sesión/i }))

    await waitFor(() => {
      expect(authService.login).toHaveBeenCalledWith('ciudadano@urbanpulse.es', 'Citizen1234')
      expect(mockNavigate).toHaveBeenCalledWith('/', { replace: true })
    })
  })

  it('redirige a / si el usuario ya se encuentra previamente autenticado', () => {
    authService.isAuthenticated.mockReturnValueOnce(true)

    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    )

    expect(mockNavigate).toHaveBeenCalledWith('/', { replace: true })
  })
})
