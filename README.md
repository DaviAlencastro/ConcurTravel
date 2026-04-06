# Travel Policy Service

Protótipo de serviço para validar despesas de viagem usando RAG (Retrieval-Augmented Generation) com LangChain4j e OpenAI.

## Tecnologias

- Java 17
- Spring Boot 3
- LangChain4j 0.31.0
- OpenAI API

## Pré-requisitos

- Java 17 ou superior instalado
- Maven 3.6+ instalado (ou use o wrapper Maven)
- Chave da API OpenAI

## Como executar

1. Clone ou baixe o projeto.

2. Configure a variável de ambiente `OPENAI_API_KEY` com sua chave da API OpenAI:
   ```bash
   export OPENAI_API_KEY=your_api_key_here
   ```

3. Execute o projeto com Maven:
   ```bash
   mvn spring-boot:run
   ```

   Ou, se Maven não estiver instalado globalmente, use o wrapper (se disponível):
   ```bash
   ./mvnw spring-boot:run
   ```

4. O serviço estará disponível em `http://localhost:8080`.

## API

### POST /api/travel-policy/assess

Valida uma despesa contra a política de viagem.

**Request Body:**
```json
{
  "expenseDescription": "Jantar de luxo em Paris por R$1000"
}
```

**Response:**
```json
{
  "allowed": false,
  "reason": "Despesa excede o limite permitido para refeições",
  "matchedPolicyRule": "Regra 1: Despesas com refeições...",
  "rawModelOutput": "..."
}
```

## Estrutura do Projeto

- `src/main/java/Kubernetes/TravelApplication.java`: Classe principal Spring Boot
- `src/main/java/Kubernetes/TravelPoliceService.java`: Serviço de validação de despesas
- `src/main/java/Kubernetes/TravelController.java`: Controlador REST
- `src/main/resources/travel-policy.txt`: Documento de política de viagem
- `src/main/resources/application.properties`: Configurações