import type {
  ActiveRoom,
  HistoryItem,
  Me,
  PlaybackMode,
  PlaybackModeState,
  QueueItem,
  Room,
  RoomActivity,
  RoomMember,
  RoomPlayer,
  RoomSummary,
  RoomState,
  SkipVote,
  Song,
  SongSearchItem,
  UserRoom,
} from './types'
import { csrfHeaders } from './lib/csrf'
import { ApiError, extractErrorMessage } from './lib/errors'

export { ApiError }

export async function logout(): Promise<void> {
  // OAuth rotates the CSRF token; a safe backend request materializes the current cookie before logout.
  await fetch('/api/me', {
    method: 'GET',
    credentials: 'same-origin',
    headers: { Accept: 'application/json' },
  })
  await request<void>('/logout', {
    method: 'POST',
  })
}

async function request<T>(url: string, options: RequestInit = {}): Promise<T> {
  const method = (options.method ?? 'GET').toUpperCase()
  const headers: Record<string, string> = { ...csrfHeaders(method, document.cookie) }

  if (options.body != null) {
    headers['Content-Type'] = 'application/json'
  }
  headers['Accept'] = 'application/json'

  let response: Response
  try {
    response = await fetch(url, {
      ...options,
      method,
      headers,
      credentials: 'same-origin',
    })
  } catch {
    throw new ApiError(0, extractErrorMessage(null, 0))
  }

  if (response.status === 204) {
    return undefined as T
  }

  const text = await response.text()
  let data: unknown = null
  if (text) {
    try {
      data = JSON.parse(text)
    } catch {
      data = null
    }
  }

  if (!response.ok) {
    throw new ApiError(response.status, extractErrorMessage(data, response.status))
  }

  return data as T
}

function jsonBody(value: unknown): string {
  return JSON.stringify(value)
}

export const api = {
  me: () => request<Me>('/api/me'),

  updateDisplayName: (displayName: string) =>
    request<Me>('/api/me', {
      method: 'PATCH',
      body: jsonBody({ displayName }),
    }),

  createRoom: (name: string) =>
    request<Room>('/rooms', { method: 'POST', body: jsonBody({ name }) }),

  renameRoom: (roomId: number, name: string) =>
    request<Room>(`/api/rooms/${roomId}`, { method: 'PATCH', body: jsonBody({ name }) }),

  listActiveRooms: () => request<ActiveRoom[]>('/api/rooms'),

  myRooms: () => request<UserRoom[]>('/api/me/rooms'),

  enterRoom: (code: string, clientSessionId: string) =>
    request<RoomMember>(`/rooms/${encodeURIComponent(code)}/members`, {
      method: 'POST',
      body: jsonBody({ clientSessionId }),
    }),

  leaveRoom: (code: string, clientSessionId: string) =>
    request<void>(`/rooms/${encodeURIComponent(code)}/members`, {
      method: 'DELETE',
      body: jsonBody({ clientSessionId }),
    }),

  renewPresence: (code: string, clientSessionId: string) =>
    request<RoomMember>(`/rooms/${encodeURIComponent(code)}/presence`, {
      method: 'POST',
      body: jsonBody({ clientSessionId }),
    }),

  roomState: (roomId: number) => request<RoomState>(`/api/rooms/${roomId}/state`),

  roomActivities: (roomId: number) =>
    request<RoomActivity[]>(`/api/rooms/${roomId}/activities`),

  roomSummary: (roomId: number) => request<RoomSummary>(`/api/rooms/${roomId}/summary`),

  searchSongs: (query: string, maxResults = 10) =>
    request<SongSearchItem[]>(
      `/api/songs/search?q=${encodeURIComponent(query)}&maxResults=${maxResults}`,
    ),

  selectSong: (videoId: string) => request<Song>(`/api/songs/${encodeURIComponent(videoId)}`),

  addToQueue: (roomId: number, songId: number) =>
    request<QueueItem>(`/api/rooms/${roomId}/queue`, {
      method: 'POST',
      body: jsonBody({ songId }),
    }),

  removeFromQueue: (roomId: number, queueItemId: number) =>
    request<void>(`/api/rooms/${roomId}/queue/${queueItemId}`, { method: 'DELETE' }),

  currentPlayer: (roomId: number) => request<RoomPlayer>(`/api/rooms/${roomId}/player`),

  claimPlayer: (roomId: number, clientSessionId: string) =>
    request<RoomPlayer>(`/api/rooms/${roomId}/player`, {
      method: 'POST',
      body: jsonBody({ clientSessionId }),
    }),

  releasePlayer: (roomId: number, clientSessionId: string) =>
    request<RoomPlayer>(`/api/rooms/${roomId}/player`, {
      method: 'DELETE',
      body: jsonBody({ clientSessionId }),
    }),

  playbackMode: (roomId: number) => request<PlaybackModeState>(`/api/rooms/${roomId}/playback-mode`),

  changePlaybackMode: (roomId: number, mode: PlaybackMode) =>
    request<PlaybackModeState>(`/api/rooms/${roomId}/playback-mode`, {
      method: 'PUT',
      body: jsonBody({ mode }),
    }),

  skipVote: (roomId: number) =>
    request<SkipVote>(`/api/rooms/${roomId}/skip-votes`, { method: 'POST' }),

  currentSkipVote: (roomId: number) => request<SkipVote>(`/api/rooms/${roomId}/skip-votes`),

  history: (roomId: number, limit = 20) =>
    request<HistoryItem[]>(`/api/rooms/${roomId}/history?limit=${limit}`),

  finishPlayback: (playbackId: number, clientSessionId?: string) =>
    request<void>(`/api/playbacks/${playbackId}/finish`, {
      method: 'POST',
      body: jsonBody({ clientSessionId }),
    }),

  errorPlayback: (playbackId: number, clientSessionId?: string) =>
    request<void>(`/api/playbacks/${playbackId}/error`, {
      method: 'POST',
      body: jsonBody({ clientSessionId }),
    }),

  pausePlayback: (playbackId: number, clientSessionId?: string) =>
    request<void>(`/api/playbacks/${playbackId}/pause`, {
      method: 'POST',
      body: jsonBody({ clientSessionId }),
    }),

  resumePlayback: (playbackId: number, clientSessionId?: string) =>
    request<void>(`/api/playbacks/${playbackId}/resume`, {
      method: 'POST',
      body: jsonBody({ clientSessionId }),
    }),
}
