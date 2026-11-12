package io.github.keel;

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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 SpringDoc 分组文档与 Actuator 暴露范围。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class HttpEndpointIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private Environment environment;

    @Test
    void openApiAndHealthEndpointsAreExposed() throws Exception {
        String baseUrl = "http://localhost:" + environment.getProperty("local.server.port");
        HttpClient client = HttpClient.newHttpClient();

        assertOk(client, baseUrl + "/v3/api-docs", "\"openapi\"");
        assertOk(client, baseUrl + "/v3/api-docs/admin", "\"openapi\"");
        assertOk(client, baseUrl + "/v3/api-docs/public", "\"openapi\"");
        assertOk(client, baseUrl + "/actuator/health", "\"status\":\"UP\"");
        // 未暴露的 Actuator 端点先被安全层拦截
        assertStatus(client, baseUrl + "/actuator/env", 401);
    }

    private void assertOk(HttpClient client, String url, String expectedBodyPart) throws Exception {
        HttpResponse<String> response = get(client, url);
        assertThat(response.statusCode()).as(url).isEqualTo(200);
        assertThat(response.body()).as(url).contains(expectedBodyPart);
    }

    private void assertStatus(HttpClient client, String url, int expectedStatus) throws Exception {
        HttpResponse<String> response = get(client, url);
        assertThat(response.statusCode()).as(url).isEqualTo(expectedStatus);
    }

    private HttpResponse<String> get(HttpClient client, String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
