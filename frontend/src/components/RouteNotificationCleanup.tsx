import { useEffect } from 'react'
import { useLocation } from 'react-router-dom'
import { clearNotifications } from '../lib/notify'

export function RouteNotificationCleanup() {
  const { pathname } = useLocation()

  useEffect(() => () => clearNotifications(), [pathname])

  return null
}
