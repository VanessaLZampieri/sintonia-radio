const CLIENT_SESSION_KEY = 'sintonia.clientSessionId'
const WINDOW_NAME_PREFIX = 'sintonia-client:'

export function getOrCreateClientSessionId(): string {
  if (typeof window !== 'undefined') {
    if (window.name.startsWith(WINDOW_NAME_PREFIX)) {
      return window.name.slice(WINDOW_NAME_PREFIX.length)
    }
    const id = crypto.randomUUID()
    window.name = `${WINDOW_NAME_PREFIX}${id}`
    sessionStorage.setItem(CLIENT_SESSION_KEY, id)
    return id
  }

  let id = sessionStorage.getItem(CLIENT_SESSION_KEY)
  if (!id) {
    id = crypto.randomUUID()
    sessionStorage.setItem(CLIENT_SESSION_KEY, id)
  }
  return id
}
