// @vitest-environment jsdom

import { afterEach, describe, expect, it, vi } from 'vitest'
import { logout } from './api'

describe('logout', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('envia o POST ao endpoint do Spring com cookie e header CSRF', async () => {
    document.cookie = 'XSRF-TOKEN=; Max-Age=0; path=/'
    const fetchMock = vi.fn()
      .mockImplementationOnce(async () => {
        document.cookie = 'XSRF-TOKEN=logout-token; path=/'
        return { status: 200, ok: true }
      })
      .mockResolvedValueOnce({ status: 204, ok: true })
    vi.stubGlobal('fetch', fetchMock)

    await logout()

    expect(fetchMock).toHaveBeenNthCalledWith(1, '/api/me', {
      method: 'GET',
      headers: { Accept: 'application/json' },
      credentials: 'same-origin',
    })
    expect(fetchMock).toHaveBeenNthCalledWith(2, '/logout', {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'X-XSRF-TOKEN': 'logout-token',
      },
      credentials: 'same-origin',
    })
  })

  it('não trata uma resposta HTTP sem sucesso como logout concluído', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce({ status: 200, ok: true })
      .mockResolvedValueOnce({
        status: 403,
        ok: false,
        text: async () => '',
      })
    vi.stubGlobal('fetch', fetchMock)

    await expect(logout()).rejects.toMatchObject({ status: 403 })
  })
})
