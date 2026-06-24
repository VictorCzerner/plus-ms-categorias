# plus-ms-categorias

Microsserviço de **categorização de produtos** do sistema **Plus** (clothing-stock management) — Trabalho 2 de Engenharia de Software 2 (PUCRS), Grupo 05.

Responsável por gerenciar as **categorias de produtos** (CRUD), incluindo hierarquia de subcategorias. A associação entre produtos e categorias é responsabilidade do MS de produtos — este serviço cuida apenas das categorias em si.

## Status

**Em desenvolvimento.** Etapa atual: **definição do contrato da API** (design-first com OpenAPI/Swagger). A implementação do backend Spring Boot ainda não foi iniciada.

## Autenticação

Este serviço valida JWT Bearer emitido pelo [`plus-ms-auth`](https://github.com/luizarosit0/plus-ms-auth).

Configure o mesmo segredo usado pelo auth:

```powershell
$env:JWT_SECRET="dev-secret"
```

Em ambiente local, `dev-secret` é o valor padrão para compatibilidade com o `plus-ms-auth`, mas em outros ambientes defina `JWT_SECRET` explicitamente.

Regras de acesso:

| Método | Rota | Permissão |
|---|---|---|
| `GET` | `/categorias` | Usuário autenticado |
| `GET` | `/categorias/{id}` | Usuário autenticado |
| `POST` | `/categorias` | ADMIN |
| `PUT` | `/categorias/{id}` | ADMIN |
| `PATCH` | `/categorias/{id}` | ADMIN |
| `DELETE` | `/categorias/{id}` | ADMIN |

Para obter um token, execute o `plus-ms-auth` e chame o login:

```powershell
$login = Invoke-RestMethod -Method Post `
  -Uri http://localhost:3001/auth/login `
  -ContentType "application/json" `
  -Body '{"email":"admindev@admin.com","password":"Senha123"}'

$token = $login.access_token
```

Exemplo de GET autenticado:

```powershell
curl.exe -H "Authorization: Bearer $token" http://localhost:3002/categorias
```

Exemplo de POST como ADMIN:

```powershell
curl.exe -X POST http://localhost:3002/categorias `
  -H "Authorization: Bearer $token" `
  -H "Content-Type: application/json" `
  -d "{\"nome\":\"Calças\",\"descricao\":\"Calças jeans e sociais\",\"ativo\":true}"
```

## Ambiente Local Com Docker Compose

O projeto possui `docker-compose.yml` para executar um ambiente local reproduzível com:

* `plus-ms-categorias`;
* PostgreSQL do serviço de categorias;
* LocalStack opcional para o ambiente Ministack/LocalStack.

Para subir o microsserviço e o banco:

```powershell
$env:JWT_SECRET="dev-secret"
docker compose up --build
```

A API fica disponível em:

```text
http://localhost:3002
```

O Compose injeta as variáveis de banco usadas pela aplicação:

```text
DB_URL=jdbc:postgresql://categorias-db:5432/categorias
DB_USER=postgres
DB_PASSWORD=postgres
JWT_SECRET=dev-secret
```

Para subir também o LocalStack:

```powershell
docker compose --profile localstack up --build
```

Este microsserviço não utiliza serviços AWS diretamente no código atual; por isso o LocalStack não é dependência obrigatória do `plus-ms-categorias`. Ele fica disponível no Compose para manter o ambiente compatível com uma execução Ministack/LocalStack quando necessário.

Para parar e remover os containers:

```powershell
docker compose down
```

## Testes e Cobertura

O projeto usa JUnit/Mockito nos testes e JaCoCo para relatÃ³rio de cobertura.

Para rodar os testes e gerar o relatÃ³rio localmente:

```powershell
.\gradlew.bat test jacocoTestReport
```

No Linux/macOS:

```bash
./gradlew test jacocoTestReport
```

O relatÃ³rio HTML Ã© gerado em:

```text
build/reports/jacoco/test/html/index.html
```

No GitHub Actions, o workflow de CI executa testes e cobertura automaticamente e publica o relatÃ³rio HTML como artifact chamado `jacoco-coverage-report`.

## Contrato da API (Swagger / OpenAPI)

A API foi modelada **design-first**: o contrato é escrito antes do código e é a fonte da verdade do serviço.

| Arquivo | Descrição |
|---|---|
| [`openapi.yaml`](./openapi.yaml) | Especificação OpenAPI 3.0.3 — **o contrato** |
| `swagger.html` | Documentação estática gerada a partir do contrato |

### Endpoints

Base path: `/categorias` · Autenticação: **JWT Bearer** (token emitido pelo `plus-ms-auth`)

| Método | Rota | Descrição | Permissão |
|---|---|---|---|
| `GET` | `/categorias` | Lista categorias (paginada, filtros: `nome`, `ativo`, `categoriaPaiId`) | Autenticado |
| `POST` | `/categorias` | Cria uma categoria | ADMIN |
| `GET` | `/categorias/{id}` | Busca categoria por ID | Autenticado |
| `PUT` | `/categorias/{id}` | Atualização completa | ADMIN |
| `PATCH` | `/categorias/{id}` | Atualização parcial (campos nulos ignorados) | ADMIN |
| `DELETE` | `/categorias/{id}` | Remove categoria | ADMIN |

### Modelo: Categoria

| Atributo | Tipo | Descrição |
|---|---|---|
| `id` | Long | Identificador único (PK) |
| `nome` | String | Nome da categoria. Obrigatório, único |
| `descricao` | String | Descrição textual. Opcional |
| `ativo` | Boolean | Liga/desliga a categoria sem apagar. Default `true` |
| `categoriaPaiId` | Long (nullable) | ID da categoria pai (subcategorias). `null` = categoria raiz |
| `criadoEm` | DateTime | Data de criação (gerado automaticamente) |
| `atualizadoEm` | DateTime | Data da última alteração (gerado automaticamente) |

## Como visualizar / validar o contrato

Requer Node.js (usa `npx`, sem instalação permanente).

**Validar (lint):**
```powershell
npx @redocly/cli@latest lint openapi.yaml
```

**Gerar documentação HTML estática:**
```powershell
npx @redocly/cli@latest build-docs openapi.yaml -o swagger.html
start swagger.html
```

**Editar/visualizar online (interativo):** cole o conteúdo de `openapi.yaml` em https://editor.swagger.io

