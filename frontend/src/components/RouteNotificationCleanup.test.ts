// @vitest-environment jsdom

import { act, createElement } from 'react'
import { createRoot } from 'react-dom/client'
import { MemoryRouter, useNavigate } from 'react-router-dom'
import { afterEach, describe, expect, it } from 'vitest'
import { RouteNotificationCleanup } from './RouteNotificationCleanup'
import { clearNotifications, notifySuccess } from '../lib/notify'

(globalThis as typeof globalThis & { IS_REACT_ACT_ENVIRONMENT: boolean }).IS_REACT_ACT_ENVIRONMENT = true

function NavigationFixture() {
  const navigate = useNavigate()
  return createElement('button', { onClick: () => navigate('/room/ABCDEFGH') }, 'Entrar')
}

describe('RouteNotificationCleanup', () => {
  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('remove notificações da página anterior ao mudar de rota', () => {
    const host = document.createElement('div')
    document.body.appendChild(host)
    const root = createRoot(host)
    act(() => {
      root.render(createElement(MemoryRouter, { initialEntries: ['/'] },
        createElement(RouteNotificationCleanup),
        createElement(NavigationFixture)))
    })
    const notification = document.createElement('div')
    notification.id = 'NotiflixNotifyWrap'
    document.body.appendChild(notification)

    act(() => {
      host.querySelector('button')?.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    })

    expect(document.getElementById('NotiflixNotifyWrap')).toBeNull()
    act(() => root.unmount())
  })

  it('expõe notificações para tecnologias assistivas', () => {
    notifySuccess('Música adicionada à fila.')

    const notification = document.getElementById('NotiflixNotifyWrap')?.lastElementChild
    expect(notification?.getAttribute('role')).toBe('status')
    expect(notification?.getAttribute('aria-live')).toBe('polite')
    clearNotifications()
  })
})
