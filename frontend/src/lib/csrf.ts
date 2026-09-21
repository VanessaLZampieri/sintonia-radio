const XSRF_COOKIE = 'XSRF-TOKEN'
const XSRF_HEADER = 'X-XSRF-TOKEN'

export function parseCookie(name: string, cookieString: string): string | null {
  const match = cookieString.match(new RegExp('(?:^|; )' + name + '=([^;]*)'))
  if (!match) {
    return null
  }
  try {
    return decodeURIComponent(match[1])
  } catch {
    return match[1]
  }
}

export function requiresCsrf(method: string): boolean {
  const normalized = method.toUpperCase()
  return normalized !== 'GET' && normalized !== 'HEAD' && normalized !== 'OPTIONS'
}

export function csrfHeaders(method: string, cookieString: string): Record<string, string> {
  const token = parseCookie(XSRF_COOKIE, cookieString)
  if (token && requiresCsrf(method)) {
    return { [XSRF_HEADER]: token }
  }
  return {}
}
