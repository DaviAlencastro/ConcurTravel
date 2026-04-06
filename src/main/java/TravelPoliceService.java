package com.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TravelPoliceService {

    private final Resource travelPolicyResource;
    private final HuggingFaceEmbeddingModel embeddingModel;
    private final ChatLanguageModel chatModel;
    private final InMemoryEmbeddingStore embeddingStore = new InMemoryEmbeddingStore();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private List<TextSegment> policySegments = Collections.emptyList();

    public TravelPoliceService(
            @Value("${travel.policy.path:classpath:travel-policy.txt}") Resource travelPolicyResource,
            HuggingFaceEmbeddingModel embeddingModel,
            OllamaChatModel chatModel) {
        this.travelPolicyResource = travelPolicyResource;
        this.embeddingModel = embeddingModel;
        this.chatModel = chatModel;
    }

    @PostConstruct
    public void initialize() {
        String travelPolicyText = loadTravelPolicyDocument();
        indexTravelPolicy(travelPolicyText);
    }

    public String assessExpenseJson(String expenseDescription) {
        TravelExpenseRequest request = new TravelExpenseRequest(expenseDescription);
        TravelPolicyResponse response = assessExpense(request);

        try {
            return objectMapper.writeValueAsString(response);
        } catch (IOException e) {
            throw new TravelPolicyException("Falha ao serializar a resposta JSON", e);
        }
    }

    public TravelPolicyResponse assessExpense(TravelExpenseRequest request) {
        if (request == null || request.getExpenseDescription() == null || request.getExpenseDescription().isBlank()) {
            throw new TravelPolicyException("Descrição da despesa é obrigatória");
        }

        try {
            Embedding queryEmbedding = embeddingModel.embed(request.getExpenseDescription()).content();
            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(1)
                    .build();
            EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);

            if (searchResult.matches().isEmpty()) {
                throw new TravelPolicyException("Nenhuma regra de política foi encontrada");
            }

            EmbeddingMatch<TextSegment> bestMatch = searchResult.matches().get(0);
            String policyContext = bestMatch.embedded().text();

            String prompt = buildPrompt(request.getExpenseDescription(), policyContext);
            String modelResult = chatModel.generate(prompt);

            // Parse JSON response
            JsonNode jsonNode = objectMapper.readTree(modelResult);
            boolean allowed = jsonNode.get("allowed").asBoolean();
            String reason = jsonNode.get("reason").asText();
            String matchedPolicyRule = jsonNode.get("matchedPolicyRule").asText();

            return new TravelPolicyResponse(
                    allowed,
                    reason,
                    matchedPolicyRule,
                    modelResult
            );
        } catch (Exception e) {
            throw new TravelPolicyException("Erro ao processar a despesa", e);
        }
    }

    private String buildPrompt(String expenseDescription, String policyContext) {
        return "Baseado APENAS no contexto fornecido, responda se a despesa é permitida ou negada e cite o motivo.\n"
                + "Contexto da política: " + policyContext + "\n"
                + "Despesa: " + expenseDescription + "\n"
                + "Retorne apenas JSON com campos: allowed (boolean), reason (string), matchedPolicyRule (string).";
    }

    private String loadTravelPolicyDocument() {
        try {
            if (travelPolicyResource != null && travelPolicyResource.exists()) {
                return new String(travelPolicyResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException ignored) {
            // Fallback to file system if classpath resource is not available.
        }

        try {
            Path fallback = Path.of("Kubernetes", "travel-policy.txt");
            return Files.readString(fallback, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new TravelPolicyException("Não foi possível carregar o documento de política de viagem", e);
        }
    }

    private void indexTravelPolicy(String policyText) {
        List<TextSegment> segments = parsePolicySegments(policyText);
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        for (int i = 0; i < segments.size(); i++) {
            embeddingStore.add(embeddings.get(i), segments.get(i));
        }
        this.policySegments = Collections.unmodifiableList(segments);
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
    private final String expenseDescription;

    public TravelExpenseRequest(String expenseDescription) {
        this.expenseDescription = expenseDescription;
    }

    public String getExpenseDescription() {
        return expenseDescription;
    }
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

    public boolean isAllowed() {
        return allowed;
    }

    public String getReason() {
        return reason;
    }

    public String getMatchedPolicyRule() {
        return matchedPolicyRule;
    }

    public String getRawModelOutput() {
        return rawModelOutput;
    }
}

class TravelPolicyException extends RuntimeException {
    public TravelPolicyException(String message) {
        super(message);
    }

    public TravelPolicyException(String message, Throwable cause) {
        super(message, cause);
    }
}
