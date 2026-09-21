# Bugs conhecidos — Sintonia

Este documento registra comportamentos conhecidos que precisam ser corrigidos ou melhorados futuramente, mas que não bloqueiam a versão atual do projeto.

## 1. Interface não atualiza corretamente ao mudar para Caixa de Música

**Status:** conhecido / não corrigido

### Comportamento observado

Ao alterar o modo da sala de `TODOS_OS_NAVEGADORES` para `CAIXA_DE_MUSICA`, a interface apresenta um pequeno "pulo", mas não reflete corretamente o novo estado da sala.

É necessário atualizar manualmente a página (`F5`) para que a interface reconheça o modo Caixa de Música e permita ao usuário assumir o player normalmente.

### Comportamento esperado

Ao selecionar `CAIXA_DE_MUSICA`, a interface deveria atualizar automaticamente com o novo estado da sala, sem necessidade de recarregar a página.

Após a mudança, o usuário deveria conseguir visualizar e utilizar normalmente a opção de assumir o player.

### Impacto atual

Não impede o funcionamento da Caixa de Música após o refresh, mas prejudica a experiência do usuário e indica uma falha de atualização/sincronização da interface após a mudança de modo.

### Investigação futura

Verificar o fluxo:

mudança de modo
→ resposta da API
→ evento `PLAYBACK_MODE_CHANGED`
→ atualização/refetch do snapshot
→ atualização do estado React
→ renderização dos controles da Caixa de Música

A causa ainda não foi investigada nem confirmada.
