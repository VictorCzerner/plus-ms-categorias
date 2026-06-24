# ADR-001: Arquitetura do microsserviço de categorias

## Status

Proposta

## Contexto

O `plus-ms-categorias` é o microsserviço do sistema Plus responsável pelo gerenciamento de categorias de produtos. Seu escopo  compreende a criação, consulta, atualização e exclusão de categorias, incluindo o relacionamento de hierarquia entre uma categoria e sua categoria pai. A associação entre produtos e categorias não pertence a este serviço e é atribuída ao microsserviço de produtos, conforme indicado na documentação existente.

O serviço precisa expor um contrato REST para consumo por outros componentes do sistema. As operações de leitura devem estar disponíveis para usuários autenticados, enquanto as operações que modificam categorias precisam ser restritas a usuários administradores.

A autenticação é integrada ao `plus-ms-auth` por meio de tokens JWT Bearer. O `plus-ms-categorias` não consulta o serviço de autenticação durante cada requisição: ele valida localmente a assinatura e a expiração do token usando um segredo compartilhado configurado por `JWT_SECRET`.

As categorias são persistidas em banco relacional próprio. O modelo permite categorias raiz e subcategorias por meio de uma relação autorreferenciada opcional. O nome é obrigatório e único, a categoria pode ser marcada como ativa ou inativa e os registros possuem datas de criação e atualização.

O contrato da API é descrito em `openapi.yaml` e apresentado no projeto como uma especificação design-first. Existem, contudo, divergências conhecidas entre esse contrato, o README e a implementação atual, registradas nesta ADR.

## Decisão

As decisões abaixo estão efetivamente sustentadas pela implementação atual:

- Implementar o componente como um microsserviço independente em Java 21 e Spring Boot.
- Expor uma API REST com recurso base `/categorias`.
- Organizar o código nas responsabilidades Controller, Service, Repository, DTO, Entity, segurança e tratamento de exceções.
- Manter no Controller o tratamento HTTP, no Service as regras de negócio e transações, e no Repository o acesso aos dados.
- Usar PostgreSQL como banco de dados relacional próprio do microsserviço.
- Usar Spring Data JPA e Hibernate para mapeamento e persistência das entidades.
- Configurar o Hibernate com `ddl-auto=validate`, sem delegar a ele a criação evolutiva do schema.
- Usar Flyway para criar e versionar o schema do banco.
- Representar a hierarquia de categorias com uma chave estrangeira autorreferenciada opcional, `categoria_pai_id`.
- Expor DTOs distintos da entidade de persistência para entrada, resposta individual e resposta paginada.
- Usar autenticação stateless por JWT Bearer por meio do Spring Security OAuth2 Resource Server.
- Desabilitar criação de sessão de segurança no servidor e validar os tokens em cada requisição.
- Validar tokens localmente com HMAC SHA-256 e segredo compartilhado compatível com o `plus-ms-auth`, sem chamada HTTP direta ao serviço de autenticação.
- Exigir assinatura HS256 válida e claim de expiração válida para aceitar o token.
- Extrair a autorização da claim `role`, normalizando-a para uma authority Spring com prefixo `ROLE_`.
- Permitir as operações `GET /categorias` e `GET /categorias/{id}` para qualquer usuário autenticado, inclusive quando o token não contém uma role.
- Restringir as operações implementadas de escrita — `POST`, `PUT` e `DELETE` — à role `ADMIN`, tanto por regras HTTP quanto por `@PreAuthorize` nos métodos correspondentes do controller.
- Usar exclusão física na implementação atual e impedir a exclusão de uma categoria que possua subcategorias diretas.
- Impedir que uma categoria seja definida diretamente como pai de si mesma.
- Manter uma especificação OpenAPI 3.0.3 em arquivo estático como contrato documentado da API.
- Manter testes automatizados para Service, Controller/API, configuração de segurança, decoder JWT e tratamento global de exceções.


## Alternativas consideradas

### Validar o token consultando o `plus-ms-auth` a cada requisição

Não corresponde à implementação atual. A validação local evita dependência síncrona do serviço de autenticação no caminho de cada requisição e reduz latência e acoplamento em runtime. 

### Usar sessão e cookie em vez de JWT stateless

Não corresponde à implementação atual, que configura `SessionCreationPolicy.STATELESS` e autenticação Bearer. Sessões exigiriam armazenamento e coordenação de estado de autenticação entre instâncias. 

### Usar banco NoSQL

Não corresponde à implementação atual. O modelo possui restrições relacionais, unicidade de nome e uma referência hierárquica entre categorias, características atendidas diretamente pelo PostgreSQL. 

### Não usar Flyway

Não corresponde à implementação atual. Sem Flyway, a evolução do schema dependeria de execução manual ou de geração automática pelo ORM. O projeto opta por migrações versionadas e configura o Hibernate apenas para validar o schema. 

### Compartilhar o banco com outros microsserviços

Não há evidência de compartilhamento na configuração atual: o serviço usa datasource e banco `categorias` próprios. Um banco compartilhado aumentaria o acoplamento de schema e permitiria que outros serviços contornassem a API do microsserviço. 

### Implementar categorias dentro do microsserviço de produtos

Não corresponde à separação documentada: este serviço gerencia categorias, enquanto a associação entre produtos e categorias é atribuída ao microsserviço de produtos.

### Usar o gateway para toda a autorização

Não corresponde à implementação atual, pois o próprio microsserviço autentica tokens e aplica autorização por método e role. Centralizar toda a autorização no gateway reduziria controles locais e faria a proteção depender da impossibilidade de acesso direto ao serviço. 

### Usar JWT assimétrico com chave pública e privada

Não corresponde à implementação atual, baseada em HS256 e segredo compartilhado. Assinatura assimétrica permitiria que apenas o serviço de autenticação mantivesse a chave privada, enquanto os consumidores validariam tokens com chave pública. Essa alternativa pode ser considerada como evolução para reduzir a distribuição de segredos de assinatura.

## Consequências positivas

- A validação local elimina chamada síncrona ao `plus-ms-auth` durante as requisições autenticadas.
- A autenticação stateless facilita a execução de múltiplas instâncias sem armazenamento compartilhado de sessão.
- A separação entre Controller, Service, Repository, DTO e Entity torna explícitas as responsabilidades técnicas.
- O isolamento do serviço de categorias reduz o acoplamento com o domínio de produtos.
- PostgreSQL e o modelo relacional suportam unicidade, integridade referencial e a hierarquia autorreferenciada.
- Spring Data JPA reduz código de acesso a dados, enquanto as transações ficam definidas no Service.
- Flyway fornece histórico versionado e reproduzível do schema.
- A validação do schema pelo Hibernate ajuda a detectar incompatibilidades entre entidades e banco.
- O contrato OpenAPI torna endpoints, payloads, respostas e requisitos de autenticação mais explícitos para consumidores.
- Os testes automatizados cobrem regras de negócio, respostas HTTP, autenticação, autorização, validação JWT e respostas de erro.

## Consequências negativas / riscos

Os itens desta seção são consequências e riscos da arquitetura atual;

- O uso de HS256 exige que emissor e validadores conheçam o mesmo segredo. O comprometimento de um consumidor que possua esse segredo pode permitir a emissão indevida de tokens válidos.
- A operação depende da sincronização correta de `JWT_SECRET` entre o `plus-ms-auth` e este microsserviço.
- Não há estratégia de rotação de segredo implementada ou documentada. Uma troca não coordenada pode invalidar tokens ou interromper autenticação.
- O decoder valida algoritmo, assinatura e expiração, mas não valida `issuer`, `audience` ou uma claim que identifique o tipo do token.
- Um token de refresh com estrutura, assinatura e claims compatíveis pode ser aceito como access token. Esse comportamento está documentado por testes.
- O logout ou a revogação no `plus-ms-auth` não invalida imediatamente tokens já emitidos e ainda não expirados, pois não há introspecção ou lista local de revogação.
- A implementação impede apenas a autorreferência direta. Ciclos indiretos na hierarquia podem ser criados e esse comportamento também está documentado por teste.
- A exclusão é física e pode causar perda definitiva do registro. Não existe mecanismo de soft delete.
- A regra de unicidade não executa normalização explícita de espaços ou caixa antes da persistência. O comportamento definitivo esperado para nomes equivalentes não está definido.
- O contrato OpenAPI é mantido separadamente da implementação e pode divergir caso não exista validação automatizada entre ambos.
- O endpoint `PATCH` é documentado no README e no OpenAPI e possui regra de autorização, mas não está implementado no Controller.
- Não há configuração explícita de CORS no projeto. A responsabilidade entre microsserviço e gateway precisa ser definida.
- As rotas de Swagger e Actuator são liberadas pela configuração de segurança, mas o build atual não declara dependências de Springdoc/OpenAPI UI nem de Actuator.

## Decisões pendentes

Os itens abaixo não são decisões aprovadas nesta ADR. Eles exigem confirmação ou decisão posterior da equipe:

- Definir a política definitiva de emissão, uso e validação de refresh tokens.
- Incluir e validar uma claim como `token_type` para distinguir access token de refresh token.
- Definir estratégia de rotação de segredo, incluindo período de transição e armazenamento seguro.
- Definir e validar `issuer` e `audience`.
- Implementar o endpoint `PATCH` conforme o contrato ou removê-lo do README, OpenAPI e regras de segurança.
- Decidir entre exclusão física e exclusão lógica de categorias.
- Definir normalização de nomes, incluindo tratamento de espaços, caixa e caracteres equivalentes.
- Implementar regra contra ciclos indiretos na hierarquia ou confirmar formalmente que esses ciclos são permitidos.
- Definir se CORS é responsabilidade deste microsserviço, do gateway ou de ambos.
- Definir se Swagger UI e Actuator devem estar disponíveis em runtime e em quais ambientes.
- Confirmar a política arquitetural de banco exclusivo por microsserviço.
- Confirmar o papel do gateway na autenticação e autorização, mantendo ou ajustando a defesa local.

## Impacto nos testes

As decisões implementadas são verificadas pelos seguintes grupos de testes:

- `CategoriaServiceTest`: cobre listagem e paginação, consulta por identificador, criação, atualização, exclusão, nomes duplicados, categoria pai inexistente, valor padrão de `ativo`, autorreferência direta e bloqueio de exclusão quando existem subcategorias. Também documenta que ciclos indiretos são atualmente aceitos.
- `CategoriaControllerTest`: cobre respostas e payloads da API, autenticação obrigatória, assinatura inválida, autorização de leitura e escrita, validação de entrada, códigos `201`, `204`, `400`, `401`, `403`, `404` e `409`. Também documenta que `PATCH` retorna `405` e que um token semelhante a refresh token é aceito.
- `SecurityConfigTest`: cobre compatibilidade do decoder com HS256 e a conversão da claim `role` para authorities `ROLE_*`, incluindo tokens sem role.
- `HmacSha256JwtDecoderTest`: cobre token válido, formato inválido, algoritmo diferente de HS256, assinatura inválida, expiração, ausência de `exp` e aceitação atual de token semelhante a refresh token.
- `GlobalExceptionHandlerTest`: cobre a estrutura das respostas para recurso não encontrado, conflito e falha de validação.

Ainda precisam ser avaliados ou adicionados, conforme decisões futuras:

- Testes de integração com PostgreSQL real ou containerizado e execução das migrações Flyway.
- Testes de integração dos queries e filtros do Repository.
- Testes E2E com tokens emitidos por uma instância real do `plus-ms-auth`.
- Testes de contrato que comparem automaticamente o OpenAPI com a implementação.
- Testes de `issuer`, `audience` e `token_type`, caso essas validações sejam adotadas.
- Testes de rotação de segredo e revogação, conforme a política definida.
- Testes de prevenção de ciclos indiretos, caso a regra seja implementada.
- Testes do comportamento de CORS e do roteamento pelo gateway.
- Testes de runtime para Swagger UI e Actuator, caso essas dependências sejam adicionadas.

## Observações

Foram identificadas as seguintes divergências entre README, OpenAPI e implementação:

- O README declara que o backend Spring Boot ainda não foi iniciado, mas o projeto já contém implementação completa de Controller, Service, Repository, Entity, segurança, migração e testes.
- README e OpenAPI documentam `PATCH /categorias/{id}` como atualização parcial. A implementação não possui método `@PatchMapping`; o teste de Controller confirma a resposta atual `405 Method Not Allowed`.
- A configuração de segurança possui autorização para `PATCH`, embora não exista endpoint correspondente.
- O OpenAPI limita o parâmetro `size` ao máximo de 100. A implementação usa `PageRequest.of` e não aplica explicitamente esse limite máximo.
- A configuração de segurança permite acesso público a `/swagger-ui/**`, `/v3/api-docs/**` e `/actuator/health`, mas o build não contém dependências de Springdoc ou Actuator.
- O README apresenta `openapi.yaml` como fonte da verdade, porém não há validação automatizada que impeça divergências entre o contrato e o código.
- O OpenAPI descreve respostas `401` e `403` com o schema `Erro`, mas o tratamento global customizado cobre apenas recurso não encontrado, conflito e validação. O formato efetivo das respostas geradas pelo Spring Security pode divergir desse schema.
- O OpenAPI declara um endereço via gateway em `/categorizacao`, mas esse roteamento não é configurado neste repositório: **pendente de confirmação pela equipe**.
