package com.example;

import dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingConfig {

    @Bean
    public HuggingFaceEmbeddingModel embeddingModel() {
        return HuggingFaceEmbeddingModel.builder()
                .modelId("sentence-transformers/all-MiniLM-L6-v2")
                .build();
    }
}