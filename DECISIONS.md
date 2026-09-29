# Decisões técnicas do Sintonia

Este documento registra as principais decisões da implementação atual, seus motivos e consequências. Ele descreve o estado efetivamente adotado no projeto; alternativas não implementadas aparecem apenas quando ajudam a explicar uma escolha.

## 1. Aplicação web em um único deploy

**Decisão:** manter o back-end em Spring Boot e o frontend em React/Vite, entregando o build da SPA como conteúdo estático do mesmo processo Java.

**Motivo:** simplificar publicação, origem, cookies de sessão, CSRF e configuração de OAuth.

**Consequências:**

- O Dockerfile compila primeiro o frontend, incorpora os artefatos ao JAR e produz uma imagem final apenas com JRE e aplicação.
- Rotas da SPA são encaminhadas para `index.html` sem interceptar rotas de API, OAuth ou WebSocket.
- Frontend e back-end são versionados e publicados juntos.

## 2. Spring Boot como monólito modular

**Decisão:** implementar autenticação, salas, presença, músicas, fila, reprodução, votação e integração com YouTube no mesmo serviço, separados por pacotes de domínio.

**Motivo:** as regras possuem transações e invariantes fortemente relacionadas; separá-las em serviços distribuídos aumentaria a complexidade sem benefício proporcional para o estágio atual.

**Consequências:**

- Operações críticas podem usar uma única transação de banco.
- O processo ainda possui componentes transitórios, como registro de conexões STOMP e timers de liberação do player.
- Escala horizontal exigiria coordenar ou externalizar esses componentes transitórios.

## 3. PostgreSQL como fonte de verdade persistente

**Decisão:** armazenar em PostgreSQL usuários, salas, memberships, presenças, atividades, músicas, itens de fila, playbacks e votos.

**Motivo:** o estado compartilhado precisa sobreviver a reinícios e manter consistência sob operações concorrentes.

**Consequências:**

- JPA/Hibernate implementa o modelo relacional e os repositórios.
- `schema.sql` complementa o mapeamento com índices e ajustes que dependem do PostgreSQL.
- Restrições relevantes são reforçadas tanto pelo domínio quanto pelo banco.
- A aplicação depende de PostgreSQL também em desenvolvimento e testes de persistência.

## 4. OAuth2/OIDC com sessão HTTP

**Decisão:** autenticar com Google OAuth2/OpenID Connect e manter a autenticação em uma sessão HTTP do Spring Security, sem JWT próprio no navegador.

**Motivo:** aproveitar o ciclo de vida e as proteções do framework, sem implementar emissão, renovação e revogação de tokens próprios.

**Consequências:**

- A identidade dos comandos vem da sessão autenticada, nunca de um `userId` escolhido pelo cliente.
- O login bem-sucedido retorna à SPA e o logout invalida a sessão.
- A publicação precisa configurar corretamente credenciais e URL de callback no Google.

## 5. CSRF explícito para a SPA

**Decisão:** proteger operações mutáveis com CSRF usando cookie `XSRF-TOKEN` e header `X-XSRF-TOKEN`.

**Motivo:** a autenticação usa cookie de sessão, enviado automaticamente pelo navegador.

**Consequências:**

- A SPA materializa e envia o token nos comandos mutáveis, inclusive logout.
- Falta ou divergência do token resulta em `403 Forbidden` antes da execução da regra de domínio.
- Endpoints de consulta continuam utilizáveis sem header CSRF.

## 6. REST para estado e comandos; WebSocket para notificações

**Decisão:** usar HTTP/REST para snapshots e comandos e WebSocket/STOMP para avisar que o estado da sala mudou.

**Motivo:** o banco e os serviços de domínio devem permanecer autoritativos; mensagens em tempo real podem se perder durante uma desconexão.

**Consequências:**

- Eventos STOMP não precisam carregar todo o estado da sala.
- Ao conectar, reconectar ou receber um evento relevante, o frontend busca novamente os snapshots REST.
- A conexão STOMP envia o `clientSessionId` para correlacionar a sessão de navegador.
- O cliente tenta reconectar após 3 segundos.

## 7. Eventos somente depois do commit

**Decisão:** publicar notificações de sala depois da confirmação da transação que alterou o estado.

**Motivo:** impedir que clientes recebam um evento e consultem dados que ainda não foram confirmados ou que serão revertidos.

**Consequências:**

- O REST continua sendo a fonte de verdade.
- Uma falha de publicação não muda o resultado já persistido; o estado pode ser recuperado no próximo snapshot ou reconnect.

## 8. Separação entre usuário, membership, presença, sessão e conexão

**Decisão:** representar separadamente:

- usuário persistente;
- membership histórico em uma sala;
- presença ativa por lease;
- sessão HTTP autenticada;
- `clientSessionId` de uma aba/janela;
- conexão WebSocket transitória.

**Motivo:** esses conceitos têm ciclos de vida diferentes. Confundi-los causaria saídas incorretas, duplicidade de participantes e perda de histórico.

**Consequências:**

- Um usuário pode abrir várias abas na mesma sala sem contar várias vezes no limite.
- Sair de uma aba remove apenas sua presença; o membership fecha quando não resta presença válida.
- Desconectar o WebSocket não encerra imediatamente a presença.
- Músicas, votos e atividades permanecem associados ao usuário e à sala depois da saída.

## 9. Presença persistida por lease

**Decisão:** persistir `RoomPresence` e renová-la por heartbeat, em vez de deduzir presença apenas da conexão WebSocket.

**Motivo:** fechamento abrupto de aba e oscilações de rede não garantem uma mensagem de saída, e uma conexão WebSocket não representa todo o contexto da sessão.

**Consequências:**

- Cada presença usa a combinação sala, usuário e `clientSessionId`.
- O lease vence após 90 segundos e o frontend envia heartbeat a cada 30 segundos.
- Uma rotina executada a cada 30 segundos expira presenças abandonadas.
- O limite de 15 pessoas considera usuários distintos com presença válida.

## 10. Ciclo de vida explícito das salas

**Decisão:** criar salas vazias, separar criação de entrada e encerrar salas ativas após 20 minutos sem presença.

**Motivo:** a criação não deve pressupor que o navegador já abriu e manteve a página da sala; salas abandonadas também não devem ficar disponíveis para sempre.

**Consequências:**

- `emptySince` registra quando a sala passou a não possuir presença válida.
- O retorno de uma presença antes do prazo limpa `emptySince`.
- Uma rotina periódica marca a sala como `CLOSED` após o prazo.
- O código único da sala não é reutilizado.

## 11. Fila e playback como estados relacionados, mas distintos

**Decisão:** usar `QueueItem` para intenção e ordem de execução e `Playback` para cada execução efetiva.

**Motivo:** uma música aguardando, uma música em execução e uma reprodução encerrada possuem dados e regras diferentes.

**Consequências:**

- A fila pública retorna somente itens `WAITING`.
- O item `PLAYING` aparece separadamente em `currentPlayback`.
- O histórico deriva de playbacks `FINISHED`, `SKIPPED` ou `ERROR`.
- Um item de fila pode originar no máximo um playback.
- O índice parcial `ux_queue_items_playing_per_room` impede mais de um `QueueItem` `PLAYING` por sala.
- Não existe uma constraint equivalente de unicidade parcial diretamente em `playbacks`; essa consistência é coordenada pelo serviço e pelo estado do item de fila.

## 12. Remoção restrita à intenção ainda pendente

**Decisão:** permitir que um participante remova apenas um item adicionado por ele e ainda no estado `WAITING`.

**Motivo:** remover uma música em execução ou já encerrada apagaria parte do estado/histórico; permitir remoção por terceiros quebraria a autoria da fila.

**Consequências:**

- Autoria inválida resulta em `403 Forbidden`.
- Estado diferente de `WAITING` resulta em `409 Conflict`.
- Itens do Auto-DJ, que não possuem usuário, não podem ser removidos como itens próprios.

## 13. Concorrência serializada por sala e playback

**Decisão:** usar transações, locks pessimistas e constraints de unicidade nas operações críticas.

**Motivo:** adições, transições de playback, votos e claims de player podem ocorrer simultaneamente.

**Consequências:**

- A posição da fila e o limite de oito itens por usuário são calculados sob lock da sala.
- Transições do playback são feitas sob lock e rejeitam estados incompatíveis.
- A unicidade do voto por usuário e playback é garantida no banco.
- Operações repetidas são idempotentes quando isso é seguro ou retornam conflito quando o estado já não permite a ação.

## 14. Dois modos de reprodução

**Decisão:** suportar `TODOS_OS_NAVEGADORES` e `CAIXA_DE_MUSICA` no mesmo modelo de playback.

**Motivo:** o produto atende tanto pessoas em locais diferentes quanto grupos ouvindo uma única saída de áudio.

**Consequências:**

- Em Todos os Navegadores, cada cliente reproduz a partir do mesmo relógio lógico; pausa é apenas local.
- Em Caixa de Música, somente uma sessão possui o claim persistido de player. Os comandos globais exigem participante presente e comparam o `clientSessionId` enviado com esse claim; a implementação atual não repete a comparação com o usuário do claim.
- Mudar para Todos os Navegadores libera o claim e retoma eventual pausa global.

## 15. Relógio lógico persistido do playback

**Decisão:** calcular a posição a partir de `startedAt`, `pausedAt` e `totalPausedMillis`, em vez de persistir atualizações frequentes de segundos.

**Motivo:** reduzir escrita no banco e permitir que clientes se sincronizem a partir de um snapshot.

**Consequências:**

- Pausa e retomada globais alteram marcos temporais do playback.
- Novos clientes conseguem calcular a posição atual sem depender de todos os eventos anteriores.
- A pausa local em Todos os Navegadores não altera o relógio global.

## 16. Claim de player associado à presença

**Decisão:** persistir no `Room` o usuário, o `clientSessionId` e o instante em que o player foi assumido.

**Motivo:** o claim precisa sobreviver a snapshots e ser validado contra a sessão específica que está presente.

**Consequências:**

- O claim exige modo Caixa de Música e presença válida da mesma combinação usuário/sessão.
- Outra sessão recebe conflito enquanto o player estiver ocupado.
- A mesma sessão pode repetir o claim de forma idempotente.
- Saída explícita ou expiração da presença libera o claim.
- A última conexão STOMP da sessão pode cair por até 15 segundos antes da liberação; reconectar nesse prazo cancela a ação.

## 17. Auto-DJ como origem sem usuário

**Decisão:** representar a origem do item por `USER` ou `AUTO_DJ` e permitir `QueueItem.user = null` para itens automáticos.

**Motivo:** atribuir recomendações a um usuário criaria autoria falsa e afetaria limites e relatórios.

**Consequências:**

- Itens automáticos não contam no limite individual de músicas em espera.
- DTOs de fila e histórico aceitam autor nulo.
- O Auto-DJ só inclui um item quando não existe `WAITING`.
- A seleção evita música atual, itens aguardando e as dez músicas recentemente reproduzidas.
- A última reprodução finalizada orienta uma busca contextual; uma música persistida é usada como fallback.

## 18. YouTube como catálogo externo

**Decisão:** buscar metadados pela YouTube Data API e reproduzir pela YouTube IFrame Player API; persistir apenas músicas efetivamente selecionadas ou usadas pelo Auto-DJ.

**Motivo:** evitar manter um catálogo próprio e reutilizar a infraestrutura pública de vídeo e playback.

**Consequências:**

- Busca e seleção dependem de chave e disponibilidade do YouTube.
- Vídeos não incorporáveis, com duração ausente ou acima de 20 minutos são rejeitados na busca.
- Falhas externas são traduzidas para erro de serviço indisponível sem invalidar o restante da sala.
- A IFrame API continua sujeita às políticas e restrições do YouTube no navegador.

## 19. Atividade persistente com snapshots

**Decisão:** persistir `RoomActivity` separadamente das notificações STOMP, incluindo snapshots escalares de nome do ator e título da música.

**Motivo:** a atividade precisa continuar legível depois de reconnect e preservar a apresentação histórica mesmo se nomes ou entidades relacionadas mudarem.

**Consequências:**

- A lista recente pode ser reconstruída exclusivamente do banco.
- IDs opcionais permitem navegação e agregação quando a entidade relacionada ainda existe.
- Os tipos registrados cobrem entrada, saída, fila, renomeação e principais transições de playback.

## 20. Resumo derivado do histórico persistido

**Decisão:** calcular o resumo da sala a partir de memberships, itens, playbacks e votos já persistidos.

**Motivo:** evitar uma segunda fonte de verdade com contadores pré-calculados e sujeitos a divergência.

**Consequências:**

- Um usuário pode consultar o resumo se já participou da sala, mesmo depois de sair.
- O relatório distingue contribuições de usuários e reproduções do Auto-DJ.
- O custo da consulta cresce com o histórico da sala e pode exigir otimização futura se o volume aumentar.

## 21. Estratégia de hospedagem

**Decisão:** usar Docker como unidade de publicação. O Render foi usado para publicar e validar a aplicação; Oracle Cloud foi avaliada, mas sua adoção foi adiada.

**Motivo:** a imagem multi-stage fornece uma forma reproduzível de compilar frontend e back-end e pode ser executada em diferentes provedores.

**Consequências:**

- A aplicação lê a porta de `PORT` e segredos de banco, YouTube e Google por variáveis de ambiente.
- A URL JDBC de produção pode ser fornecida por configuração externa do Spring, sem embutir credenciais na imagem.
- A validação no Render não implica que uma implantação em Oracle Cloud tenha sido concluída.
- Uma mudança futura de provedor deve preservar HTTPS, PostgreSQL, callback OAuth e suporte a WebSocket.
