package ro.cpa.advisor.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;
import ro.cpa.advisor.ai.AdvisorAvailability;

/**
 * Starea serviciului pentru ecranul de pornire al frontend-ului. Controllerele nu ating direct stratul `ai`,
 * deci trecem prin serviciu (regula de straturi verificata de ArchUnit).
 */
@Service
public class StatusService {

    private final AdvisorAvailability advisorAvailability;
    private final ObjectProvider<BuildProperties> buildProperties;

    public StatusService(AdvisorAvailability advisorAvailability, ObjectProvider<BuildProperties> buildProperties) {
        this.advisorAvailability = advisorAvailability;
        this.buildProperties = buildProperties;
    }

    public ServiceStatus currentStatus() {
        // BuildProperties lipseste cand rulezi din IDE fara `mvn package`; nu e motiv sa pice endpoint-ul.
        BuildProperties build = buildProperties.getIfAvailable();
        return new ServiceStatus(build != null ? build.getVersion() : "unknown", advisorAvailability.isConfigured());
    }

    public record ServiceStatus(String version, boolean advisorConfigured) {}
}
