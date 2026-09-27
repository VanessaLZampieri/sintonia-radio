import { describe, expect, it } from 'vitest'
import { backendProxy } from '../vite.config'

describe('Vite backend proxy', () => {
  it('encaminha o logout para o Spring Security', () => {
    expect(backendProxy['/logout']).toEqual({
      target: 'http://localhost:8080',
      changeOrigin: true,
    })
  })
})
