package ro.cpa.advisor.config;

import jakarta.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;

/**
 * Adresa clientului pentru limitele per IP. Antetul X-Forwarded-For poate fi scris de oricine, deci nu-l credem orbeste:
 * luam intrarea aflata la `trustedProxyHops` pozitii de la DREAPTA (cea adaugata de proxy-urile noastre), niciodata
 * prima din stanga, pe care o controleaza clientul. Fara proxy de incredere (local) folosim adresa conexiunii.
 */
final class ClientIpResolver {

    // O adresa IPv4/IPv6 are doar aceste caractere; orice altceva ar fi o cheie arbitrara in cache.
    private static final Pattern IP_SHAPE = Pattern.compile("^[0-9a-fA-F:.]{2,45}$");

    private ClientIpResolver() {}

    static String resolve(HttpServletRequest request, int trustedProxyHops) {
        String fallback = request.getRemoteAddr();
        if (trustedProxyHops <= 0) {
            return fallback;
        }
        String header = request.getHeader("X-Forwarded-For");
        if (header == null || header.isBlank()) {
            return fallback;
        }
        String[] parts = header.split(",");
        int index = parts.length - trustedProxyHops;
        if (index < 0) {
            return fallback;
        }
        String candidate = parts[index].trim();
        return IP_SHAPE.matcher(candidate).matches() ? candidate : fallback;
    }
}
