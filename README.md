# Travel Policy Service

Protótipo de serviço para validar despesas de viagem usando RAG (Retrieval-Augmented Generation) com LangChain4j e Ollama.

## Tecnologias

- Java 17
- Spring Boot 3
- LangChain4j 0.31.0
- Ollama (Llama3 para chat)
- HuggingFace (all-MiniLM-L6-v2 para embeddings)

## Pré-requisitos

- Java 17 ou superior instalado
- Maven 3.6+ instalado (ou use o wrapper Maven)
- Ollama instalado e rodando (com modelo llama3)

## Como executar

1. Instale e inicie o Ollama: `ollama serve`
2. Baixe o modelo: `ollama pull llama3`
3. Clone ou baixe o projeto.
4. Execute o projeto com Maven:
   ```bash
   mvn spring-boot:run
   ```

   Ou, se Maven não estiver instalado globalmente, use o wrapper (se disponível):
   ```bash
   ./mvnw spring-boot:run
   ```

5. O serviço estará disponível em `http://localhost:8080`.

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