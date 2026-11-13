package io.github.keel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.keel.audit.application.log.AuditCleanupJob;
import io.github.keel.iam.infrastructure.persistence.mapper.IamRoleMapper;
import io.github.keel.iam.infrastructure.persistence.mapper.IamUserMapper;
import io.github.keel.iam.infrastructure.persistence.mapper.IamUserRoleMapper;
import io.github.keel.iam.infrastructure.persistence.po.IamRolePO;
import io.github.keel.iam.infrastructure.persistence.po.IamUserPO;
import io.github.keel.iam.infrastructure.persistence.po.IamUserRolePO;
import io.github.keel.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 3 系统参数、操作日志、登录日志与保留期清理验证。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SystemAuditIntegrationTest extends IntegrationTestSupport {

    private static final String PASSWORD = "{{SECRET_q3phase3}}";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();

    @Autowired
    private Environment environment;

    @Autowired
    private IamUserMapper userMapper;

    @Autowired
    private IamRoleMapper roleMapper;

    @Autowired
    private IamUserRoleMapper userRoleMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AuditCleanupJob auditCleanupJob;

    private String baseUrl;
    private String adminToken;
    private String adminUsername;

    @BeforeEach
    void setUp() throws Exception {
        baseUrl = "http://localhost:" + environment.getProperty("local.server.port");
        adminUsername = unique("phase3-admin");
        createSuperAdmin(adminUsername);
        adminToken = login(adminUsername, PASSWORD);
    }

    @Test
    void configUpdateRefreshesPublicConfigAndWritesOperationLog() throws Exception {
        JsonNode page = read(get("/api/v1/admin/system/configs?keyword=system.name", adminToken));
        JsonNode summary = page.path("data").path("items").get(0);
        long id = summary.path("id").asLong();
        int version = summary.path("version").asInt();

        HttpResponse<String> updated = put("/api/v1/admin/system/configs/" + id, adminToken,
                objectMapper.writeValueAsString(Map.of(
                        "configValue", "Keel Phase3",
                        "description", "系统名称",
                        "publicFlag", true,
                        "version", version)));
        assertThat(updated.statusCode()).isEqualTo(200);
        assertThat(read(updated).path("data").path("configValue").asText()).isEqualTo("Keel Phase3");

        JsonNode publicConfig = read(client.send(HttpRequest.newBuilder(
                URI.create(baseUrl + "/api/v1/public/system/config")).GET().build(),
                HttpResponse.BodyHandlers.ofString()));
        assertThat(publicConfig.path("data").path("systemName").asText()).isEqualTo("Keel Phase3");

        JsonNode log = awaitOperationLog("system", "UPDATE");
        assertThat(log.path("username").asText()).isEqualTo(adminUsername);
        assertThat(log.path("requestUri").asText()).contains("/api/v1/admin/system/configs/");
        assertThat(log.path("success").asBoolean()).isTrue();
    }

    @Test
    void privateConfigCannotBeMarkedPublic() throws Exception {
        JsonNode page = read(get("/api/v1/admin/system/configs?keyword=auth.web.idle-timeout-minutes", adminToken));
        JsonNode summary = page.path("data").path("items").get(0);
        long id = summary.path("id").asLong();
        int version = summary.path("version").asInt();

        HttpResponse<String> response = put("/api/v1/admin/system/configs/" + id, adminToken,
                objectMapper.writeValueAsString(Map.of(
                        "configValue", "30",
                        "description", "Web 空闲超时（分钟）",
                        "publicFlag", true,
                        "version", version)));
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("SYS_CONFIG_PUBLIC_FORBIDDEN");
    }

    @Test
    void loginLogsRecordSuccessAndFailureReasons() throws Exception {
        JsonNode page = read(get("/api/v1/admin/audit/login-logs?username=" + adminUsername
                + "&action=LOGIN&success=true", adminToken));
        assertThat(page.path("data").path("total").asLong()).isGreaterThanOrEqualTo(1);

        HttpResponse<String> failed = post("/api/v1/admin/auth/login", null,
                objectMapper.writeValueAsString(Map.of(
                        "username", adminUsername,
                        "password", "WrongPassword1",
                        "device", "APP")));
        assertThat(failed.statusCode()).isEqualTo(401);

        JsonNode failedLogs = read(get("/api/v1/admin/audit/login-logs?username=" + adminUsername
                + "&action=LOGIN_FAILED", adminToken));
        JsonNode failedItem = failedLogs.path("data").path("items").get(0);
        assertThat(failedItem.path("failureReason").asText()).isEqualTo("BAD_CREDENTIALS");
        assertThat(failedItem.path("success").asBoolean()).isFalse();
    }

    @Test
    void operationLogMasksSensitiveRequestFields() throws Exception {
        String username = unique("phase3-user");
        String password = "SecretPass1";

        HttpResponse<String> created = post("/api/v1/admin/iam/users", adminToken,
                objectMapper.writeValueAsString(Map.of(
                        "username", username,
                        "password", password,
                        "nickname", username,
                        "roleIds", List.of())));
        assertThat(created.statusCode()).isEqualTo(201);

        JsonNode log = awaitOperationLog("iam", "CREATE");
        JsonNode detail = read(get("/api/v1/admin/audit/operation-logs/"
                + log.path("id").asLong(), adminToken));
        String requestParams = detail.path("data").path("requestParams").asText();
        assertThat(requestParams).contains("***").doesNotContain(password);
    }

    @Test
    void cleanupRemovesLogsOlderThanRetentionPeriod() {
        LocalDateTime now = LocalDateTime.now();
        insertLoginLog("cleanup-old-login", now.minusDays(400));
        insertLoginLog("cleanup-recent-login", now.minusDays(1));
        insertOperationLog("cleanup-old-op", now.minusDays(200));
        insertOperationLog("cleanup-recent-op", now.minusDays(1));

        auditCleanupJob.cleanup(now);

        assertThat(count("SELECT COUNT(*) FROM aud_login_log WHERE username = 'cleanup-old-login'")).isZero();
        assertThat(count("SELECT COUNT(*) FROM aud_operation_log WHERE trace_id = 'cleanup-old-op'")).isZero();
        assertThat(count("SELECT COUNT(*) FROM aud_login_log WHERE username = 'cleanup-recent-login'"))
                .isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM aud_operation_log WHERE trace_id = 'cleanup-recent-op'"))
                .isEqualTo(1);
    }

    private JsonNode awaitOperationLog(String module, String action) throws Exception {
        for (int attempt = 0; attempt < 50; attempt++) {
            JsonNode page = read(get("/api/v1/admin/audit/operation-logs?module=" + module
                    + "&action=" + action + "&username=" + adminUsername
                    + "&sort=createdAt,desc", adminToken));
            JsonNode items = page.path("data").path("items");
            if (items.size() > 0) {
                return items.get(0);
            }
            Thread.sleep(100);
        }
        throw new AssertionError("未在预期时间内写入操作日志");
    }

    private void insertLoginLog(String username, LocalDateTime createdAt) {
        jdbcTemplate.update("""
                INSERT INTO aud_login_log
                    (trace_id, user_id, username, action, login_type, device, ip, user_agent,
                     success, failure_reason, created_at)
                VALUES (?, NULL, ?, 'LOGIN', 'ADMIN', 'APP', '127.0.0.1', 'test', 1, NULL, ?)
                """, "cleanup-" + username, username, Timestamp.valueOf(createdAt));
    }

    private void insertOperationLog(String traceId, LocalDateTime createdAt) {
        jdbcTemplate.update("""
                INSERT INTO aud_operation_log
                    (trace_id, user_id, username, module, action, method, request_method, request_uri,
                     request_params, success, error_message, ip, user_agent, duration_ms, created_at)
                VALUES (?, NULL, 'cleanup', 'system', 'UPDATE', 'test#cleanup', 'PUT', '/test',
                        NULL, 1, NULL, '127.0.0.1', 'test', 1, ?)
                """, traceId, Timestamp.valueOf(createdAt));
    }

    private long count(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }

    private void createSuperAdmin(String username) {
        IamUserPO user = new IamUserPO();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setNickname("超级管理员");
        user.setStatus("ENABLED");
        user.setMustChangePassword(false);
        userMapper.insert(user);

        IamRolePO role = roleMapper.selectOne(com.baomidou.mybatisplus.core.toolkit.Wrappers
                .<IamRolePO>lambdaQuery().eq(IamRolePO::getCode, "SUPER_ADMIN"));
        IamUserRolePO link = new IamUserRolePO();
        link.setUserId(user.getId());
        link.setRoleId(role.getId());
        userRoleMapper.insert(link);
    }

    private String login(String username, String password) throws Exception {
        HttpResponse<String> response = post("/api/v1/admin/auth/login", null,
                objectMapper.writeValueAsString(Map.of(
                        "username", username,
                        "password", password,
                        "device", "APP")));
        assertThat(response.statusCode()).isEqualTo(200);
        return read(response).path("data").path("token").asText();
    }

    private JsonNode read(HttpResponse<String> response) throws Exception {
        return objectMapper.readTree(response.body());
    }

    private HttpResponse<String> get(String path, String token) throws Exception {
        return send("GET", path, token, null);
    }

    private HttpResponse<String> post(String path, String token, String body) throws Exception {
        return send("POST", path, token, body);
    }

    private HttpResponse<String> put(String path, String token, String body) throws Exception {
        return send("PUT", path, token, body);
    }

    private HttpResponse<String> send(String method, String path, String token, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path));
        if (token != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        if (body != null) {
            builder.header(HttpHeaders.CONTENT_TYPE, "application/json");
        }
        switch (method) {
            case "GET" -> builder.GET();
            case "POST" -> builder.POST(HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
            case "PUT" -> builder.PUT(HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
            default -> throw new IllegalArgumentException("unsupported method: " + method);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String unique(String prefix) {
        return prefix + "-" + Long.toHexString(System.nanoTime());
    }
}