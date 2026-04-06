package com.example;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/travel-policy")
public class TravelController {

    private final TravelPoliceService travelPoliceService;

    public TravelController(TravelPoliceService travelPoliceService) {
        this.travelPoliceService = travelPoliceService;
    }

    @PostMapping("/assess")
    public ResponseEntity<String> assessExpense(@RequestBody TravelExpenseRequest request) {
        try {
            String response = travelPoliceService.assessExpenseJson(request.getExpenseDescription());
            return ResponseEntity.ok(response);
        } catch (TravelPolicyException e) {
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @PostMapping("/check")
public ResponseEntity<String> check(@RequestBody SearchRequest request) {
    String response = travelPoliceService.processQuery(request.getQuery()); 
    return ResponseEntity.ok(response);
}

    // Uma classe simples (DTO) para o Spring converter o JSON automaticamente
    public static class SearchRequest {
        private String query;

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }
    }
}