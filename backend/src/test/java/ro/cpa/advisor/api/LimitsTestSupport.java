package ro.cpa.advisor.api;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Ajutoare comune pentru testele de limite: fiecare test isi alege o adresa IP (`remoteAddr`), ca galetile de rata
 * per IP sa nu se amestece intre teste si sa putem atinge o limita fara sa o atinga si celelalte.
 */
abstract class LimitsTestSupport {

    @Autowired
    protected MockMvc mvc;

    protected ResultActions createSession(String ip) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post("/api/v1/sessions").with(request -> {
            request.setRemoteAddr(ip);
            return request;
        }));
    }

    protected String sessionToken(String ip) throws Exception {
        return JsonPath.read(createSession(ip).andReturn().getResponse().getContentAsString(), "$.token");
    }

    protected ResultActions get(String token, String ip, String path) throws Exception {
        return send(token, ip, MockMvcRequestBuilders.get(path));
    }

    protected ResultActions post(String token, String ip, String path, String json) throws Exception {
        return send(token, ip, MockMvcRequestBuilders.post(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected String createConversation(String token, String ip) throws Exception {
        String body = send(token, ip, MockMvcRequestBuilders.post("/api/v1/conversations")).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private ResultActions send(String token, String ip, MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token).with(r -> {
            r.setRemoteAddr(ip);
            return r;
        }));
    }
}
