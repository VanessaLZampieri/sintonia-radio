// @vitest-environment jsdom

import { act, createElement } from 'react'
import { createRoot } from 'react-dom/client'
import { describe, expect, it } from 'vitest'
import { Brand } from './Brand'
import { LogoMark } from './LogoMark'

const APPROVED_WAVE_PATH = 'M2 19c7 0 7-10 14-10s7 19 14 19S37 4 45 4s7 28 15 28S67 9 75 9s7 10 17 10';

(globalThis as typeof globalThis & { IS_REACT_ACT_ENVIRONMENT: boolean }).IS_REACT_ACT_ENVIRONMENT = true

describe('LogoMark', () => {
  it('usa o mesmo waveform orgânico na marca compacta', () => {
    const host = document.createElement('div')
    const root = createRoot(host)

    act(() => root.render(createElement(Brand)))

    expect(host.querySelector('.brand-wave path')?.getAttribute('d')).toBe(APPROVED_WAVE_PATH)
    expect(host.querySelector('.brand-wave rect')).toBeNull()
    act(() => root.unmount())
  })

  it('permite reutilizar a marca oficial em tamanhos diferentes', () => {
    const host = document.createElement('div')
    const root = createRoot(host)

    act(() => root.render(createElement(LogoMark, { className: 'login-wave' })))

    expect(host.querySelector('.login-wave path')?.getAttribute('d')).toBe(APPROVED_WAVE_PATH)
    act(() => root.unmount())
  })
})
