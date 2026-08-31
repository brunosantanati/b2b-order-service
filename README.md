# 🚀 B2B Order Processing Service

Seja bem-vindo ao repositório do **B2B Order Processing Service**. Este microsserviço foi desenvolvido para gerenciar o ciclo de vida de parceiros comerciais e processar pedidos B2B de forma resiliente, escalável e com alta performance.

Desenvolvi esta solução focando em padrões modernos de arquitetura de software, garantindo consistência financeira no controle de limites de crédito e mensageria assíncrona para notificação de eventos.

---

## 🛠️ Tecnologias e Ferramentas

- **Java 21** (Virtual Threads / Project Loom)
- **Spring Boot 3.x** (Spring Data MongoDB, Spring Kafka, Spring Validation)
- **MongoDB 8.0** (Comunicação reativa e atômica)
- **Apache Kafka** (Event-Driven Architecture)
- **Testcontainers & RestAssured** (Testes de integração com infraestrutura real em container)
- **JUnit 5, Mockito e AssertJ** (Testes unitários)
- **Docker & Docker Compose** (Orquestração de ambiente local)
- **Springdoc OpenAPI / Swagger UI** (Documentação viva da API)

---

## 🏛️ Decisões Arquiteturais e de Performance

Ao desenhar a arquitetura do projeto, foquei em resolver dois dos maiores desafios de sistemas de processamento de pedidos: **alta vazão de requisições** e **concorrência ao debitar/estornar limites de crédito dos parceiros**.

### 1. Virtual Threads (Java 21)
Optei por habilitar as **Virtual Threads** (`spring.threads.virtual.enabled=true`). Elas permitem que cada requisição HTTP receba uma thread leve gerenciada pela JVM em vez de uma thread do sistema operacional. Com isso, operações de I/O bloqueantes (como consultas ao MongoDB ou publicação no Kafka) não saturam a máquina, permitindo que a aplicação atinja alta vazão de throughput com baixo consumo de memória.

### 2. Controle de Concorrência sem Locks (Operações Atômicas no MongoDB)
Um dos requisitos cruciais em um sistema B2B é impedir que um parceiro realize compras além do seu limite de crédito em requisições simultâneas (*race conditions*).

Em vez de utilizar *pessimistic locking* ou *distributed locks* (que introduzem latência e potenciais gargalos em cenários de alta concorrência), optei por realizar atualizações atômicas diretamente no MongoDB com **Conditional Updates (`findAndModify` / `updateFirst`)**:

- **Debitar Limite:** O débito ocorre em uma única operação de banco que verifica atomicamente se o parceiro possui saldo suficiente antes de decrementar:
  ```javascript
  db.partners.updateOne(
    { _id: partnerId, availableLimit: { $gte: orderAmount } },
    { $inc: { availableLimit: -orderAmount } }
  );
  ```
  Se dois pedidos tentarem consumir o saldo simultaneamente, a operação atômica do MongoDB garante que apenas as transações válidas atualizem o documento. Se o número de documentos modificados for `0`, a aplicação identifica a falha de saldo e lança `InsufficientCreditException`.
- **Estorno Atômico:** No cancelamento do pedido, o valor é somado novamente ao `availableLimit` utilizando o operador `$inc` positivo de forma atômica.

### 3. Event-Driven Architecture (EDA) com Apache Kafka
Sempre que um pedido altera seu estado (seja na criação como `PENDING`, no cancelamento como `CANCELLED` ou nas transições de status como `APPROVED`, `DELIVERED`), a aplicação publica um evento no tópico `b2b-order-status-events`. Isso desacopla o serviço de pedidos dos demais microsserviços da empresa (como faturamento, estoque ou notificações).

---

## 🧪 Estratégia de Testes Automatizados

Devido ao prazo de entrega enxuto, concentrei a estratégia de testes no topo e no meio da pirâmide de testes para garantir a máxima confiabilidade no menor tempo possível:

### 1. Testes de Integração (`src/test/java/.../integration`)
A estratégia foi utilizar **Testcontainers** para subir instâncias reais do **MongoDB 8.0** e do **Apache Kafka** em containers Docker efêmeros durante a execução do Maven.
- **`CreatePartnerIntegrationTest`:** Testa o contrato HTTP e a persistência real do parceiro no banco de dados.
- **`CreateOrderIntegrationTest`:** Valida o fluxo de criação do pedido com **RestAssured**, confirma a gravação no MongoDB e utiliza um consumer Kafka efêmero para validar se a mensagem foi devidamente publicada no tópico `b2b-order-status-events`.

### 2. Testes Unitários (`src/test/java/.../service`)
Espelhados exatamente nos pacotes da aplicação (`br.com.vpsconsulting.b2b_order_service.service`), os testes unitários cobrem as regras de negócio de `PartnerService` e `OrderService`.
- Utilizam **JUnit 5**, **Mockito** e **AssertJ**.
- Estruturados com **`@Nested`** e **`@DisplayName`** para facilidade de leitura.

### 💡 Observação sobre Testes End-to-End (E2E)
Optou-se por **não incluir testes E2E estritos** (rodando contra infraestrutura de nuvem/Staging) por exigirem o provisionamento prévio de ambiente GCP/AWS (K8s, Kafka gerenciado e DocumentDB) ou um ciclo de deploy em pipeline, o que comprometeria o prazo de entrega. Os testes de integração com Testcontainers exercitam os mesmos contratos e integrações de infraestrutura de forma local e determinística.

---

## 🔗 Links Úteis do Ambiente Local

Após subir a aplicação via Docker Compose, as seguintes interfaces ficam disponíveis:

- **Swagger UI (Documentação da API):** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI Docs (JSON):** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- **Kafka UI (Gerenciamento do Cluster Kafka):** [http://localhost:8085](http://localhost:8085)
- **Mongo Express (Admin do MongoDB):** [http://localhost:8081](http://localhost:8081)

---

## 🚀 Como Rodar o Projeto

### Pré-requisitos
- **Docker** e **Docker Compose** instalados.
- **Java 21** e **Maven** (caso deseje rodar a aplicação via IDE).

### Opção 1: Executando Tudo via Docker Compose (Aplicação + Infraestrutura)
Para subir o banco de dados MongoDB, o Apache Kafka, as interfaces visuais e a aplicação pronta:

```bash
docker compose up --build -d
```

Para acompanhar os logs do serviço de pedidos em tempo real:
```bash
docker logs -f b2b_order_service_app
```

Para encerrar e remover os containers e volumes:
```bash
docker compose down -v
```

---

### Opção 2: Executando Infraestrutura no Docker e Aplicação pela IDE

1. **Configuração de Host Local (Apenas para resolução de nomes do Mongo Express/Kafka se necessário):**
   ```bash
   sudo sh -c 'echo "127.0.0.1 mongodb" >> /etc/hosts'
   ```

2. **Subir apenas a infraestrutura:**
   ```bash
   docker compose up -d mongodb mongo-express kafka kafka-ui
   ```

3. Execute a classe principal `B2bOrderServiceApplication` através da sua IDE favorita (IntelliJ / VS Code).

---

## 📡 Exemplos de Uso (cURL)

Abaixo estão os comandos cURL organizados para testar os fluxos da API manualmente:

### 1. Gerenciamento de Parceiros (Partners)

**Cadastrar Novo Parceiro:**
```bash
curl -X POST http://localhost:8080/api/v1/partners \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Empresa Comercial B2B LTDA",
    "cnpj": "12345678000199",
    "creditLimit": 50000.00
  }'
```

**Listar Todos os Parceiros:**
```bash
curl -X GET http://localhost:8080/api/v1/partners \
  -H "Accept: application/json"
```

**Buscar Parceiro por ID:**
```bash
curl -X GET http://localhost:8080/api/v1/partners/{partnerId} \
  -H "Accept: application/json"
```

---

### 2. Gerenciamento de Pedidos (Orders)

**Criar Novo Pedido:**
```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "partnerId": "{partnerId}",
    "items": [
      {
        "productId": "PROD-001",
        "quantity": 2,
        "unitPrice": 150.50
      },
      {
        "productId": "PROD-002",
        "quantity": 1,
        "unitPrice": 99.00
      }
    ]
  }'
```

**Buscar Pedido por ID:**
```bash
curl -X GET http://localhost:8080/api/v1/orders/{orderId}
```

**Listar Todos os Pedidos Sem Filtro:**
```bash
curl -X GET http://localhost:8080/api/v1/orders
```

**Consultas com Filtros Dinâmicos:**

- *Filtrar por ID do Pedido:*
  ```bash
  curl -X GET "http://localhost:8080/api/v1/orders?orderId={orderId}"
  ```

- *Filtrar por Parceiro:*
  ```bash
  curl -X GET "http://localhost:8080/api/v1/orders?partnerId={partnerId}"
  ```

- *Filtrar por Status:*
  ```bash
  curl -X GET "http://localhost:8080/api/v1/orders?status=APPROVED"
  ```

- *Filtrar por Período (Data Inicial e Final):*
  ```bash
  curl -X GET "http://localhost:8080/api/v1/orders?startDate=2026-08-01T00:00:00Z&endDate=2026-08-31T23:59:59Z"
  ```

- *Filtro Combinado Completo:*
  ```bash
  curl -X GET "http://localhost:8080/api/v1/orders?partnerId={partnerId}&status=APPROVED&startDate=2026-08-01T00:00:00Z&endDate=2026-08-31T23:59:59Z"
  ```

---

### 3. Atualização e Cancelamento de Pedidos

**Cancelar Pedido (Estorna Limite do Parceiro e Publica Evento no Kafka):**
```bash
curl -X PATCH http://localhost:8080/api/v1/orders/{orderId}/cancel
```

**Atualizar Status do Pedido:**
```bash
curl -X PATCH http://localhost:8080/api/v1/orders/{orderId}/status \
  -H "Content-Type: application/json" \
  -d '{
    "status": "DELIVERED"
  }'
```