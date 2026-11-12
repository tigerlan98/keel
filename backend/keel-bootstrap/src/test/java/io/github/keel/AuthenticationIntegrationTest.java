package io.github.keel;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 1A 认证链路验证：Web Cookie + CSRF、App Bearer、同设备单会话。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthenticationIntegrationTest extends IntegrationTestSupport {

    private static final String USERNAME = "auth-super-admin";
    private static final String PASSWORD = "Passw0rd!";
    private static final String LOGIN_PATH = "/api/v1/admin/auth/login";
    private static final String LOGOUT_PATH = "/api/v1/admin/auth/logout";
    private static final String ME_PATH = "/api/v1/admin/auth/me";
    private static final String CSRF_PATH = "/api/v1/admin/auth/csrf";
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"");

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

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + environment.getProperty("local.server.port");
        createSuperAdminUser();
    }

    @Test
    void webLoginUsesCsrfAndCookieSession() throws Exception {
        CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient client = HttpClient.newBuilder().cookieHandler(cookieManager).build();

        HttpResponse<String> csrfResponse = get(client, CSRF_PATH, null);
        assertThat(csrfResponse.statusCode()).isEqualTo(200);
        String csrfCookie = cookieValue(cookieManager, "XSRF-TOKEN");
        String csrfBodyToken = extractToken(csrfResponse.body());
        assertThat(csrfCookie).isNotBlank();
        assertThat(csrfBodyToken).as("csrf body token").isEqualTo(csrfCookie);

        HttpResponse<String> loginResponse = postJson(client, LOGIN_PATH, loginBody("WEB"), csrfBodyToken);
        assertThat(loginResponse.statusCode()).isEqualTo(200);
        assertThat(loginResponse.body())
                .contains("\"superAdmin\":true", "\"mustChangePassword\":false", "iam:user:list")
                .doesNotContain("\"token\":");
        assertThat(cookieValue(cookieManager, "KEEL_TOKEN")).isNotBlank();

        HttpResponse<String> meResponse = get(client, ME_PATH, null);
        assertThat(meResponse.statusCode()).isEqualTo(200);
        assertThat(meResponse.body()).contains("\"username\":\"auth-super-admin\"");

        HttpResponse<String> csrfAfterLogin = get(client, CSRF_PATH, null);
        String logoutCsrfToken = extractToken(csrfAfterLogin.body());
        HttpResponse<String> logoutResponse = postJson(client, LOGOUT_PATH, "", logoutCsrfToken);
        assertThat(logoutResponse.statusCode()).as("logout body=%s", logoutResponse.body()).isEqualTo(204);

        assertThat(get(client, ME_PATH, null).statusCode()).isEqualTo(401);
        assertThat(loginLogCount("LOGOUT", null)).isGreaterThanOrEqualTo(1);
    }

    @Test
    void appLoginKeepsSingleSessionPerDevice() throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        HttpResponse<String> firstLogin = postJson(client, LOGIN_PATH, loginBody("APP"), null);
        assertThat(firstLogin.statusCode()).isEqualTo(200);
        String firstToken = extractToken(firstLogin.body());
        assertThat(firstToken).isNotBlank();
        assertThat(get(client, ME_PATH, firstToken).statusCode()).isEqualTo(200);

        HttpResponse<String> secondLogin = postJson(client, LOGIN_PATH, loginBody("APP"), null);
        assertThat(secondLogin.statusCode()).isEqualTo(200);
        String secondToken = extractToken(secondLogin.body());
        assertThat(secondToken).isNotBlank().isNotEqualTo(firstToken);

        assertThat(get(client, ME_PATH, firstToken).statusCode()).isEqualTo(401);
        assertThat(get(client, ME_PATH, secondToken).statusCode()).isEqualTo(200);
        assertThat(loginLogCount("LOGIN", null)).isGreaterThanOrEqualTo(2);
    }

    @Test
    void webLoginWithoutCsrfIsRejected() throws Exception {
        HttpResponse<String> response = postJson(HttpClient.newHttpClient(), LOGIN_PATH, loginBody("WEB"), null);
        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("AUTH_CSRF_INVALID");
    }

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        HttpResponse<String> response = get(HttpClient.newHttpClient(), ME_PATH, null);
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("COMMON_UNAUTHORIZED");
    }

    @Test
    void badCredentialsReturn401() throws Exception {
        String body = "{\"username\":\"" + USERNAME + "\",\"password\":\"wrong\",\"device\":\"APP\"}";
        HttpResponse<String> response = postJson(HttpClient.newHttpClient(), LOGIN_PATH, body, null);
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("AUTH_BAD_CREDENTIALS");
        assertThat(loginLogCount("LOGIN_FAILED", "BAD_CREDENTIALS")).isGreaterThanOrEqualTo(1);
    }

    private void createSuperAdminUser() {
        IamUserPO existing = userMapper.selectOne(Wrappers.<IamUserPO>lambdaQuery()
                .eq(IamUserPO::getUsername, USERNAME));
        if (existing != null) {
            return;
        }

        IamRolePO superAdminRole = roleMapper.selectOne(Wrappers.<IamRolePO>lambdaQuery()
                .eq(IamRolePO::getCode, "SUPER_ADMIN"));
        assertThat(superAdminRole).isNotNull();

        IamUserPO user = new IamUserPO();
        user.setUsername(USERNAME);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setNickname("认证测试管理员");
        user.setStatus("ENABLED");
        user.setMustChangePassword(false);
        userMapper.insert(user);

        IamUserRolePO link = new IamUserRolePO();
        link.setUserId(user.getId());
        link.setRoleId(superAdminRole.getId());
        userRoleMapper.insert(link);
    }

    private String loginBody(String device) {
        return "{\"username\":\"" + USERNAME + "\",\"password\":\"" + PASSWORD
                + "\",\"device\":\"" + device + "\"}";
    }

    private HttpResponse<String> get(HttpClient client, String path, String bearerToken) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path)).GET();
        if (bearerToken != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postJson(HttpClient client, String path, String body,
                                          String csrfToken) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (csrfToken != null) {
            builder.header("X-XSRF-TOKEN", csrfToken);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String cookieValue(CookieManager cookieManager, String name) {
        return cookieManager.getCookieStore().getCookies().stream()
                .filter(cookie -> cookie.getName().equals(name))
                .map(HttpCookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private String extractToken(String body) {
        Matcher matcher = TOKEN_PATTERN.matcher(body);
        return matcher.find() ? matcher.group(1) : null;
    }

    private int loginLogCount(String action, String failureReason) {
        if (failureReason == null) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM aud_login_log WHERE username = ? AND action = ?",
                    Integer.class, USERNAME, action);
            return count == null ? 0 : count;
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM aud_login_log WHERE username = ? AND action = ? AND failure_reason = ?",
                Integer.class, USERNAME, action, failureReason);
        return count == null ? 0 : count;
    }
}
