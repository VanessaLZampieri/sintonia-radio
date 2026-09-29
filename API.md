# API — Sintonia

Este documento descreve os endpoints HTTP utilizados pela versão atual do Sintonia. Os comandos e snapshots usam REST; as notificações em tempo real são tratadas separadamente por WebSocket/STOMP.

## 1. Convenções gerais

### Autenticação

- Todas as rotas `/api/**`, `/rooms/**` e `/ws/**` exigem usuário autenticado.
- A identidade é obtida pelo Spring Security; o cliente não envia `userId` para representar o usuário atual.
- Uma requisição protegida sem autenticação recebe `401 Unauthorized`, sem redirecionamento para login.
- O `clientSessionId` não substitui a autenticação. Ele é um UUID que identifica um contexto específico de navegador e é usado em presença, player, playback e STOMP.

### CSRF

- Operações mutáveis exigem proteção CSRF.
- A SPA recebe o token no cookie `XSRF-TOKEN` e o envia no header `X-XSRF-TOKEN`.
- Token ausente ou inválido recebe `403 Forbidden`.

### Erros

Os erros de domínio tratados pelo back-end usam, em geral:

```json
{
  "message": "Descrição do erro."
}
```

Status usados com frequência:

- `400 Bad Request`: body, parâmetro, nome ou `clientSessionId` inválido;
- `401 Unauthorized`: sessão não autenticada;
- `403 Forbidden`: usuário sem participação/autorização, autoria inválida ou sessão que não é o player;
- `404 Not Found`: sala, usuário, música, item ou playback inexistente;
- `409 Conflict`: sala fechada/cheia, estado incompatível ou conflito de regra de negócio;
- `503 Service Unavailable`: falha ao acessar o YouTube.

Os erros específicos listados nas seções seguintes se somam às regras transversais de autenticação e CSRF.

## 2. Rotas de autenticação

Estas rotas são fornecidas pelo Spring Security e pelo encaminhamento da SPA; não fazem parte dos 30 endpoints REST de negócio.

### GET `/login`

Rota pública da página de login. É encaminhada internamente para `/index.html`.

### GET `/oauth2/authorization/google`

Inicia o fluxo OAuth2/OpenID Connect e redireciona o navegador para o Google.

### GET `/login/oauth2/code/google`

Callback do Google. Em caso de sucesso, redireciona para `/`; em caso de falha, redireciona para `/login?error=oauth`.

### POST `/logout`

Invalida a sessão autenticada.

**Response:** `204 No Content`.

Requer CSRF. Sem token válido, retorna `403` e não conclui o logout.

## 3. Usuário autenticado

### GET `/api/me`

Retorna o usuário atual.

```json
{
  "id": 7,
  "name": "Nome completo",
  "displayName": "Nome",
  "email": "usuario@exemplo.com",
  "avatarUrl": "https://..."
}
```

**Erros:** `404` se o registro interno não existir.

### PATCH `/api/me`

Altera o nome exibido. O valor é normalizado e deve possuir entre 2 e 20 caracteres.

**Request:**

```json
{
  "displayName": "Novo nome"
}
```

**Response:** `200 OK` com `MeResponse`.

**Erros:** `400` nome inválido; `404` usuário inexistente.

### GET `/api/me/rooms`

Retorna as salas ainda `ACTIVE` das quais o usuário já participou, sem duplicar salas com mais de um membership.

```json
[
  {
    "roomId": 1,
    "name": "Sala da turma",
    "code": "A1B2C3D4",
    "status": "ACTIVE",
    "canEnter": true,
    "participantCount": 4
  }
]
```

As salas são ordenadas pela participação mais recente. `canEnter` é calculado pela situação atual da sala e pelo limite de participantes.

## 4. Salas e descoberta

### POST `/rooms`

Cria uma sala ativa e inicialmente vazia. Criar não registra automaticamente a entrada do usuário.

**Request:**

```json
{
  "name": "Sala da turma"
}
```

O nome deve possuir entre 3 e 40 caracteres após normalização.

**Response:** `201 Created`.

```json
{
  "id": 1,
  "code": "A1B2C3D4",
  "name": "Sala da turma",
  "status": "ACTIVE",
  "createdAt": "2026-09-21T14:30:00Z"
}
```

**Erros:** `400` nome ou body inválido.

### GET `/api/rooms`

Lista salas `ACTIVE` com pelo menos uma presença válida, da mais recente para a mais antiga.

```json
[
  {
    "roomId": 1,
    "name": "Sala da turma",
    "code": "A1B2C3D4",
    "participantCount": 4,
    "waitingCount": 3,
    "nowPlaying": {
      "title": "Título",
      "youtubeVideoId": "videoId",
      "thumbnailUrl": "https://...",
      "addedBy": {
        "userId": 7,
        "displayName": "Nome",
        "avatarUrl": "https://..."
      }
    }
  }
]
```

`nowPlaying` e seu `addedBy` podem ser `null`.

### PATCH `/api/rooms/{roomId}`

Renomeia uma sala. Exige que o usuário esteja atualmente presente.

**Request:**

```json
{
  "name": "Novo nome"
}
```

**Response:** `200 OK` com `RoomResponse`.

**Erros:** `400` nome inválido; `403` usuário não presente; `404` sala/usuário; `409` sala fechada.

### GET `/api/rooms/{roomId}/state`

Retorna o snapshot autoritativo da sala.

```json
{
  "roomId": 1,
  "roomCode": "A1B2C3D4",
  "name": "Sala da turma",
  "status": "ACTIVE",
  "members": [
    {
      "userId": 7,
      "displayName": "Nome",
      "avatarUrl": "https://...",
      "waitingCount": 2
    }
  ],
  "playbackMode": "TODOS_OS_NAVEGADORES",
  "player": null,
  "currentPlayback": {
    "playbackId": 31,
    "queueItemId": 15,
    "startedAt": "2026-09-21T14:30:00Z",
    "paused": false,
    "positionSeconds": 42,
    "song": {
      "youtubeVideoId": "videoId",
      "title": "Título",
      "thumbnailUrl": "https://...",
      "duration": "PT3M32S"
    },
    "addedByUserId": 7,
    "addedByDisplayName": "Nome",
    "source": "USER"
  },
  "queue": [
    {
      "queueItemId": 16,
      "position": 4,
      "status": "WAITING",
      "song": {
        "youtubeVideoId": "outroVideo",
        "title": "Próxima música",
        "thumbnailUrl": "https://...",
        "duration": "PT4M10S"
      },
      "addedByUserId": null,
      "source": "AUTO_DJ"
    }
  ],
  "skipVote": {
    "votes": 1,
    "requiredVotes": 2,
    "currentUserVoted": true
  }
}
```

`player`, `currentPlayback` e `skipVote` podem ser `null`. `queue` contém somente itens `WAITING`; o item em reprodução aparece exclusivamente em `currentPlayback`. Itens encerrados aparecem no histórico.

**Erros:** `404` sala; `409` sala fechada.

### GET `/api/rooms/{roomId}/members`

Lista usuários com presença válida, em ordem de entrada.

```json
[
  {
    "userId": 7,
    "displayName": "Nome",
    "avatarUrl": "https://...",
    "waitingCount": 2
  }
]
```

**Erros:** `404` sala; `409` sala fechada.

### GET `/api/rooms/{roomId}/activities`

Retorna até 50 atividades persistidas, da mais recente para a mais antiga.

```json
[
  {
    "id": 10,
    "type": "SONG_ADDED",
    "actorUserId": 7,
    "actorDisplayName": "Nome",
    "songId": 42,
    "songTitle": "Título",
    "detail": null,
    "createdAt": "2026-09-21T14:30:00Z"
  }
]
```

Tipos atuais: `MEMBER_JOINED`, `MEMBER_LEFT`, `SONG_ADDED`, `SONG_REMOVED`, `ROOM_RENAMED`, `PLAYBACK_STARTED`, `PLAYBACK_FINISHED` e `PLAYBACK_SKIPPED`.

Esta atividade é persistida e não deve ser confundida com eventos transitórios do WebSocket.

**Erros:** `404` sala.

### GET `/api/rooms/{roomId}/summary`

Retorna o resumo para um usuário que já tenha participado da sala.

```json
{
  "roomId": 1,
  "playbackCount": 12,
  "participantCount": 5,
  "skippedCount": 2,
  "autoDjPlaybackCount": 3,
  "skipVoteCount": 8,
  "contributions": [
    {
      "userId": 7,
      "displayName": "Nome",
      "avatarUrl": "https://...",
      "addedCount": 4,
      "playedCount": 3,
      "skippedCount": 1,
      "skipVoteCount": 2
    }
  ]
}
```

As contagens de playback consideram os registros de playback existentes para a sala. `participantCount` considera usuários históricos distintos.

**Erros:** `403` usuário nunca participou; `404` sala.

## 5. Participação e presença

Os três endpoints usam o mesmo body:

```json
{
  "clientSessionId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### POST `/rooms/{code}/members`

Entra na sala e cria ou reutiliza o membership aberto. Também cria ou renova o lease da presença informada.

**Response:** `200 OK` com `RoomMemberResponse`.

```json
{
  "id": 12,
  "roomId": 1,
  "roomCode": "A1B2C3D4",
  "userId": 7,
  "joinedAt": "2026-09-21T14:30:00Z",
  "leftAt": null
}
```

**Erros:** `400` UUID inválido; `404` sala/usuário; `409` sala fechada ou cheia.

### POST `/rooms/{code}/presence`

Heartbeat que cria ou renova a presença da sessão. O lease atual é de 90 segundos.

**Response:** `200 OK` com `RoomMemberResponse`.

**Erros:** os mesmos da entrada.

### DELETE `/rooms/{code}/members`

Remove somente a presença correspondente ao `clientSessionId`. O membership recebe `leftAt` apenas quando não resta presença válida do usuário.

**Response:** `200 OK` com `RoomMemberResponse`, ou `204 No Content` quando a presença não existe.

**Erros:** `400` UUID inválido; `404` sala/usuário.

## 6. Busca e seleção de músicas

### GET `/api/songs/search`

Pesquisa vídeos elegíveis no YouTube.

**Query:** `q` obrigatório; `maxResults` opcional, padrão `10`, mínimo `1`, máximo `25`.

```json
[
  {
    "videoId": "videoId",
    "title": "Título",
    "channelTitle": "Canal",
    "thumbnailUrl": "https://...",
    "duration": "PT3M32S"
  }
]
```

São descartados vídeos não incorporáveis, vídeos sem duração válida e vídeos acima de 20 minutos.

**Erros:** `400` query inválida; `503` YouTube indisponível.

### GET `/api/songs/{youtubeVideoId}`

Obtém ou persiste os dados completos da música selecionada.

```json
{
  "id": 42,
  "youtubeVideoId": "videoId",
  "title": "Título",
  "thumbnailUrl": "https://...",
  "duration": "PT3M32S"
}
```

**Erros:** `404` vídeo não encontrado; `409` duração não permitida; `503` YouTube indisponível.

## 7. Fila

### GET `/api/rooms/{roomId}/queue`

Retorna somente os itens que ainda estão em `WAITING`, ordenados por posição e ID.

Não retorna `PLAYING`, `FINISHED`, `SKIPPED` ou `ERROR`. O item em execução é obtido pelo snapshot da sala e os estados encerrados pelo histórico.

```json
[
  {
    "id": 15,
    "position": 3,
    "addedAt": "2026-09-21T14:30:00Z",
    "song": {
      "id": 42,
      "youtubeVideoId": "videoId",
      "title": "Título",
      "thumbnailUrl": "https://...",
      "duration": "PT3M32S"
    },
    "addedBy": {
      "id": 7,
      "name": "Nome completo",
      "avatarUrl": "https://..."
    }
  }
]
```

Itens do Auto-DJ não possuem usuário e retornam `addedBy: null`.

**Erros:** `404` sala; `409` sala fechada.

### POST `/api/rooms/{roomId}/queue`

Adiciona uma música do usuário à fila e verifica se é necessário iniciar um playback.

**Request:**

```json
{
  "songId": 42
}
```

**Response:** `201 Created` com `QueueItemResponse`.

**Erros:** `400` `songId` inválido; `403` usuário não presente; `404` sala/música; `409` sala fechada, duplicidade ativa, duração acima de 20 minutos ou limite de oito itens `WAITING`.

### DELETE `/api/rooms/{roomId}/queue/{queueItemId}`

Remove um item da fila. O usuário deve estar presente, ser o autor do item e o item deve continuar em `WAITING`.

**Response:** `200 OK`, sem body.

**Erros:** `403` usuário não presente ou não é o autor; `404` sala/item; `409` sala fechada ou item fora de `WAITING`.

## 8. Playback e histórico

Os comandos de playback aceitam body opcional:

```json
{
  "clientSessionId": "550e8400-e29b-41d4-a716-446655440000"
}
```

No modo Caixa de Música, o usuário deve estar presente e o `clientSessionId` enviado deve ser igual ao claim atual do player. A autorização desses comandos não compara separadamente o usuário autenticado com `player.userId`. Em Todos os Navegadores, pausa e retomada são locais e os endpoints globais correspondentes são rejeitados.

### POST `/api/playbacks/{playbackId}/finish`

Finaliza normalmente e tenta iniciar a próxima música. Retorna `200 OK`, sem body.

### POST `/api/playbacks/{playbackId}/error`

Finaliza como erro e tenta iniciar a próxima música. Retorna `200 OK`, sem body.

### POST `/api/playbacks/{playbackId}/pause`

Pausa globalmente no modo Caixa de Música. Retorna `200 OK`, sem body.

### POST `/api/playbacks/{playbackId}/resume`

Retoma globalmente no modo Caixa de Música. Retorna `200 OK`, sem body.

**Erros dos comandos:** `403` usuário não presente ou `clientSessionId` diferente do claim do player; `404` playback; `409` playback fora de `PLAYING` ou comando incompatível com o modo.

### GET `/api/rooms/{roomId}/history`

Retorna apenas playbacks encerrados: `FINISHED`, `SKIPPED` e `ERROR`.

**Query:** `limit` opcional, padrão `20`, mínimo `1`, máximo `100`.

```json
[
  {
    "playbackId": 31,
    "queueItemId": 15,
    "song": {
      "id": 42,
      "youtubeVideoId": "videoId",
      "title": "Título",
      "thumbnailUrl": "https://...",
      "duration": "PT3M32S"
    },
    "startedAt": "2026-09-21T14:30:00Z",
    "endedAt": "2026-09-21T14:33:32Z",
    "status": "FINISHED",
    "addedBy": null
  }
]
```

`addedBy` é nulo para itens do Auto-DJ.

**Erros:** `400` limite inválido; `404` sala; `409` sala fechada.

## 9. Votação para pular

### POST `/api/rooms/{roomId}/skip-votes`

Registra o voto do usuário atual. Não possui body.

```json
{
  "playbackId": 31,
  "votes": 2,
  "requiredVotes": 3,
  "skipped": false
}
```

Ao atingir o arredondamento para cima de 60% dos participantes presentes, o playback é pulado.

**Erros:** `403` usuário não presente; `404` sala/playback; `409` sala fechada ou voto duplicado.

### GET `/api/rooms/{roomId}/skip-votes`

Retorna o estado da votação atual no mesmo formato.

**Erros:** `404` sala ou ausência de playback; `409` sala fechada.

## 10. Modo de reprodução

Valores: `TODOS_OS_NAVEGADORES` e `CAIXA_DE_MUSICA`.

### GET `/api/rooms/{roomId}/playback-mode`

```json
{
  "roomId": 1,
  "mode": "TODOS_OS_NAVEGADORES"
}
```

**Erros:** `404` sala; `409` sala fechada.

### PUT `/api/rooms/{roomId}/playback-mode`

Altera o modo. Exige participante presente.

```json
{
  "mode": "CAIXA_DE_MUSICA"
}
```

**Response:** `200 OK` com `PlaybackModeResponse`.

Mudar para Todos os Navegadores libera o player e retoma eventual pausa global.

**Erros:** `400` modo inválido; `403` usuário não presente; `404` sala; `409` sala fechada.

## 11. Player da sala

### GET `/api/rooms/{roomId}/player`

Retorna o claim atual:

```json
{
  "roomId": 1,
  "clientSessionId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": 7,
  "assumedAt": "2026-09-21T14:30:00Z"
}
```

Sem player, `roomId` permanece preenchido e os demais campos são nulos.

**Erros:** `404` sala; `409` sala fechada.

### POST `/api/rooms/{roomId}/player`

Assume o player no modo Caixa de Música.

```json
{
  "clientSessionId": "550e8400-e29b-41d4-a716-446655440000"
}
```

O claim exige presença válida do mesmo usuário e `clientSessionId`. Repetir pela mesma sessão é idempotente.

**Response:** `200 OK` com `RoomPlayerResponse`.

**Erros:** `400` UUID inválido; `403` sessão sem presença; `404` sala/usuário; `409` sala fechada, modo incompatível ou player ocupado.

### DELETE `/api/rooms/{roomId}/player`

Libera o player associado ao `clientSessionId` informado.

```json
{
  "clientSessionId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response:** `200 OK` com `RoomPlayerResponse`. Sem player atual, a operação é idempotente.

**Erros:** `400` UUID inválido; `403` sessão diferente do player; `404` sala; `409` sala fechada.

## 12. Resumo dos endpoints REST

| Método | Endpoint | Finalidade |
|---|---|---|
| GET | `/api/me` | Consultar usuário atual |
| PATCH | `/api/me` | Alterar `displayName` |
| GET | `/api/me/rooms` | Listar salas do usuário |
| POST | `/rooms` | Criar sala |
| GET | `/api/rooms` | Descobrir salas ativas |
| PATCH | `/api/rooms/{roomId}` | Renomear sala |
| GET | `/api/rooms/{roomId}/state` | Consultar snapshot da sala |
| GET | `/api/rooms/{roomId}/members` | Listar participantes presentes |
| GET | `/api/rooms/{roomId}/activities` | Consultar atividades |
| GET | `/api/rooms/{roomId}/summary` | Consultar resumo |
| POST | `/rooms/{code}/members` | Entrar na sala |
| POST | `/rooms/{code}/presence` | Renovar presença |
| DELETE | `/rooms/{code}/members` | Sair com uma sessão |
| GET | `/api/songs/search` | Buscar músicas |
| GET | `/api/songs/{youtubeVideoId}` | Selecionar música |
| GET | `/api/rooms/{roomId}/queue` | Consultar itens `WAITING` |
| POST | `/api/rooms/{roomId}/queue` | Adicionar música |
| DELETE | `/api/rooms/{roomId}/queue/{queueItemId}` | Remover música própria `WAITING` |
| POST | `/api/playbacks/{playbackId}/finish` | Finalizar playback |
| POST | `/api/playbacks/{playbackId}/error` | Informar erro |
| POST | `/api/playbacks/{playbackId}/pause` | Pausar playback global |
| POST | `/api/playbacks/{playbackId}/resume` | Retomar playback global |
| GET | `/api/rooms/{roomId}/history` | Consultar histórico encerrado |
| POST | `/api/rooms/{roomId}/skip-votes` | Registrar voto |
| GET | `/api/rooms/{roomId}/skip-votes` | Consultar votação |
| GET | `/api/rooms/{roomId}/playback-mode` | Consultar modo |
| PUT | `/api/rooms/{roomId}/playback-mode` | Alterar modo |
| GET | `/api/rooms/{roomId}/player` | Consultar player |
| POST | `/api/rooms/{roomId}/player` | Assumir player |
| DELETE | `/api/rooms/{roomId}/player` | Liberar player |
