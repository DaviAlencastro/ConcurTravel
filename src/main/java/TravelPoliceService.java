package com.example;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TravelPoliceService {

    private final Resource travelPolicyResource;
    private final EmbeddingModel embeddingModel;
    private final ChatLanguageModel chatModel;
    // Corrigido: Simplificado o tipo genérico para o Store
    private final InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TravelPoliceService(
            @Value("${travel.policy.path:classpath:travel-policy.txt}") Resource travelPolicyResource, 
            EmbeddingModel embeddingModel, 
            ChatLanguageModel chatModel) {
        this.travelPolicyResource = travelPolicyResource;
        this.embeddingModel = embeddingModel;
        this.chatModel = chatModel;
    }

    @PostConstruct
    public void initialize() {
        try {
            String travelPolicyText = loadTravelPolicyDocument();
            indexTravelPolicy(travelPolicyText);
            System.out.println("✅ Política de viagem indexada com sucesso!");
        } catch (Exception e) {
            System.err.println("❌ Erro ao inicializar política: " + e.getMessage());
        }
    }

    public String assessExpenseJson(String expenseDescription) {
        try {
            TravelExpenseRequest request = new TravelExpenseRequest(expenseDescription);
            TravelPolicyResponse response = assessExpense(request);
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    public TravelPolicyResponse assessExpense(TravelExpenseRequest request) {
        if (request == null || request.getExpenseDescription() == null || request.getExpenseDescription().isBlank()) {
            throw new TravelPolicyException("Descrição da despesa é obrigatória");
        }

        try {
            // 1. Busca semântica (RAG)
            Embedding queryEmbedding = embeddingModel.embed(request.getExpenseDescription()).content();
            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(1)
                    .build();
            
            // CORREÇÃO AQUI: Simplificado o tipo do resultado
            EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);

            if (searchResult.matches().isEmpty()) {
                throw new TravelPolicyException("Nenhuma regra de política foi encontrada");
            }

            // CORREÇÃO AQUI: Acesso correto ao texto via .embedded().text()
            String policyContext = searchResult.matches().get(0).embedded().text();

            // 2. Geração da IA
            String prompt = buildPrompt(request.getExpenseDescription(), policyContext);
            String modelResult = chatModel.generate(prompt);

            // 3. Limpeza do JSON
            String cleanJson = modelResult.replace("```json", "").replace("```", "").trim();
            if (cleanJson.contains("{") && cleanJson.contains("}")) {
                cleanJson = cleanJson.substring(cleanJson.indexOf("{"), cleanJson.lastIndexOf("}") + 1);
            }

            // 4. Parse do JSON
            JsonNode jsonNode = objectMapper.readTree(cleanJson);
            boolean allowed = jsonNode.has("allowed") && jsonNode.get("allowed").asBoolean();
            String reason = jsonNode.has("reason") ? jsonNode.get("reason").asText() : "Motivo não fornecido";
            String matchedRule = jsonNode.has("matchedPolicyRule") ? jsonNode.get("matchedPolicyRule").asText() : "N/A";

            return new TravelPolicyResponse(allowed, reason, matchedRule, modelResult);

        } catch (Exception e) {
            throw new TravelPolicyException("Erro no processamento: " + e.getMessage(), e);
        }
    }

    private String buildPrompt(String expenseDescription, String policyContext) {
    return "### INSTRUÇÃO SISTÊMICA ###\n"
            + "Você é o validador automático de despesas da SAP Concur.\n"
            + "Sua resposta deve conter UNICAMENTE o objeto JSON. É PROIBIDO adicionar saudações, explicações ou sugestões de exercícios.\n\n"
            + "### CONTEXTO DA POLÍTICA ###\n" + policyContext + "\n\n"
            + "### DESPESA PARA ANÁLISE ###\n" + expenseDescription + "\n\n"
            + "### FORMATO OBRIGATÓRIO (JSON PURO) ###\n"
            + "{ \"allowed\": boolean, \"reason\": \"string\", \"matchedPolicyRule\": \"string\" }";
    }

    private String loadTravelPolicyDocument() {
        try {
            if (travelPolicyResource != null && travelPolicyResource.exists()) {
                return new String(travelPolicyResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException ignored) {}

        try {
            Path fallback = Path.of("travel-policy.txt");
            if (Files.exists(fallback)) return Files.readString(fallback, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new TravelPolicyException("Arquivo de política não encontrado.");
        }
        return "Nenhuma política disponível.";
    }

    private void indexTravelPolicy(String policyText) {
        List<TextSegment> segments = parsePolicySegments(policyText);
        for (TextSegment segment : segments) {
            Embedding embedding = embeddingModel.embed(segment).content();
            embeddingStore.add(embedding, segment);
        }
    }

    private List<TextSegment> parsePolicySegments(String policyText) {
        return List.of(policyText.split("\r?\n\r?\n"))
                .stream()
                .map(String::trim)
                .filter(block -> !block.isBlank())
                .map(TextSegment::from)
                .collect(Collectors.toList());
    }
}

class TravelExpenseRequest {
    private String expenseDescription;
    public TravelExpenseRequest() {}
    public TravelExpenseRequest(String desc) { this.expenseDescription = desc; }
    public String getExpenseDescription() { return expenseDescription; }
    public void setExpenseDescription(String d) { this.expenseDescription = d; }
}

class TravelPolicyResponse {
    private final boolean allowed;
    private final String reason;
    private final String matchedPolicyRule;
    private final String rawModelOutput;

    public TravelPolicyResponse(boolean allowed, String reason, String matchedPolicyRule, String rawModelOutput) {
        this.allowed = allowed;
        this.reason = reason;
        this.matchedPolicyRule = matchedPolicyRule;
        this.rawModelOutput = rawModelOutput;
    }

    public boolean isAllowed() { return allowed; }
    public String getReason() { return reason; }
    public String getMatchedPolicyRule() { return matchedPolicyRule; }
    public String getRawModelOutput() { return rawModelOutput; }
}

class TravelPolicyException extends RuntimeException {
    public TravelPolicyException(String m) { super(m); }
    public TravelPolicyException(String m, Throwable c) { super(m, c); }
}