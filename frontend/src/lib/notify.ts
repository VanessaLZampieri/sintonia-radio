import { Notify } from 'notiflix'

Notify.init({
  position: 'right-bottom',
  distance: '16px',
  width: '320px',
  borderRadius: '12px',
  timeout: 4000,
  fontSize: '15px',
  cssAnimation: true,
  cssAnimationDuration: 300,
  cssAnimationStyle: 'fade',
  closeButton: false,
  clickToClose: true,
  showOnlyTheLastOne: true,
  useIcon: true,
  success: {
    background: '#1c1322',
    textColor: '#f5f2f7',
    notiflixIconColor: '#ff5fa8',
  },
  failure: {
    background: '#1c1322',
    textColor: '#f5f2f7',
    notiflixIconColor: '#ff6b6b',
  },
  warning: {
    background: '#1c1322',
    textColor: '#f5f2f7',
    notiflixIconColor: '#ffb74d',
  },
  info: {
    background: '#1c1322',
    textColor: '#f5f2f7',
    notiflixIconColor: '#b9a5ff',
  },
})

export function notifySuccess(message: string): void {
  Notify.success(message)
  applyAccessibility('status', 'polite')
}

export function notifyFailure(message: string): void {
  Notify.failure(message)
  applyAccessibility('alert', 'assertive')
}

export function notifyInfo(message: string): void {
  Notify.info(message)
  applyAccessibility('status', 'polite')
}

export function notifyWarning(message: string): void {
  Notify.warning(message)
  applyAccessibility('alert', 'assertive')
}

export function clearNotifications(): void {
  document.getElementById('NotiflixNotifyWrap')?.remove()
  document.getElementById('NotiflixNotifyOverlay')?.remove()
}

function applyAccessibility(role: 'alert' | 'status', live: 'assertive' | 'polite'): void {
  const notification = document.getElementById('NotiflixNotifyWrap')?.lastElementChild
  notification?.setAttribute('role', role)
  notification?.setAttribute('aria-live', live)
}
