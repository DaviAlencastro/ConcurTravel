package Kubernetes;

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
}