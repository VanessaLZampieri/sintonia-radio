# Sintonia 🎧

**Boa música toca melhor em boa companhia.**

O Sintonia é uma rádio compartilhada que desenvolvi como projeto de estudo enquanto aprendo desenvolvimento back-end com Java.

A ideia surgiu a partir de uma proposta do meu professor: cada aluno recebeu a mesma especificação e ficou responsável por pensar e construir sua própria solução.

O objetivo não era apenas fazer o sistema funcionar, mas também tomar decisões técnicas, lidar com os problemas que aparecessem durante o desenvolvimento e, no final, comparar as diferentes soluções criadas pela turma.

O resultado foi um projeto bem maior do que eu imaginava quando comecei.

## Sobre o projeto

O Sintonia permite criar salas em que várias pessoas podem montar e ouvir uma fila de músicas juntas.

As músicas são pesquisadas através do YouTube e o estado da sala é atualizado em tempo real. Quem está participando consegue acompanhar o que está tocando, adicionar músicas, votar para pular uma faixa e consultar o que aconteceu durante a sessão.

Existem duas formas de reprodução:

### Todos os navegadores

Cada participante pode ouvir a música no próprio dispositivo.

A rádio mantém um estado compartilhado, mas cada pessoa pode pausar localmente sem interromper a reprodução para os outros participantes.

### Caixa de música

Esse modo foi pensado para quando várias pessoas estão no mesmo ambiente.

Apenas um navegador fica responsável pela reprodução do áudio, enquanto todos continuam podendo adicionar músicas, votar e acompanhar a sala.

## Funcionalidades

- Login com Google
- Criação de salas com código de acesso
- Presença de participantes
- Busca de músicas pelo YouTube
- Fila compartilhada
- Limite de músicas por participante
- Reprodução sincronizada
- Modos **Todos os navegadores** e **Caixa de música**
- Votação para pular músicas
- Histórico de reprodução
- Auto-DJ quando não existem músicas de usuários aguardando
- Atividade da sala em tempo real
- Resumo da sessão
- Expiração automática de salas vazias

Algumas dessas funcionalidades acabaram envolvendo regras maiores do que eu esperava.

A fila, por exemplo, não é apenas uma lista exibida no front-end. O back-end controla a ordem, impede músicas duplicadas simultaneamente, limita cada participante a 8 músicas próprias aguardando e precisa lidar com operações concorrentes sem deixar a fila em um estado inconsistente.

## Tecnologias

### Back-end

- Java 25
- Spring Boot 4.1.1
- Spring Security
- OAuth2
- Spring Data JPA
- Hibernate
- WebSocket + STOMP
- Maven

### Front-end

- React
- TypeScript
- Vite
- YouTube IFrame Player API

### Banco de dados

- PostgreSQL

### Integrações e ferramentas

- YouTube Data API v3
- Google OAuth2
- Git e GitHub
- Docker

## Arquitetura

De forma simplificada, o projeto funciona assim:

```text
            ┌─────────────────┐
            │  React + Vite   │
            │    Front-end    │
            └────────┬────────┘
                     │
              REST + WebSocket
                     │
            ┌────────▼────────┐
            │   Spring Boot   │
            │    Back-end     │
            └───┬─────────┬───┘
                │         │
                │         └────────► YouTube Data API
                │
        ┌───────▼───────┐
        │  PostgreSQL   │
        └───────────────┘

O navegador também utiliza a
YouTube IFrame Player API para
executar o áudio.
```

O back-end funciona como a principal fonte do estado compartilhado da rádio.

O WebSocket é usado para comunicar mudanças aos participantes em tempo real, mas os navegadores também conseguem consultar um snapshot do estado atual da sala. Assim, uma reconexão não depende apenas dos eventos que foram recebidos anteriormente.

## Algumas decisões que deram trabalho

Uma das coisas que mais gostei nesse projeto foi perceber que funcionalidades que parecem simples podem esconder decisões bem maiores.

### Quem toca o áudio?

No começo, uma das decisões era escolher entre cada navegador tocar a música ou existir apenas um dispositivo responsável pelo áudio.

Acabei implementando os dois comportamentos.

Foi daí que surgiram os modos **Todos os navegadores** e **Caixa de música**.

### O que significa estar presente em uma sala?

Também precisei separar coisas que inicialmente pareciam iguais.

Fechar uma aba, perder a conexão WebSocket por alguns segundos e clicar em **Sair** não significam necessariamente a mesma coisa.

A presença acabou precisando de uma lógica própria para que uma desconexão temporária não removesse imediatamente alguém da sala.

### Quem decide o estado da rádio?

Outra decisão importante foi manter o back-end como fonte principal do estado compartilhado.

O navegador executa o áudio e envia comandos, mas regras como fila, votos, modo de reprodução e estado da música são controladas pelo servidor.

### E se duas pessoas fizerem alguma coisa ao mesmo tempo?

Esse foi um problema que eu não tinha pensado quando comecei o projeto.

Adicionar músicas, votar e alterar uma reprodução são operações que podem acontecer praticamente ao mesmo tempo em navegadores diferentes.

Por isso, algumas partes do projeto precisaram lidar com concorrência para impedir estados inválidos, como duas músicas ocupando a mesma situação de reprodução ou um voto sendo contado mais de uma vez.

As decisões técnicas do projeto estão registradas com mais detalhes em [`DECISIONS.md`](DECISIONS.md).

## Testes

O Sintonia possui testes automatizados tanto no back-end quanto no front-end.

Durante o desenvolvimento, foram testadas situações como:

- limite de músicas por participante;
- músicas duplicadas;
- votação duplicada;
- concorrência na fila;
- transições de playback;
- autenticação;
- CSRF;
- presença e expiração;
- regras dos modos de reprodução;
- estados da sala;
- comportamento da interface.

Também fiz vários testes manualmente, principalmente nos fluxos que dependem da interação entre navegadores diferentes e na validação da interface.

## Desenvolvimento com apoio de IA

**Este projeto foi desenvolvido com apoio de Inteligência Artificial.**

Usei IA para discutir soluções, entender conceitos, investigar erros, revisar código, implementar partes do projeto e criar testes.

Como ainda estou aprendendo Java e desenvolvimento back-end, existem partes do Sintonia que são mais avançadas do que o código que eu conseguiria escrever sozinha hoje.

Preferi deixar isso explícito.

Durante o desenvolvimento, procurei acompanhar as alterações, testar o comportamento da aplicação e entender as decisões que estavam sendo tomadas. Quando alguma coisa não funcionava como esperado, eu também voltava ao problema, testava e revisava a solução antes de seguir.

Agora, com a aplicação funcional, uma das minhas próximas etapas é voltar ao código e estudar com mais calma os módulos e conceitos que ainda não domino.

Para mim, o Sintonia não é uma demonstração de que já sei fazer sozinha tudo o que existe nele. É um registro do que estou conseguindo construir e aprender usando as ferramentas que tenho disponíveis hoje.

## O que aprendi

Comecei o Sintonia querendo praticar Java fora de exercícios pequenos.

No processo, acabei tendo contato com vários assuntos que eu ainda estava começando a estudar:

- APIs REST
- Spring Boot
- autenticação com OAuth2
- persistência com JPA/Hibernate
- PostgreSQL
- WebSocket
- concorrência
- sessões e presença
- integração com APIs externas
- testes automatizados
- React e TypeScript
- Docker
- deploy
- debugging de problemas envolvendo front-end e back-end

Ainda tenho bastante coisa para estudar dentro do próprio código do projeto.

Mas essa era justamente uma das ideias: construir algo que me obrigasse a encontrar problemas que eu ainda não sabia resolver.

## Rodando o projeto localmente

### Pré-requisitos

Para executar o Sintonia localmente, você vai precisar de:

- JDK 25
- Node.js 22
- PostgreSQL
- uma aplicação OAuth configurada no Google
- uma chave da YouTube Data API v3

O Maven não precisa ser instalado separadamente porque o projeto utiliza o Maven Wrapper.

### Banco de dados

Crie um banco PostgreSQL chamado `sintonia`:

```sql
CREATE DATABASE sintonia;
```

A configuração local utiliza:

```text
host: localhost
porta: 5432
banco: sintonia
usuário: postgres
```

A senha do banco é fornecida através de variável de ambiente.

### Variáveis de ambiente

Antes de iniciar o back-end, configure:

```text
DB_PASSWORD
GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET
YOUTUBE_API_KEY
```

As credenciais não devem ser adicionadas ao repositório.

No PowerShell, por exemplo:

```powershell
$env:DB_PASSWORD="<sua-senha>"
$env:GOOGLE_CLIENT_ID="<seu-client-id>"
$env:GOOGLE_CLIENT_SECRET="<seu-client-secret>"
$env:YOUTUBE_API_KEY="<sua-api-key>"
```

### Google OAuth

Para executar o login com Google localmente, o cliente OAuth precisa possuir o seguinte redirect URI:

```text
http://localhost:8080/login/oauth2/code/google
```

Os escopos utilizados pelo projeto são:

```text
openid
email
profile
```

### YouTube

A busca de músicas utiliza a **YouTube Data API v3**, que precisa estar habilitada no projeto associado à chave utilizada em `YOUTUBE_API_KEY`.

A reprodução no navegador utiliza a **YouTube IFrame Player API**.

### Iniciando o back-end

Na raiz do projeto:

```powershell
.\mvnw.cmd spring-boot:run
```

O back-end será iniciado em:

```text
http://localhost:8080
```

### Iniciando o front-end

Em outro terminal:

```powershell
cd frontend
npm ci
npm run dev
```

O Vite será iniciado em:

```text
http://localhost:5173
```

Durante o desenvolvimento, o Vite encaminha as requisições necessárias para o back-end executado na porta `8080`.

## Executando os testes

### Back-end

Na raiz do projeto:

```powershell
.\mvnw.cmd test
```

A suíte completa do back-end utiliza o PostgreSQL configurado para a aplicação. Por isso, o banco precisa estar disponível e `DB_PASSWORD` precisa estar definida.

### Front-end

```powershell
cd frontend
npm ci
npm test
```

Para validar o build:

```powershell
npm run build
```

## Docker

O projeto também possui um `Dockerfile` multi-stage.

Durante o build, o front-end é compilado com Node.js e incorporado à aplicação Spring Boot. A imagem final executa a aplicação utilizando Java 25.

O PostgreSQL não faz parte da imagem e precisa ser fornecido separadamente.

## Documentação

Além deste README, o repositório possui documentos que registram partes mais específicas do projeto:

- [`SPEC.md`](SPEC.md) — especificação e regras de negócio
- [`API.md`](API.md) — principais endpoints da aplicação
- [`DECISIONS.md`](DECISIONS.md) — decisões técnicas e alternativas consideradas
- [`docs/data-model.md`](docs/data-model.md) — modelo de dados

A intenção é registrar não apenas o resultado final, mas também algumas das decisões que levaram até ele.

## Status

O Sintonia está funcional e já passou por testes automatizados e validações manuais dos principais fluxos.

A aplicação também foi publicada para validação.

Ainda existem coisas que quero fazer depois, principalmente continuar estudando a implementação, melhorar a documentação e experimentar a publicação do projeto na Oracle Cloud.

## Sobre

Este é um projeto de estudo e portfólio desenvolvido durante minha formação em Engenharia de Software, com foco principalmente no meu aprendizado de **Java e desenvolvimento back-end**.

Comecei o Sintonia sem saber como resolver boa parte dos problemas que apareceriam pelo caminho.

Esse acabou sendo justamente o ponto mais importante do projeto.