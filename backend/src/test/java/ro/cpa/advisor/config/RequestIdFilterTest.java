package ro.cpa.advisor.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

    final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void generatesAnIdAndExposesItInLogContextAndResponse() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> idSeenInChain = new AtomicReference<>();
        FilterChain chain = (req, res) -> idSeenInChain.set(MDC.get(RequestIdFilter.MDC_KEY));

        filter.doFilter(new MockHttpServletRequest(), response, chain);

        assertThat(response.getHeader(RequestIdFilter.HEADER)).isNotBlank().isEqualTo(idSeenInChain.get());
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void keepsAHarmlessIncomingId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER, "client-id-1234");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo("client-id-1234");
    }

    @Test
    void replacesAnIncomingIdThatCouldForgeLogLines() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER, "abc\nERROR fake log line");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        assertThat(response.getHeader(RequestIdFilter.HEADER)).doesNotContain("\n").hasSize(36);
    }
}
