# Sintonia 🎧

Uma rádio compartilhada.

O **Sintonia** é um projeto de desenvolvimento de software criado a partir de uma proposta do meu professor de programação.

A proposta é simples: **cada aluno deve construir o sistema completo**, passando pelo back-end, front-end, banco de dados e testes.

A especificação do projeto é a mesma para todos, mas as decisões de implementação ficam por conta de cada pessoa.

No final, a ideia é comparar os caminhos escolhidos, entender as diferenças entre as soluções e discutir as decisões tomadas durante o desenvolvimento.

Este repositório registra o meu caminho na construção do Sintonia.

---

## 💡 Sobre o projeto

O Sintonia é uma aplicação de **rádio compartilhada**, em que diferentes pessoas podem entrar em uma mesma sala e acompanhar a reprodução de um conteúdo de forma sincronizada.

A proposta parece simples à primeira vista, mas envolve vários problemas interessantes de desenvolvimento:

* Como criar e gerenciar as salas?
* Como representar os usuários conectados?
* Como controlar a fila de reprodução?
* Como manter todos os usuários sincronizados?
* Como o servidor deve controlar o estado atual da rádio?
* Como comunicar mudanças em tempo real?
* Como organizar as regras de negócio?
* Como armazenar as informações no banco de dados?
* Como testar tudo isso?

É justamente nesses problemas que está grande parte do aprendizado do projeto.

---

## 🎯 Objetivo

O objetivo não é apenas construir uma aplicação que funcione.

A proposta é desenvolver o sistema completo e, durante esse processo, tomar decisões técnicas próprias e entender as consequências de cada uma.

O projeto envolve:

* Back-end
* Front-end
* Banco de dados
* Testes
* API
* Comunicação em tempo real
* Arquitetura de software
* Regras de negócio
* Versionamento com Git

Como a especificação é compartilhada entre os alunos, as escolhas feitas durante o desenvolvimento poderão ser comparadas posteriormente com outras soluções para o mesmo problema.

---

## 👩‍💻 Meu caminho

Uma das partes mais interessantes dessa proposta é que **não existe uma implementação única obrigatória**.

A especificação define o que o sistema precisa fazer, mas não necessariamente como fazer.

Isso significa que algumas decisões serão minhas.

Por exemplo:

* Como estruturar o back-end?
* Como dividir as responsabilidades?
* Como modelar o banco?
* Como representar uma sala?
* Como implementar a sincronização?
* Como organizar a API?
* Como tratar determinadas regras de negócio?
* Quais tecnologias e ferramentas utilizar?

Durante o desenvolvimento, algumas dessas decisões podem mudar.

E isso também faz parte do projeto.

Quero registrar não apenas o código final, mas também as decisões e aprendizados que acontecerem pelo caminho.

---

## 🚧 Status do projeto

**Em desenvolvimento.**

O projeto está atualmente na fase de planejamento e definição da solução.

A implementação será feita por etapas, começando pelo MVP e evoluindo conforme os problemas forem sendo resolvidos.

---

## 🧩 MVP

A primeira versão deverá contemplar as funcionalidades essenciais da rádio compartilhada.

Entre elas:

* Criar uma sala
* Entrar em uma sala
* Escolher um apelido
* Ver os usuários presentes
* Adicionar músicas à fila
* Reproduzir músicas
* Manter a reprodução sincronizada
* Controlar a reprodução
* Sair da sala

Funcionalidades adicionais serão consideradas depois que o funcionamento básico estiver estabelecido.

---

## 🏗️ Arquitetura

A arquitetura será definida durante a fase de planejamento e documentada neste repositório.

A ideia inicial é trabalhar com uma aplicação dividida entre:

```text
Frontend
    ↓
API / Comunicação
    ↓
Backend
    ↓
Banco de Dados
```

Também será necessário lidar com comunicação em tempo real para que o estado da rádio possa ser compartilhado entre os usuários conectados.

A estrutura definitiva será resultado das decisões tomadas durante o desenvolvimento.

---

## 🛠️ Tecnologias

A stack inicial planejada é:

### Back-end

* **Java**
* **Spring Boot**
* **API REST**
* **WebSocket**

### Banco de dados

* **PostgreSQL**

### Front-end

* **React**
* **Vite**

### Ferramentas

* **Git**
* **GitHub**
* **IntelliJ IDEA**

Essas escolhas ainda podem ser revistas durante o projeto.

---

## ☕ Por que Java?

Escolhi desenvolver o back-end em **Java** porque é uma das principais tecnologias que estou estudando atualmente.

A ideia é aproveitar o projeto para sair um pouco dos exercícios isolados e começar a lidar com problemas mais próximos dos encontrados no desenvolvimento de uma aplicação real.

Não espero saber tudo antes de começar.

Parte do objetivo é justamente encontrar problemas que ainda não sei resolver, pesquisar, testar alternativas, errar, corrigir e entender o que estou fazendo.

---

## ⚛️ Front-end

A ideia inicial é utilizar **React com Vite** no front-end.

O React ficará responsável pela interface da aplicação e pela interação com o usuário.

O Vite será utilizado como ferramenta de desenvolvimento do projeto front-end.

A separação entre front-end e back-end também será importante para praticar a comunicação entre diferentes partes de uma aplicação.

---

## 📚 O que estou aprendendo com o projeto

O Sintonia está sendo usado como uma forma de colocar em prática conceitos que estou estudando.

### Java

* Programação Orientada a Objetos
* Classes e interfaces
* Collections
* Tratamento de exceções
* Streams
* Organização de código

### Back-end

* Spring Boot
* APIs REST
* HTTP
* JSON
* WebSocket
* Validação
* Regras de negócio

### Banco de dados

* Modelagem
* SQL
* Relacionamentos
* PostgreSQL
* Persistência de dados

### Arquitetura

* Separação de responsabilidades
* Camadas
* DTOs
* Services
* Repositories
* Organização da aplicação

### Testes

* Testes unitários
* Testes de integração
* Validação das regras de negócio

### Desenvolvimento

* Git
* GitHub
* Commits
* Branches
* Debugging
* Documentação

---

## 🗺️ Roadmap

O roadmap pode mudar conforme o projeto evoluir.

### Fase 1 — Planejamento

* [x] Definir a ideia do projeto
* [x] Definir o nome
* [x] Definir o objetivo
* [x] Definir o MVP
* [ ] Documentar requisitos
* [ ] Documentar regras de negócio
* [ ] Definir arquitetura
* [ ] Definir modelagem do banco
* [ ] Definir estrutura do projeto

### Fase 2 — Back-end

* [ ] Criar projeto Java + Spring Boot
* [ ] Criar estrutura inicial
* [ ] Criar entidades
* [ ] Criar regras de negócio
* [ ] Criar API
* [ ] Configurar PostgreSQL
* [ ] Implementar salas
* [ ] Implementar usuários
* [ ] Implementar fila de músicas
* [ ] Implementar WebSocket
* [ ] Implementar sincronização

### Fase 3 — Front-end

* [ ] Criar projeto React + Vite
* [ ] Criar tela inicial
* [ ] Criar entrada na sala
* [ ] Criar interface da rádio
* [ ] Criar lista de usuários
* [ ] Criar fila de músicas
* [ ] Conectar ao back-end
* [ ] Implementar comunicação em tempo real

### Fase 4 — Testes

* [ ] Criar testes unitários
* [ ] Criar testes de integração
* [ ] Testar regras de negócio
* [ ] Testar comunicação entre componentes
* [ ] Testar múltiplos usuários

### Fase 5 — Integração

* [ ] Integrar front-end e back-end
* [ ] Testar sincronização
* [ ] Corrigir problemas encontrados
* [ ] Melhorar a experiência de uso

### Fase 6 — Comparação

Depois que o projeto estiver concluído, comparar minha implementação com a de outros alunos que receberam a mesma especificação.

A ideia é analisar:

* Decisões de arquitetura
* Modelagem do banco
* Organização do código
* Tecnologias utilizadas
* Estratégias de sincronização
* Tratamento das regras de negócio
* Testes
* Pontos positivos e limitações de cada solução

---

## 📁 Estrutura do projeto

A estrutura inicial planejada é:

```text
sintonia-radio/
│
├── backend/
│
├── frontend/
│
├── docs/
│   ├── requisitos.md
│   ├── regras-de-negocio.md
│   ├── arquitetura.md
│   └── roadmap.md
│
├── .gitignore
└── README.md
```

Essa estrutura poderá mudar conforme o projeto evoluir.

---

## 📝 Documentação

Além do código, este repositório será utilizado para registrar algumas das decisões tomadas durante o desenvolvimento.

A documentação deverá incluir:

* Requisitos
* Regras de negócio
* Arquitetura
* Modelagem do banco
* Decisões técnicas
* Roadmap
* Anotações do desenvolvimento

A intenção é conseguir olhar para o projeto depois e entender **não apenas o que foi feito, mas por que foi feito daquela maneira**.

---

## 🎓 Por que este projeto existe?

Eu estou estudando programação e queria começar a aplicar o que estou aprendendo em algo maior do que exercícios separados.

Foi daí que surgiu a proposta do Sintonia.

Meu professor propôs que cada aluno construísse sua própria solução para o mesmo problema.

Todos partimos da mesma especificação, mas cada pessoa precisa descobrir como transformar aquilo em um sistema funcionando.

Para mim, isso torna o projeto ainda mais interessante.

Não quero simplesmente chegar ao final com um código funcionando.

Quero entender o caminho.

Quero descobrir quais decisões fazem sentido, quais não fazem, onde vou errar e o que vou aprender tentando resolver cada problema.

E, no final, quero poder comparar minha solução com outras soluções construídas a partir exatamente do mesmo ponto de partida.

---

## 🚀 Próximos passos

Antes de começar a escrever código, vou finalizar:

1. Requisitos
2. Regras de negócio
3. Modelagem
4. Arquitetura
5. Estrutura inicial do projeto

Depois disso, começa a implementação.

A ideia é seguir mais ou menos assim:

```text
Especificação
     ↓
Requisitos
     ↓
Regras de negócio
     ↓
Modelagem
     ↓
Arquitetura
     ↓
Implementação
     ↓
Testes
     ↓
Integração
     ↓
Comparação
```

---

## 📄 Licença

A licença do projeto ainda será definida.

---

## 👩‍💻 Sobre

Este é um projeto de estudo e portfólio.

O Sintonia está sendo desenvolvido enquanto estudo desenvolvimento de software, com foco principalmente em **Java e back-end**.

Mais do que mostrar um resultado pronto, este repositório pretende registrar o processo de construção do projeto.

**Em desenvolvimento.**
