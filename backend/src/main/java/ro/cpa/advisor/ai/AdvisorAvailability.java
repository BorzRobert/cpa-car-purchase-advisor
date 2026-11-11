package ro.cpa.advisor.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ro.cpa.advisor.domain.AdvisorNotConfiguredException;

/**
 * Raspunde la o singura intrebare: avem o cheie OpenAI utilizabila? Aplicatia porneste si fara ea, iar restul
 * codului intreaba aici in loc sa citeasca cheia. Valoarea cheii nu paraseste aceasta clasa.
 */
@Component
public class AdvisorAvailability {

    // Valoarea-substituent din .env.example nu e o cheie; fara aceasta verificare am raporta "configurat" gresit.
    private static final String PLACEHOLDER = "your_openai_api_key";

    private final boolean configured;

    public AdvisorAvailability(@Value("${spring.ai.openai.api-key:}") String apiKey) {
        this.configured = apiKey != null && !apiKey.isBlank() && !PLACEHOLDER.equals(apiKey.trim());
    }

    public boolean isConfigured() {
        return configured;
    }

    public void requireConfigured() {
        if (!configured) {
            throw new AdvisorNotConfiguredException();
        }
    }
}
