package ro.cpa.advisor.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/**
 * Verificarea „snapshot-ul OpenAPI nu e invechit” din CI: documentul generat de aplicatie trebuie sa fie identic cu
 * `docs/architecture/openapi.json`, care este contractul din care frontend-ul isi genereaza tipurile (Faza 6).
 * Cine schimba un endpoint sau un DTO regenereaza fisierul si il comite impreuna cu codul:
 * <pre>./mvnw test -Dtest=OpenApiSnapshotTest -Dopenapi.update=true</pre>
 */
@SpringBootTest(properties = "spring.ai.openai.api-key=")
@AutoConfigureMockMvc
class OpenApiSnapshotTest {

    private static final Path SNAPSHOT = Path.of("..", "docs", "architecture", "openapi.json");

    @Autowired
    MockMvc mvc;

    @Test
    void snapshotMatchesTheGeneratedDocument() throws Exception {
        String generated = normalized(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

        if (Boolean.getBoolean("openapi.update")) {
            Files.createDirectories(SNAPSHOT.getParent());
            Files.writeString(SNAPSHOT, generated, StandardCharsets.UTF_8);
            return;
        }

        assertThat(SNAPSHOT).as("OpenAPI snapshot is missing; run with -Dopenapi.update=true").exists();
        String committed = Files.readString(SNAPSHOT, StandardCharsets.UTF_8).replace("\r\n", "\n");
        assertThat(generated)
                .as("docs/architecture/openapi.json is stale. Regenerate: ./mvnw test -Dtest=OpenApiSnapshotTest -Dopenapi.update=true")
                .isEqualTo(committed);
    }

    // Chei sortate, fara `servers` (adresa difera de la o masina la alta) si cu LF: aceeasi iesire peste tot.
    private static String normalized(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Object tree = sorted(mapper.readValue(json, Object.class));
        if (tree instanceof Map<?, ?> map) {
            map.remove("servers");
        }
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(tree).replace("\r\n", "\n") + "\n";
    }

    private static Object sorted(Object node) {
        if (node instanceof Map<?, ?> map) {
            Map<String, Object> out = new TreeMap<>();
            map.forEach((k, v) -> out.put(String.valueOf(k), sorted(v)));
            return out;
        }
        if (node instanceof List<?> list) {
            return list.stream().map(OpenApiSnapshotTest::sorted).toList();
        }
        return node;
    }
}
