import { Client, type IMessage } from '@stomp/stompjs'
import { getOrCreateClientSessionId } from './lib/session'
import type { RoomEvent } from './types'

function websocketUrl(): string {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/ws`
}

export function connectToRoom(
  roomCode: string,
  onEvent: (event: RoomEvent) => void,
  onConnected?: () => void,
): Client {
  const client = new Client({
    brokerURL: websocketUrl(),
    connectHeaders: { clientSessionId: getOrCreateClientSessionId() },
    reconnectDelay: 3000,
    onConnect: () => {
      client.subscribe(`/topic/rooms/${roomCode}`, (message: IMessage) => {
        try {
          onEvent(JSON.parse(message.body) as RoomEvent)
        } catch {
          // Ignora mensagens que não sejam um RoomEvent válido.
        }
      })
      onConnected?.()
    },
  })

  client.activate()
  return client
}

export { getOrCreateClientSessionId }
