package ro.cpa.advisor.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class CpaPropertiesTest {

    @Test
    void rejectsWildcardOrigin() {
        assertThatThrownBy(() -> new CpaProperties.Cors(List.of("*"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CpaProperties.Cors(List.of("https://*.vercel.app"))).isInstanceOf(IllegalArgumentException.class);
    }
}
