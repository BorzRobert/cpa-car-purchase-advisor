package ro.cpa.advisor.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.cpa.advisor.domain.SessionPrincipal;
import ro.cpa.advisor.domain.UsageSummary;
import ro.cpa.advisor.service.UsageService;

@RestController
@RequestMapping(value = "/api/v1/usage", produces = "application/json")
@Tag(name = "Usage")
public class UsageController {

    private final UsageService usage;

    public UsageController(UsageService usage) {
        this.usage = usage;
    }

    @GetMapping
    @Operation(summary = "Today's token usage", description = "Usage of this session for the current UTC day against the daily budget.")
    public UsageSummary today(@AuthenticationPrincipal SessionPrincipal principal) {
        return usage.summary(principal.sessionId());
    }
}
