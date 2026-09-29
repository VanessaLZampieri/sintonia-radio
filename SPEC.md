# Especificação do projeto Sintonia

## 1. Visão geral

O Sintonia é uma rádio compartilhada pela web. Pessoas autenticadas podem criar ou acessar salas, montar uma fila de músicas, acompanhar a reprodução, votar para pular faixas e receber atualizações em tempo real.

O sistema possui dois modos de reprodução:

- **Todos os navegadores:** cada participante pode ouvir a música em seu próprio navegador.
- **Caixa de música:** apenas um navegador fica responsável pelo áudio, enquanto os demais continuam interagindo com a sala.

As músicas são pesquisadas pela YouTube Data API e reproduzidas pela YouTube IFrame Player API. O back-end é a fonte principal das regras e do estado compartilhado.

## 2. Usuários e autenticação

### História de usuário

Como usuário, quero entrar no Sintonia com minha conta Google para ser identificado sem criar uma nova senha.

### Regras de negócio

- O acesso às funcionalidades da aplicação exige autenticação com Google por OAuth2/OpenID Connect.
- Cada pessoa autenticada possui um registro interno, identificado nas relações do banco por seu ID interno.
- O sistema armazena identificador Google, nome, e-mail e imagem de perfil, mas não armazena a senha Google.
- Cada usuário possui um `displayName`, inicialmente derivado do primeiro nome recebido do Google.
- O `displayName` pode ser alterado e deve possuir entre 2 e 20 caracteres após a remoção de espaços externos.
- A autenticação é mantida por sessão HTTP no servidor.
- O logout deve ser feito por uma requisição protegida por CSRF, invalidar a sessão e impedir novos acessos autenticados.
- Após login bem-sucedido, o usuário retorna à página inicial. Falhas de OAuth são apresentadas na página de login.

## 3. Salas

### Histórias de usuário

Como usuário autenticado, quero criar uma sala para iniciar uma rádio compartilhada.

Como usuário autenticado, quero entrar em uma sala existente por seu código.

Como participante, quero identificar e renomear a sala em que estou.

### Regras de negócio

- Cada sala possui um nome obrigatório entre 3 e 40 caracteres.
- Qualquer participante ativo pode renomear a sala.
- Cada sala possui um código único de exatamente 8 caracteres, formado por letras e números não ambíguos.
- O código não diferencia letras maiúsculas de minúsculas e é normalizado internamente para maiúsculas.
- Criar uma sala e entrar nela são operações separadas.
- Uma sala recém-criada começa ativa e vazia; o navegador criador entra ao abrir a página da sala.
- A sala não possui dono com privilégios especiais.
- Uma sala comporta no máximo 15 usuários presentes ao mesmo tempo.
- Quando a última presença termina, a sala permanece disponível por 20 minutos.
- Se alguém retornar nesse período, a sala deixa de ser considerada vazia.
- Se ninguém retornar, a sala é encerrada pela rotina periódica de ciclo de vida.
- Uma sala encerrada não pode ser reativada e seu código não é reutilizado.

## 4. Membership, presença e sessões

### História de usuário

Como usuário, quero entrar, sair e reconectar a uma sala sem apagar as contribuições e o histórico já produzidos.

### Conceitos

- **Membership:** registro histórico de que um usuário participou de uma sala, com horário de entrada e, quando aplicável, de saída.
- **Presença ativa:** lease temporário que indica que uma sessão de navegador ainda está presente na sala.
- **Sessão HTTP:** mantém a autenticação do usuário no servidor.
- **`clientSessionId`:** UUID que identifica um contexto específico de navegador, como uma aba ou janela.
- **Conexão WebSocket:** conexão transitória usada para receber notificações; não substitui a presença nem a sessão HTTP.

### Regras de negócio

- A presença é persistida por combinação de sala, usuário e `clientSessionId`.
- Cada presença possui um lease de 90 segundos.
- O navegador renova sua presença por heartbeat HTTP a cada 30 segundos.
- Uma rotina executada a cada 30 segundos encerra presenças expiradas.
- O mesmo usuário pode manter várias presenças na mesma sala, por exemplo em mais de uma aba.
- Um usuário com várias presenças conta uma única vez no limite de participantes.
- Sair em uma aba remove somente a presença daquele `clientSessionId`.
- O membership é encerrado apenas quando não resta presença válida para o usuário na sala.
- Fechar a aba sem enviar a saída é tratado pela expiração do lease.
- Uma desconexão temporária do WebSocket não representa saída da sala.
- A saída ou expiração não remove músicas, votos, atividades ou histórico persistidos.

## 5. Descoberta e participantes

### Histórias de usuário

Como usuário, quero encontrar salas em uso e retornar às salas das quais já participei.

Como participante, quero ver quem está atualmente presente na sala.

### Regras de negócio

- A descoberta global apresenta somente salas `ACTIVE` com pelo menos uma presença válida.
- As salas ativas são ordenadas da criação mais recente para a mais antiga.
- A descoberta informa nome, código, participantes presentes, quantidade de itens `WAITING` e música atual, quando houver.
- A lista de salas do usuário contém somente salas ainda `ACTIVE` das quais ele já participou.
- Salas repetidas por diferentes memberships aparecem uma única vez, respeitando a participação mais recente.
- A lista de participantes contém apenas usuários com presença válida.
- Para cada participante são apresentados `displayName`, avatar e quantidade de músicas próprias em `WAITING`.

## 6. Busca e seleção de músicas

### História de usuário

Como participante, quero pesquisar músicas para escolher o que adicionar à rádio.

### Regras de negócio

- A busca utiliza a YouTube Data API.
- A busca aceita entre 1 e 25 resultados e retorna título, canal, thumbnail e duração.
- A busca descarta vídeos não incorporáveis e vídeos sem duração válida.
- A duração máxima aceita é de 20 minutos, inclusive.
- Buscar não persiste automaticamente todos os resultados.
- Ao selecionar um vídeo, o sistema consulta os detalhes e persiste a música somente quando necessário.
- Uma música já conhecida pode ser reutilizada, mas sua duração continua sujeita à regra de 20 minutos.
- Falhas da API do YouTube devem ser tratadas sem travar o restante da aplicação.

## 7. Fila de músicas

### Histórias de usuário

Como participante, quero adicionar músicas e visualizar a ordem das próximas faixas.

Como participante, quero remover uma música minha enquanto ela ainda aguarda reprodução.

### Regras de negócio

- A fila atual contém somente itens no estado `WAITING`.
- O item `PLAYING` é representado separadamente pelo playback atual.
- Itens `FINISHED`, `SKIPPED` ou `ERROR` não fazem parte da fila atual; suas reproduções pertencem ao histórico.
- A fila segue a ordem de posição e usa o ID como desempate.
- Cada usuário pode possuir no máximo 8 músicas próprias em `WAITING` na mesma sala.
- Itens do Auto-DJ não possuem usuário e não contam para esse limite.
- A mesma música não pode aparecer simultaneamente em `WAITING` ou `PLAYING` mais de uma vez na sala.
- Depois que uma música deixa os estados ativos, ela pode ser adicionada novamente, respeitando as regras do Auto-DJ.
- Somente um participante ativo pode alterar a fila.
- Um participante só pode remover um item adicionado por ele próprio e que ainda esteja em `WAITING`.
- Itens do Auto-DJ não podem ser removidos como se pertencessem a um participante.
- A saída do usuário não remove automaticamente as músicas que ele adicionou.
- Operações simultâneas não podem corromper a ordem nem ultrapassar silenciosamente os limites.

## 8. Reprodução e histórico

### História de usuário

Como participante, quero acompanhar o que está tocando e consultar o que já passou pela rádio.

### Regras de negócio

- Apenas uma música pode estar em reprodução por sala.
- Um playback está relacionado ao item de fila que originou a execução.
- O playback atual informa música, origem, autor quando existir, início, pausa e posição lógica.
- Ao terminar, pular ou registrar erro, o sistema tenta iniciar o próximo item `WAITING`.
- O histórico contém somente playbacks encerrados nos estados `FINISHED`, `SKIPPED` ou `ERROR`.
- Playbacks `PLAYING` não fazem parte do histórico.
- As reproduções mais recentes aparecem primeiro.
- A consulta aceita de 1 a 100 registros e usa 20 como padrão.

## 9. Modo Todos os Navegadores

### História de usuário

Como participante, quero ouvir a rádio no meu navegador quando as pessoas estiverem em locais diferentes.

### Regras de negócio

- Todos os navegadores participantes podem reproduzir a música atual.
- A música e a posição lógica da rádio são compartilhadas.
- Pausar nesse modo é uma ação local e não interrompe os demais participantes.
- O relógio global continua avançando durante a pausa local.
- Ao retomar, o navegador consulta o snapshot atual e se reposiciona.
- Se a música mudar durante a pausa local, o navegador permanece em silêncio até o usuário retomar.
- Não existe pausa global nesse modo.

## 10. Modo Caixa de Música

### História de usuário

Como participante em um ambiente compartilhado, quero que apenas um navegador reproduza o áudio.

### Regras de negócio

- Apenas uma sessão de navegador pode assumir o player da sala.
- O player é identificado pelo `clientSessionId` e pelo usuário associado ao claim persistido.
- O claim só é permitido para uma presença válida da mesma combinação de usuário e `clientSessionId`.
- Repetir o claim pela mesma sessão é idempotente; outra sessão recebe conflito enquanto o player estiver ocupado.
- Somente a sessão player deve reproduzir o áudio; no back-end, comandos globais exigem participante presente e um `clientSessionId` igual ao claim atual.
- A pausa global interrompe o avanço da posição lógica até a retomada.
- O player é liberado por saída explícita da sessão, expiração da presença, perda prolongada do WebSocket ou troca para Todos os Navegadores.
- Após a última conexão WebSocket do player cair, existe tolerância de 15 segundos antes da liberação.
- Reconectar com o mesmo `clientSessionId` durante a tolerância cancela a liberação pendente.

## 11. Troca do modo de reprodução

### História de usuário

Como participante, quero alternar o modo de reprodução conforme a forma de uso da sala.

### Regras de negócio

- Apenas participantes ativos podem alterar o modo.
- Ao mudar para Todos os Navegadores, o player é liberado.
- Uma reprodução globalmente pausada é retomada nessa mudança.

## 12. Votação para pular

### História de usuário

Como participante, quero votar para pular uma música.

### Regras de negócio

- Cada usuário pode votar apenas uma vez por playback.
- O voto é associado ao usuário e à reprodução atual.
- A música é pulada ao atingir o arredondamento para cima de 60% dos usuários presentes.
- A quantidade necessária considera somente participantes com presença válida.
- Os votos não são reaproveitados no playback seguinte.
- Ao atingir o limite, o playback é marcado como `SKIPPED` e o sistema tenta iniciar o próximo.

## 13. Auto-DJ

### História de usuário

Como participante, quero que a rádio tente continuar quando não houver músicas aguardando.

### Regras de negócio

- O Auto-DJ só atua quando não existe nenhum item `WAITING`.
- Itens automáticos possuem origem `AUTO_DJ` e usuário nulo.
- A última reprodução `FINISHED` pode ser usada como referência para uma busca contextual de até 10 resultados.
- O Auto-DJ não escolhe a música atual, itens já aguardando nem as 10 músicas reproduzidas recentemente.
- Reproduções recentes finalizadas, puladas ou com erro participam da janela de exclusão.
- Candidatos continuam sujeitos à duração máxima e às regras de elegibilidade.
- Se a busca contextual falhar ou não produzir candidato, o sistema tenta uma música elegível já persistida.
- Se nenhuma música for elegível, a rádio pode permanecer sem novo playback.

## 14. Atividade e resumo da sessão

### Histórias de usuário

Como participante, quero acompanhar acontecimentos recentes da sala.

Como usuário que já participou, quero consultar um resumo da sessão.

### Regras de negócio

- A atividade é persistente e distinta dos eventos transitórios do WebSocket.
- A consulta retorna até 50 atividades, da mais recente para a mais antiga.
- São registradas entradas, saídas, adições feitas por usuários, remoções de músicas, renomeações e playbacks iniciados, finalizados ou pulados.
- A atividade preserva snapshots do nome do ator e do título da música quando aplicável.
- O resumo pode ser consultado por qualquer usuário que já tenha membership na sala, mesmo sem presença atual.
- O resumo informa playbacks, participantes históricos, skips, execuções Auto-DJ, votos e contribuições por usuário.

## 15. Tempo real e reconnect

### História de usuário

Como participante, quero receber mudanças da sala e recuperar o estado depois de uma desconexão.

### Regras de negócio

- As principais mudanças são notificadas por WebSocket com STOMP.
- Os comandos e snapshots continuam sendo realizados por HTTP/REST.
- Eventos incluem mudanças de fila, playback, participantes, votação, player e modo.
- Eventos WebSocket indicam que o estado mudou, mas não substituem o snapshot autoritativo.
- Ao conectar, reconectar ou receber eventos relevantes, o frontend consulta novamente estado, histórico e atividades.
- O cliente tenta reconectar o STOMP após 3 segundos.
- O `clientSessionId` é enviado também na conexão STOMP para correlacionar a sessão de navegador.

## 16. Tratamento de erros

### Regras de negócio

- Falhas na integração com o YouTube são apresentadas como indisponibilidade temporária.
- Uma música com erro de reprodução é registrada como `ERROR`.
- Depois de erro, finalização ou skip, o sistema tenta continuar com o próximo item.
- Falhas individuais não devem corromper a fila nem bloquear permanentemente a sala.

## 17. Concorrência e consistência

### Regras de negócio

- Operações críticas usam transações e bloqueios no banco.
- Adições simultâneas não podem quebrar a ordem nem ultrapassar o limite por usuário.
- Votos simultâneos não podem duplicar o voto de um usuário.
- O banco impede mais de um item de fila `PLAYING` por sala.
- Transições concorrentes do mesmo playback devem ser idempotentes ou rejeitadas.
- Eventos de sala são publicados somente após a confirmação da transação correspondente.

## 18. Persistência e segurança

### Regras de negócio

- Os dados persistentes são armazenados em PostgreSQL.
- Usuários, salas, memberships, presenças, atividades, músicas, fila, playbacks e votos sobrevivem ao reinício da aplicação.
- Conexões WebSocket e a tolerância de liberação do player são transitórias no processo; o timer de reconnect STOMP pertence ao navegador.
- Credenciais e chaves externas são fornecidas por variáveis de ambiente.
- Rotas de API, salas e WebSocket exigem autenticação.
- Operações mutáveis exigem proteção CSRF.
- A identidade do usuário vem da autenticação, não de um `userId` enviado livremente pelo cliente.
- O `clientSessionId` identifica um contexto de navegador e não substitui autenticação nem funciona como senha.
