// package com.example;

// import dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;

// @Configuration
// public class EmbeddingConfig {

//     @Bean
//     public HuggingFaceEmbeddingModel embeddingModel(@Value("${langchain4j.hugging-face.embedding-model.access-token:}") String accessToken) {
//         return HuggingFaceEmbeddingModel.builder()
//                 .modelId("sentence-transformers/all-MiniLM-L6-v2")
//                 .accessToken(accessToken.isEmpty() ? null : accessToken)
//                 .build();
//     }
// }