package ro.cpa.advisor.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;
import ro.cpa.advisor.domain.ForbiddenException;
import ro.cpa.advisor.domain.UnauthorizedException;
import ro.cpa.advisor.service.RateLimitService;
import ro.cpa.advisor.service.SessionService;

/**
 * Securitate fara stare: identitatea este un token de sesiune anonim in `Authorization: Bearer`, nu login si cookie-uri.
 * Tot ce nu este listat explicit ca public este refuzat — un endpoint nou trebuie deschis constient.
 */
@Configuration
@EnableConfigurationProperties(CpaProperties.class)
public class SecurityConfig {

    // Singurele cai deschise fara token. Totul din `/api/v1/**` cere sesiune; orice altceva este refuzat (denyAll).
    private static final String[] PUBLIC_PATHS = {
        "/api/v1/health", "/actuator/health/**", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**"
    };

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SessionService sessions,
            RateLimitService rateLimits,
            CpaProperties properties,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        SessionAuthenticationFilter sessionFilter = new SessionAuthenticationFilter(sessions);
        RateLimitFilter rateLimitFilter = new RateLimitFilter(rateLimits, exceptionResolver, properties.limits().trustedProxyHops());

        http.cors(Customizer.withDefaults())
                // Fara cookie-uri de sesiune nu exista ce sa fure prin CSRF; autentificarea este un header Bearer.
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(sessionFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(rateLimitFilter, SessionAuthenticationFilter.class)
                // 401/403 trec prin acelasi resolver ca restul erorilor, ca sa aiba formatul `ProblemDetail`.
                .exceptionHandling(e -> e.authenticationEntryPoint(
                                (request, response, ex) -> exceptionResolver.resolveException(request, response, null, new UnauthorizedException()))
                        .accessDeniedHandler(
                                (request, response, ex) -> exceptionResolver.resolveException(request, response, null, new ForbiddenException())))
                .authorizeHttpRequests(a -> a.requestMatchers(PUBLIC_PATHS)
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/sessions")
                        .permitAll()
                        .requestMatchers("/api/v1/**")
                        .authenticated()
                        .anyRequest()
                        .denyAll());
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(CpaProperties properties) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(properties.cors().allowedOrigins());
        cors.setAllowedMethods(List.of(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name()));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", RequestIdFilter.HEADER));
        // Retry-After trebuie expus explicit, altfel JavaScript-ul din browser nu-l poate citi la un 429.
        cors.setExposedHeaders(List.of(RequestIdFilter.HEADER, HttpHeaders.RETRY_AFTER));
        // Tokenul merge in header-ul Authorization, nu in cookie, deci nu avem nevoie de credentials.
        cors.setAllowCredentials(false);
        cors.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }

    // Un manager de utilizatori gol opreste Boot sa genereze si sa scrie in log o parola implicita fara rost.
    @Bean
    UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager();
    }
}
