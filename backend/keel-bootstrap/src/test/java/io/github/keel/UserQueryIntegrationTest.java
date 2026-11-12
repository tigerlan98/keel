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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 用户分页查询集成测试。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserQueryIntegrationTest extends IntegrationTestSupport {

    private static final String ADMIN_USERNAME = "user-query-admin";
    private static final String ADMIN_PASSWORD = "TestPassw0rd!";
    private static final String USERNAME_PREFIX = "userpage-";
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

    private String baseUrl;

    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        baseUrl = "http://localhost:" + environment.getProperty("local.server.port");
        createUser(ADMIN_USERNAME, "ENABLED", "用户查询管理员", true);
        createUser(USERNAME_PREFIX + "alpha", "ENABLED", "Alpha", false);
        createUser(USERNAME_PREFIX + "beta", "DISABLED", "Beta", false);
        createUser(USERNAME_PREFIX + "gamma", "ENABLED", "Gamma", false);
        accessToken = login();
    }

    @Test
    void paginationAndPrefixFilterWork() throws Exception {
        HttpResponse<String> firstPage = get(
                "/api/v1/admin/iam/users?page=1&size=2&sort=username,asc&username=" + USERNAME_PREFIX,
                accessToken);

        assertThat(firstPage.statusCode()).isEqualTo(200);
        assertThat(firstPage.body())
                .contains("\"total\":3", "\"page\":1", "\"size\":2")
                .contains("userpage-alpha", "userpage-beta")
                .doesNotContain("userpage-gamma");

        HttpResponse<String> secondPage = get(
                "/api/v1/admin/iam/users?page=2&size=2&sort=username,asc&username=" + USERNAME_PREFIX,
                accessToken);

        assertThat(secondPage.statusCode()).isEqualTo(200);
        assertThat(secondPage.body()).contains("userpage-gamma");
    }

    @Test
    void statusFilterWorks() throws Exception {
        HttpResponse<String> response = get(
                "/api/v1/admin/iam/users?username=" + USERNAME_PREFIX + "&status=DISABLED",
                accessToken);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("\"total\":1", "userpage-beta")
                .doesNotContain("userpage-alpha", "userpage-gamma");
    }

    @Test
    void invalidSortFieldIsRejected() throws Exception {
        HttpResponse<String> response = get(
                "/api/v1/admin/iam/users?sort=passwordHash,asc",
                accessToken);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("COMMON_BAD_REQUEST");
    }

    @Test
    void pageSizeAboveLimitIsRejected() throws Exception {
        HttpResponse<String> response = get(
                "/api/v1/admin/iam/users?size=101",
                accessToken);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("COMMON_VALIDATION_FAILED");
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        HttpResponse<String> response = get("/api/v1/admin/iam/users", null);

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("COMMON_UNAUTHORIZED");
    }

    private void createUser(String username, String status, String nickname, boolean superAdmin) {
        IamUserPO existing = userMapper.selectOne(Wrappers.<IamUserPO>lambdaQuery()
                .eq(IamUserPO::getUsername, username));
        IamUserPO user = existing;
        if (user == null) {
            user = new IamUserPO();
            user.setUsername(username);
            user.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
            user.setNickname(nickname);
            user.setStatus(status);
            user.setMustChangePassword(false);
            userMapper.insert(user);
        }

        if (!superAdmin) {
            return;
        }

        Long links = userRoleMapper.selectCount(Wrappers.<IamUserRolePO>lambdaQuery()
                .eq(IamUserRolePO::getUserId, user.getId()));
        if (links != null && links > 0) {
            return;
        }

        IamRolePO role = roleMapper.selectOne(Wrappers.<IamRolePO>lambdaQuery()
                .eq(IamRolePO::getCode, "SUPER_ADMIN"));
        assertThat(role).isNotNull();

        IamUserRolePO link = new IamUserRolePO();
        link.setUserId(user.getId());
        link.setRoleId(role.getId());
        userRoleMapper.insert(link);
    }

    private String login() throws Exception {
        String body = "{\"username\":\"" + ADMIN_USERNAME + "\",\"password\":\"" + ADMIN_PASSWORD
                + "\",\"device\":\"APP\"}";
        HttpResponse<String> response = postJson("/api/v1/admin/auth/login", body);
        assertThat(response.statusCode()).isEqualTo(200);

        Matcher matcher = TOKEN_PATTERN.matcher(response.body());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private HttpResponse<String> get(String path, String bearerToken) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path)).GET();
        if (bearerToken != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
        }
        return HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postJson(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }
}
