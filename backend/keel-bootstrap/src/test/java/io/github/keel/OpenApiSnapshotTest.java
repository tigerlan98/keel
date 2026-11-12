package io.github.keel;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.keel.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 导出 OpenAPI 分组快照，作为前端 TypeScript 类型生成的唯一来源。
 *
 * <p>输出目录通过 {@code -Dopenapi.outputDir} 指定，默认 {@code target/openapi}；
 * 由 {@code scripts/openapi/export-openapi.ps1} 同步到各前端工程。</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OpenApiSnapshotTest extends IntegrationTestSupport {

    private static final String[] GROUPS = {"admin", "public"};

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private Environment environment;

    @Test
    void exportGroupedSnapshots() throws Exception {
        Path outputDir = Path.of(System.getProperty("openapi.outputDir", "target/openapi"));
        Files.createDirectories(outputDir);

        String baseUrl = "http://localhost:" + environment.getProperty("local.server.port");
        HttpClient client = HttpClient.newHttpClient();

        for (String group : GROUPS) {
            HttpResponse<String> response = get(client, baseUrl + "/v3/api-docs/" + group);
            assertThat(response.statusCode()).as("group %s", group).isEqualTo(200);
            assertThat(response.body()).as("group %s", group).contains("\"openapi\"");

            String snapshot = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(objectMapper.readTree(response.body()))
                    + System.lineSeparator();
            Files.writeString(outputDir.resolve(group + ".json"), snapshot);
        }
    }

    private HttpResponse<String> get(HttpClient client, String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
