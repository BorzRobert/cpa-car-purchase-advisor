package ro.cpa.advisor.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ro.cpa.advisor.domain.AdvisorNotConfiguredException;

class AdvisorAvailabilityTest {

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "your_openai_api_key"})
    void missingOrPlaceholderKeyMeansNotConfigured(String key) {
        AdvisorAvailability availability = new AdvisorAvailability(key);

        assertThat(availability.isConfigured()).isFalse();
        assertThatThrownBy(availability::requireConfigured).isInstanceOf(AdvisorNotConfiguredException.class);
    }

    @Test
    void realLookingKeyMeansConfigured() {
        AdvisorAvailability availability = new AdvisorAvailability("sk-test-value");

        assertThat(availability.isConfigured()).isTrue();
        availability.requireConfigured();
    }
}
