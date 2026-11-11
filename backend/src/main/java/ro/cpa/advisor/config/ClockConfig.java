package ro.cpa.advisor.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Ceasul este o dependenta injectata, ca bugetul zilnic si expirarile sa se poata testa fara sa asteptam miezul noptii. */
@Configuration
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
