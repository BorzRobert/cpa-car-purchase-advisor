package ro.cpa.advisor.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.cpa.advisor.service.StatusService;

/**
 * Primul endpoint al contractului `/api/v1`. Frontend-ul il apeleaza la incarcare ca sa stie daca backend-ul
 * raspunde si daca consilierul e configurat (Faza 3 il foloseste ca dovada CORS + HTTPS cap-coada).
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Status")
public class HealthController {

    private final StatusService statusService;

    public HealthController(StatusService statusService) {
        this.statusService = statusService;
    }

    @GetMapping("/health")
    @Operation(summary = "Service status", description = "Reports that the backend is up and whether the advisor is configured.")
    public HealthResponse health() {
        StatusService.ServiceStatus status = statusService.currentStatus();
        return new HealthResponse("UP", "cpa-backend", status.version(), status.advisorConfigured());
    }
}
