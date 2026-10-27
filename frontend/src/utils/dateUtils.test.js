import { describe, it, expect } from 'vitest'
import { formatDateTime } from './dateUtils'

describe('dateUtils', () => {
  it('formatea correctamente una fecha ISO a formato local legible en espanol', () => {
    const isoString = '2026-03-15T10:30:00Z'
    const formatted = formatDateTime(isoString)
    expect(formatted).toBeDefined()
    expect(formatted).toContain('2026')
    expect(formatted).toContain('15')
  })

  it('retorna texto informativo cuando la fecha es nula, vacia o no valida', () => {
    expect(formatDateTime(null)).toBe('Fecha no disponible')
    expect(formatDateTime('')).toBe('Fecha no disponible')
    expect(formatDateTime(undefined)).toBe('Fecha no disponible')
  })
})
