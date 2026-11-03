# Guia de testes da API

Roteiro para testar todos os endpoints na ordem natural de uso do sistema, do login às exclusões.

## Como usar

- **URL base:** `http://localhost:8080/api`. Os caminhos abaixo são relativos a ela.
- **Banco:** comece com o banco vazio, porque e-mails, ISBNs e códigos de exemplar não podem se repetir. Para zerar: `docker compose down -v`.
- **Ordem:** siga os passos em sequência, porque cada um depende dos anteriores.
- **Variáveis:** troque cada `{{variavel}}` pelo valor guardado em um passo anterior. Os IDs variam, então use os da sua execução.
- **Token:** quando indicado, envie o cabeçalho `Authorization: Bearer <token>`. No Swagger, use o botão **Authorize** e informe só o token.
- **Corpo:** envie o JSON com `Content-Type: application/json`.

## 1. Saúde e documentação

Rotas públicas, para confirmar que a aplicação subiu.

### Passo 1: Verificar a saúde da aplicação

`GET /actuator/health`

- **Token:** nenhum
- **Resposta esperada:** `200`

### Passo 2: Baixar o contrato OpenAPI

`GET /v3/api-docs`

- **Token:** nenhum
- **Resposta esperada:** `200`

## 2. Autenticação inicial

O bibliotecário inicial é criado na subida, com os dados do `.env`. Se você trocou os valores do `.env.example`, use os seus no passo 3.

### Passo 3: Login do bibliotecário

`POST /auth/login`

- **Token:** nenhum
- **Resposta esperada:** `200`
- **Guardar:** `accessToken` como `{{librarianToken}}`

```json
{
  "email": "bibliotecario@example.com",
  "password": "troque-esta-senha-tambem"
}
```

### Passo 4: Registrar a membro Ana

`POST /auth/register`

- **Token:** nenhum
- **Resposta esperada:** `201`
- **Guardar:** `id` como `{{anaId}}`

```json
{
  "name": "Ana Souza",
  "email": "ana@example.com",
  "password": "senha-da-ana-123"
}
```

### Passo 5: Registrar o membro Bruno

`POST /auth/register`

- **Token:** nenhum
- **Resposta esperada:** `201`
- **Guardar:** `id` como `{{brunoId}}`

```json
{
  "name": "Bruno Lima",
  "email": "bruno@example.com",
  "password": "senha-do-bruno-123"
}
```

### Passo 6: Login da Ana

`POST /auth/login`

- **Token:** nenhum
- **Resposta esperada:** `200`
- **Guardar:** `accessToken` como `{{anaToken}}`, `refreshToken` como `{{anaRefreshToken}}`

```json
{
  "email": "ana@example.com",
  "password": "senha-da-ana-123"
}
```

### Passo 7: Login do Bruno

`POST /auth/login`

- **Token:** nenhum
- **Resposta esperada:** `200`
- **Guardar:** `accessToken` como `{{brunoToken}}`

```json
{
  "email": "bruno@example.com",
  "password": "senha-do-bruno-123"
}
```

### Passo 8: Consultar o próprio perfil

`GET /auth/me`

- **Token:** `{{anaToken}}`
- **Resposta esperada:** `200`

## 3. Usuários

### Passo 9: Criar um bibliotecário

`POST /users`

- **Token:** `{{librarianToken}}`
- **Resposta esperada:** `201`
- **Guardar:** `id` como `{{carlaId}}`

```json
{
  "name": "Carla Mendes",
  "email": "carla@example.com",
  "password": "senha-da-carla-123",
  "roles": [
    "ROLE_LIBRARIAN"
  ]
}
```

### Passo 10: Listar usuários

`GET /users?page=0&size=20&sort=name&direction=asc`

- **Token:** `{{librarianToken}}`
- **Resposta esperada:** `200`

### Passo 11: Consultar um usuário

`GET /users/{{anaId}}`

- **Token:** `{{librarianToken}}`
- **Resposta esperada:** `200`

### Passo 12: Atualizar o próprio perfil

`PUT /users/{{anaId}}`

- **Token:** `{{anaToken}}`
- **Resposta esperada:** `200`

Só um bibliotecário pode enviar o campo `roles`.

```json
{
  "name": "Ana Souza Pereira",
  "email": "ana@example.com"
}
```

## 4. Livros e exemplares

Os ISBNs são válidos, mas fictícios para estes títulos. Cada um só pode ser cadastrado uma vez.

### Passo 13: Cadastrar um livro com um exemplar

`POST /books`

- **Token:** `{{librarianToken}}`
- **Resposta esperada:** `201`
- **Guardar:** `id` como `{{narniaId}}`

```json
{
  "title": "As Crônicas de Nárnia",
  "author": "C. S. Lewis",
  "isbn": "9780306406157",
  "description": "Livro das Crônicas de Nárnia",
  "assetCodes": [
    "NAR-001"
  ]
}
```

### Passo 14: Cadastrar livros em lote

`POST /books/batch`

- **Token:** `{{librarianToken}}`
- **Resposta esperada:** `201`
- **Guardar:** `0.id` como `{{domCasmurroId}}`, `2.id` como `{{semHistoricoId}}`

O lote é tudo ou nada: um item inválido cancela todos.

```json
{
  "books": [
    {
      "title": "Dom Casmurro",
      "author": "Machado de Assis",
      "isbn": "9788535902778",
      "description": "Romance de 1899",
      "assetCodes": [
        "DOM-001"
      ]
    },
    {
      "title": "Grande Sertão: Veredas",
      "author": "João Guimarães Rosa",
      "isbn": "9783161484100",
      "assetCodes": [
        "GSV-001"
      ]
    },
    {
      "title": "Livro Sem Histórico",
      "author": "Autor de Teste",
      "isbn": "9781402894626"
    }
  ]
}
```

### Passo 15: Buscar livros por título

`GET /books?title=cr%C3%B4nicas`

- **Token:** `{{anaToken}}`
- **Resposta esperada:** `200`

### Passo 16: Buscar livros por autor

`GET /books?author=machado`

- **Token:** `{{anaToken}}`
- **Resposta esperada:** `200`

### Passo 17: Buscar livros por ISBN

`GET /books?isbn=978-0-306-40615-7`

- **Token:** `{{anaToken}}`
- **Resposta esperada:** `200`

### Passo 18: Buscar livros disponíveis, com paginação e ordenação

`GET /books?available=true&page=0&size=10&sort=title&direction=asc`

- **Token:** `{{anaToken}}`
- **Resposta esperada:** `200`

### Passo 19: Consultar um livro

`GET /books/{{narniaId}}`

- **Token:** `{{anaToken}}`
- **Resposta esperada:** `200`

### Passo 20: Atualizar um livro

`PUT /books/{{domCasmurroId}}`

- **Token:** `{{librarianToken}}`
- **Resposta esperada:** `200`

```json
{
  "title": "Dom Casmurro",
  "author": "Machado de Assis",
  "isbn": "9788535902778",
  "description": "Romance de Machado de Assis, publicado em 1899"
}
```

### Passo 21: Cadastrar mais exemplares

`POST /books/{{domCasmurroId}}/copies`

- **Token:** `{{librarianToken}}`
- **Resposta esperada:** `201`
- **Guardar:** `0.id` como `{{domCopyId}}`

```json
{
  "assetCodes": [
    "DOM-002",
    "DOM-003"
  ]
}
```

### Passo 22: Inativar um exemplar

`DELETE /books/{{domCasmurroId}}/copies/{{domCopyId}}`

- **Token:** `{{librarianToken}}`
- **Resposta esperada:** `200`

### Passo 23: Listar os exemplares de Nárnia

`GET /books/{{narniaId}}/copies`

- **Token:** `{{librarianToken}}`
- **Resposta esperada:** `200`
- **Guardar:** `0.id` como `{{narniaCopyId}}`

## 5. Empréstimos

### Passo 24: Emprestar Nárnia para a Ana

`POST /loans`

- **Token:** `{{librarianToken}}`
- **Resposta esperada:** `201`
- **Guardar:** `id` como `{{loanId}}`

A resposta traz `dueAt`, a previsão de devolução.

```json
{
  "userId": "{{anaId}}",
  "bookCopyId": "{{narniaCopyId}}"
}
```

### Passo 25: Listar empréstimos ativos

`GET /loans?active=true`

- **Token:** `{{librarianToken}}`
- **Resposta esperada:** `200`

### Passo 26: Consultar o próprio empréstimo

`GET /loans/{{loanId}}`

- **Token:** `{{anaToken}}`
- **Resposta esperada:** `200`

### Passo 27: Consultar o próprio histórico

`GET /users/{{anaId}}/loans`

- **Token:** `{{anaToken}}`
- **Resposta esperada:** `200`

## 6. Reservas

Só é possível reservar quando o livro não tem exemplar disponível. Por isso, esta etapa vem depois do empréstimo.

### Passo 28: Bruno reserva Nárnia

`POST /reservations`

- **Token:** `{{brunoToken}}`
- **Resposta esperada:** `201`
- **Guardar:** `id` como `{{reservationId}}`

Status esperado: `WAITING`.

```json
{
  "bookId": "{{narniaId}}"
}
```

### Passo 29: Consultar a reserva

`GET /reservations/{{reservationId}}`

- **Token:** `{{brunoToken}}`
- **Resposta esperada:** `200`

### Passo 30: Listar as próprias reservas

`GET /users/{{brunoId}}/reservations`

- **Token:** `{{brunoToken}}`
- **Resposta esperada:** `200`

## 7. Devolução e retirada da reserva

### Passo 31: Registrar a devolução da Ana

`POST /loans/{{loanId}}/return`

- **Token:** `{{librarianToken}}`
- **Resposta esperada:** `200`

O exemplar fica separado para o Bruno, que tem 48 horas para retirá-lo.

### Passo 32: Confirmar que a reserva está pronta

`GET /reservations/{{reservationId}}`

- **Token:** `{{brunoToken}}`
- **Resposta esperada:** `200`

Status esperado: `READY`.

### Passo 33: Emprestar o exemplar reservado para o Bruno

`POST /loans`

- **Token:** `{{librarianToken}}`
- **Resposta esperada:** `201`
- **Guardar:** `id` como `{{brunoLoanId}}`

A reserva passa para `FULFILLED`.

```json
{
  "userId": "{{brunoId}}",
  "bookCopyId": "{{narniaCopyId}}"
}
```

## 8. Cancelamento de reserva

### Passo 34: Ana entra na fila de Nárnia

`POST /reservations`

- **Token:** `{{anaToken}}`
- **Resposta esperada:** `201`
- **Guardar:** `id` como `{{cancelReservationId}}`

```json
{
  "bookId": "{{narniaId}}"
}
```

### Passo 35: Ana cancela a reserva

`DELETE /reservations/{{cancelReservationId}}`

- **Token:** `{{anaToken}}`
- **Resposta esperada:** `204`

## 9. Relatórios

### Passo 36: 20 livros mais emprestados

`GET /reports/most-borrowed`

- **Token:** `{{librarianToken}}`
- **Resposta esperada:** `200`

### Passo 37: Empréstimos ativos e previsão de devolução

`GET /reports/active-loans?page=0&size=20`

- **Token:** `{{librarianToken}}`
- **Resposta esperada:** `200`

## 10. Sessão e senha

### Passo 38: Renovar o token da Ana

`POST /auth/refresh`

- **Token:** nenhum
- **Resposta esperada:** `200`
- **Guardar:** `accessToken` como `{{anaToken}}`, `refreshToken` como `{{anaRefreshToken}}`

O refresh token muda a cada uso. Reutilizar um antigo encerra todas as sessões do usuário.

```json
{
  "refreshToken": "{{anaRefreshToken}}"
}
```

### Passo 39: Trocar a senha da Ana

`POST /auth/change-password`

- **Token:** `{{anaToken}}`
- **Resposta esperada:** `204`

Essa troca encerra as sessões da Ana.

```json
{
  "currentPassword": "senha-da-ana-123",
  "newPassword": "nova-senha-da-ana-123"
}
```

### Passo 40: Login da Ana com a nova senha

`POST /auth/login`

- **Token:** nenhum
- **Resposta esperada:** `200`
- **Guardar:** `accessToken` como `{{anaToken}}`, `refreshToken` como `{{anaRefreshToken}}`

```json
{
  "email": "ana@example.com",
  "password": "nova-senha-da-ana-123"
}
```

### Passo 41: Logout da Ana

`POST /auth/logout`

- **Token:** nenhum
- **Resposta esperada:** `204`

Revoga o refresh token. O token de acesso continua válido até expirar, em até 15 minutos.

```json
{
  "refreshToken": "{{anaRefreshToken}}"
}
```

## 11. Encerramento: exclusões

### Passo 42: Devolver o empréstimo do Bruno

`POST /loans/{{brunoLoanId}}/return`

- **Token:** `{{librarianToken}}`
- **Resposta esperada:** `200`

### Passo 43: Excluir um livro sem histórico

`DELETE /books/{{semHistoricoId}}`

- **Token:** `{{librarianToken}}`
- **Resposta esperada:** `204`

O livro é apagado de fato.

### Passo 44: Excluir um livro com histórico

`DELETE /books/{{narniaId}}`

- **Token:** `{{librarianToken}}`
- **Resposta esperada:** `204`

O livro é desativado, e o histórico é mantido. Depois disso, a busca não o retorna mais.

### Passo 45: Desativar um usuário

`DELETE /users/{{carlaId}}`

- **Token:** `{{librarianToken}}`
- **Resposta esperada:** `204`

Usuários nunca são apagados, para preservar o histórico.

## 12. Cenários de erro

Rode com os dados criados acima, para ver as respostas de erro. Estes passos não alteram nada.

### Passo 46: ISBN inválido

`POST /books`

- **Token:** `{{librarianToken}}`
- **Resposta esperada:** `400`

```json
{
  "title": "Teste",
  "author": "Teste",
  "isbn": "12345678910"
}
```

### Passo 47: ISBN repetido

`POST /books`

- **Token:** `{{librarianToken}}`
- **Resposta esperada:** `409`

```json
{
  "title": "Dom Casmurro",
  "author": "Machado de Assis",
  "isbn": "9788535902778"
}
```

### Passo 48: Sem token

`GET /books`

- **Token:** nenhum
- **Resposta esperada:** `401`

### Passo 49: Membro em rota de bibliotecário

`GET /reports/most-borrowed`

- **Token:** `{{brunoToken}}`
- **Resposta esperada:** `403`

### Passo 50: Membro acessando dados de outro membro

`GET /users/{{anaId}}`

- **Token:** `{{brunoToken}}`
- **Resposta esperada:** `403`

### Passo 51: Livro inexistente

`GET /books/999999`

- **Token:** `{{librarianToken}}`
- **Resposta esperada:** `404`

### Passo 52: Reserva com exemplar disponível

`POST /reservations`

- **Token:** `{{brunoToken}}`
- **Resposta esperada:** `422`

```json
{
  "bookId": "{{domCasmurroId}}"
}
```

### Passo 53: Senha errada

`POST /auth/login`

- **Token:** nenhum
- **Resposta esperada:** `401`

```json
{
  "email": "bruno@example.com",
  "password": "senha-errada-123"
}
```
