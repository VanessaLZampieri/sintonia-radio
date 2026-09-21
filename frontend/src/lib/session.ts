const CLIENT_SESSION_KEY = 'sintonia.clientSessionId'

export function getOrCreateClientSessionId(): string {
  let id = sessionStorage.getItem(CLIENT_SESSION_KEY)
  if (!id) {
    id = crypto.randomUUID()
    sessionStorage.setItem(CLIENT_SESSION_KEY, id)
  }
  return id
}
