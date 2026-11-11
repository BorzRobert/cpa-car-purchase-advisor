package ro.cpa.advisor.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/** Un X-Forwarded-For falsificat de client nu trebuie sa-i dea o galeata de rata proprie la fiecare cerere. */
class ClientIpResolverTest {

    @Test
    void usesTheConnectionAddressWithoutTrustedProxies() {
        MockHttpServletRequest request = request("203.0.113.9", "1.2.3.4");
        assertThat(ClientIpResolver.resolve(request, 0)).isEqualTo("203.0.113.9");
    }

    @Test
    void takesTheEntryAddedByTheTrustedProxyFromTheRight() {
        // „6.6.6.6” e scris de client si ignorat; ultima intrare e cea adaugata de proxy-ul nostru.
        MockHttpServletRequest request = request("10.0.0.1", "6.6.6.6, 198.51.100.7");
        assertThat(ClientIpResolver.resolve(request, 1)).isEqualTo("198.51.100.7");
        assertThat(ClientIpResolver.resolve(request, 2)).isEqualTo("6.6.6.6");
    }

    @Test
    void fallsBackToTheConnectionAddressWhenTheHeaderIsMissingShortOrGarbage() {
        assertThat(ClientIpResolver.resolve(request("10.0.0.1", null), 1)).isEqualTo("10.0.0.1");
        assertThat(ClientIpResolver.resolve(request("10.0.0.1", "198.51.100.7"), 2)).isEqualTo("10.0.0.1");
        assertThat(ClientIpResolver.resolve(request("10.0.0.1", "<script>"), 1)).isEqualTo("10.0.0.1");
    }

    private static MockHttpServletRequest request(String remoteAddr, String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        return request;
    }
}
