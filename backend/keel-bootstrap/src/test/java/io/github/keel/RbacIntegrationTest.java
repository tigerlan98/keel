package io.github.keel;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.keel.iam.infrastructure.persistence.mapper.IamResourceMapper;
import io.github.keel.iam.infrastructure.persistence.mapper.IamRoleMapper;
import io.github.keel.iam.infrastructure.persistence.mapper.IamUserMapper;
import io.github.keel.iam.infrastructure.persistence.mapper.IamUserRoleMapper;
import io.github.keel.iam.infrastructure.persistence.po.IamResourcePO;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 2 RBAC 集成测试：权限即时生效、保护规则、用户生命周期。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RbacIntegrationTest extends IntegrationTestSupport {

    private static final String PASSWORD = "Passw0rd1";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();

    @Autowired
    private Environment environment;

    @Autowired
    private IamUserMapper userMapper;

    @Autowired
    private IamRoleMapper roleMapper;

    @Autowired
    private IamResourceMapper resourceMapper;

    @Autowired
    private IamUserRoleMapper userRoleMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String baseUrl;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        baseUrl = "http://localhost:" + environment.getProperty("local.server.port");
        String adminUsername = unique("rbac-admin");
        createSuperAdmin(adminUsername);
        adminToken = login(adminUsername, PASSWORD);
    }

    @Test
    void roleAuthorizationTakesEffectOnNextRequest() throws Exception {
        long roleId = createRole(uniqueCode("ROLE"), "测试角色");
        String username = unique("rbac-user");
        createUser(username, List.of(roleId));

        String userToken = login(username, PASSWORD);
        assertThat(get("/api/v1/admin/iam/users", userToken).statusCode()).isEqualTo(403);

        long userListResourceId = resourceId("iam:user:list");
        HttpResponse<String> assign = put("/api/v1/admin/iam/roles/" + roleId + "/resources", adminToken,
                objectMapper.writeValueAsString(Map.of("resourceIds", List.of(userListResourceId))));
        assertThat(assign.statusCode()).isEqualTo(204);

        assertThat(get("/api/v1/admin/iam/users", userToken).statusCode()).isEqualTo(200);

        JsonNode me = read(get("/api/v1/admin/auth/me", userToken));
        assertThat(me.path("data").path("permissions").toString()).contains("iam:user:list");
        assertThat(me.path("data").path("menus").toString()).contains("/iam/users");
    }

    @Test
    void superAdminRoleIsProtected() throws Exception {
        IamRolePO superAdmin = roleMapper.selectOne(Wrappers.<IamRolePO>lambdaQuery()
                .eq(IamRolePO::getCode, "SUPER_ADMIN"));

        HttpResponse<String> disable = put("/api/v1/admin/iam/roles/" + superAdmin.getId(), adminToken,
                objectMapper.writeValueAsString(Map.of(
                        "name", superAdmin.getName(),
                        "description", "",
                        "status", "DISABLED",
                        "sort", superAdmin.getSort(),
                        "version", superAdmin.getVersion())));
        assertThat(disable.statusCode()).isEqualTo(409);
        assertThat(disable.body()).contains("IAM_ROLE_PROTECTED");

        HttpResponse<String> delete = delete("/api/v1/admin/iam/roles/" + superAdmin.getId(), adminToken);
        assertThat(delete.statusCode()).isEqualTo(409);
        assertThat(delete.body()).contains("IAM_ROLE_PROTECTED");
    }

    @Test
    void roleInUseCannotBeDeleted() throws Exception {
        long roleId = createRole(uniqueCode("USED_ROLE"), "使用中角色");
        String username = unique("rbac-role-user");
        long userId = createUser(username, List.of(roleId));

        HttpResponse<String> delete = delete("/api/v1/admin/iam/roles/" + roleId, adminToken);
        assertThat(delete.statusCode()).isEqualTo(409);
        assertThat(delete.body()).contains("IAM_ROLE_IN_USE");

        assertThat(put("/api/v1/admin/iam/users/" + userId + "/roles", adminToken,
                objectMapper.writeValueAsString(Map.of("roleIds", List.of()))).statusCode()).isEqualTo(204);

        assertThat(delete("/api/v1/admin/iam/roles/" + roleId, adminToken).statusCode()).isEqualTo(204);
    }

    @Test
    void resourceDisableAndParentRulesAreEnforced() throws Exception {
        IamResourcePO directory = resource("iam:directory");
        HttpResponse<String> disable = patch("/api/v1/admin/iam/resources/" + directory.getId() + "/status",
                adminToken,
                objectMapper.writeValueAsString(Map.of("status", "DISABLED", "version", directory.getVersion())));
        assertThat(disable.statusCode()).isEqualTo(409);
        assertThat(disable.body()).contains("IAM_RESOURCE_HAS_ENABLED_CHILDREN");

        IamResourcePO button = resource("iam:user:create");
        HttpResponse<String> moveToRoot = put("/api/v1/admin/iam/resources/" + button.getId(), adminToken,
                objectMapper.writeValueAsString(Map.of(
                        "name", button.getName(),
                        "parentId", 0,
                        "sort", button.getSort(),
                        "visible", false,
                        "version", button.getVersion())));
        assertThat(moveToRoot.statusCode()).isEqualTo(400);
        assertThat(moveToRoot.body()).contains("IAM_RESOURCE_PARENT_INVALID");
    }

    @Test
    void disablingUserRevokesTokenImmediately() throws Exception {
        String username = unique("rbac-disabled");
        long userId = createUser(username, List.of());
        String userToken = login(username, PASSWORD);
        assertThat(get("/api/v1/admin/auth/me", userToken).statusCode()).isEqualTo(200);

        JsonNode detail = read(get("/api/v1/admin/iam/users/" + userId, adminToken));
        int version = detail.path("data").path("version").asInt();
        HttpResponse<String> disable = patch("/api/v1/admin/iam/users/" + userId + "/status", adminToken,
                objectMapper.writeValueAsString(Map.of("status", "DISABLED", "version", version)));
        assertThat(disable.statusCode()).isEqualTo(200);

        assertThat(get("/api/v1/admin/auth/me", userToken).statusCode()).isEqualTo(401);
    }

    @Test
    void passwordChangeIsRequiredAndRevokesTokens() throws Exception {
        String username = unique("rbac-password");
        createUserRaw(username, List.of());
        String userToken = login(username, PASSWORD);

        HttpResponse<String> blocked = get("/api/v1/admin/iam/users", userToken);
        assertThat(blocked.statusCode()).isEqualTo(403);
        assertThat(blocked.body()).contains("AUTH_PASSWORD_CHANGE_REQUIRED");

        HttpResponse<String> wrongOld = post("/api/v1/admin/auth/change-password", userToken,
                objectMapper.writeValueAsString(Map.of(
                        "oldPassword", "WrongPass1",
                        "newPassword", "NewPassw0rd")));
        assertThat(wrongOld.statusCode()).isEqualTo(400);
        assertThat(wrongOld.body()).contains("AUTH_OLD_PASSWORD_INVALID");

        HttpResponse<String> changed = post("/api/v1/admin/auth/change-password", userToken,
                objectMapper.writeValueAsString(Map.of(
                        "oldPassword", PASSWORD,
                        "newPassword", "NewPassw0rd")));
        assertThat(changed.statusCode()).isEqualTo(204);
        assertThat(get("/api/v1/admin/auth/me", userToken).statusCode()).isEqualTo(401);

        String newToken = login(username, "NewPassw0rd");
        JsonNode me = read(get("/api/v1/admin/auth/me", newToken));
        assertThat(me.path("data").path("mustChangePassword").asBoolean()).isFalse();
    }

    @Test
    void forceLogoutInvalidatesTokens() throws Exception {
        String username = unique("rbac-logout");
        long userId = createUser(username, List.of());
        String userToken = login(username, PASSWORD);

        assertThat(delete("/api/v1/admin/iam/users/" + userId + "/sessions", adminToken).statusCode())
                .isEqualTo(204);
        assertThat(get("/api/v1/admin/auth/me", userToken).statusCode()).isEqualTo(401);

        String secondToken = login(username, PASSWORD);
        assertThat(delete("/api/v1/admin/iam/users/" + userId + "/sessions/APP", adminToken).statusCode())
                .isEqualTo(204);
        assertThat(get("/api/v1/admin/auth/me", secondToken).statusCode()).isEqualTo(401);
        assertThat(delete("/api/v1/admin/iam/users/" + userId + "/sessions/APP", adminToken).statusCode())
                .isEqualTo(204);
    }

    private void createSuperAdmin(String username) {
        IamUserPO user = new IamUserPO();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setNickname("超级管理员");
        user.setStatus("ENABLED");
        user.setMustChangePassword(false);
        userMapper.insert(user);

        IamRolePO role = roleMapper.selectOne(Wrappers.<IamRolePO>lambdaQuery()
                .eq(IamRolePO::getCode, "SUPER_ADMIN"));
        IamUserRolePO link = new IamUserRolePO();
        link.setUserId(user.getId());
        link.setRoleId(role.getId());
        userRoleMapper.insert(link);
    }

    private long createRole(String code, String name) throws Exception {
        HttpResponse<String> response = post("/api/v1/admin/iam/roles", adminToken,
                objectMapper.writeValueAsString(Map.of(
                        "code", code,
                        "name", name,
                        "status", "ENABLED",
                        "sort", 0)));
        assertThat(response.statusCode()).isEqualTo(201);
        return read(response).path("data").path("id").asLong();
    }

    private long createUser(String username, List<Long> roleIds) throws Exception {
        long userId = createUserRaw(username, roleIds);
        // 本测试聚焦 RBAC，跳过首次改密限制
        userMapper.update(null, Wrappers.<IamUserPO>lambdaUpdate()
                .eq(IamUserPO::getId, userId)
                .set(IamUserPO::getMustChangePassword, false));
        return userId;
    }

    private long createUserRaw(String username, List<Long> roleIds) throws Exception {
        HttpResponse<String> response = post("/api/v1/admin/iam/users", adminToken,
                objectMapper.writeValueAsString(Map.of(
                        "username", username,
                        "password", PASSWORD,
                        "nickname", username,
                        "roleIds", roleIds)));
        assertThat(response.statusCode()).isEqualTo(201);
        return read(response).path("data").path("id").asLong();
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

    private long resourceId(String code) {
        return resource(code).getId();
    }

    private IamResourcePO resource(String code) {
        IamResourcePO resource = resourceMapper.selectOne(Wrappers.<IamResourcePO>lambdaQuery()
                .eq(IamResourcePO::getCode, code));
        assertThat(resource).as(code).isNotNull();
        return resource;
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

    private HttpResponse<String> patch(String path, String token, String body) throws Exception {
        return send("PATCH", path, token, body);
    }

    private HttpResponse<String> delete(String path, String token) throws Exception {
        return send("DELETE", path, token, null);
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
            case "PATCH" -> builder.method("PATCH", HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
            case "DELETE" -> builder.DELETE();
            default -> throw new IllegalArgumentException("unsupported method: " + method);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String unique(String prefix) {
        return prefix + "-" + Long.toHexString(System.nanoTime());
    }

    private static String uniqueCode(String prefix) {
        return prefix + "_" + Long.toHexString(System.nanoTime()).toUpperCase();
    }
}
