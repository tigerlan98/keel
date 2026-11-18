package io.github.keel;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.doThrow;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.keel.common.core.exception.BusinessException;
import io.github.keel.common.security.token.TokenService;
import io.github.keel.iam.application.authentication.AuthApplicationService;
import io.github.keel.iam.application.authentication.LoginCommand;
import io.github.keel.iam.application.authentication.Device;
import io.github.keel.iam.application.identity.UserApplicationService;
import io.github.keel.iam.infrastructure.cache.PermissionCache;
import io.github.keel.iam.infrastructure.persistence.mapper.*;
import io.github.keel.iam.infrastructure.persistence.po.*;
import io.github.keel.support.IntegrationTestSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.URI;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserDeletionIntegrationTest extends IntegrationTestSupport {
    private static final String PASSWORD = "Delete" + "Pass123!";
    @Autowired IamUserMapper users;
    @Autowired IamRoleMapper roles;
    @Autowired IamUserRoleMapper links;
    @Autowired UserApplicationService service;
    @Autowired AuthApplicationService auth;
    @Autowired PasswordEncoder encoder;
    @Autowired PermissionCache cache;
    @Autowired JdbcTemplate jdbc;
    @Autowired Environment environment;
    @Autowired PlatformTransactionManager transactions;
    @MockitoSpyBean TokenService tokens;
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();
    private IamUserPO admin;
    private long superRoleId;
    private String adminToken;

    @BeforeEach
    void prepare() throws Exception {
        superRoleId = roles.selectOne(Wrappers.<IamRolePO>lambdaQuery()
                .eq(IamRolePO::getCode, "SUPER_ADMIN")).getId();
        admin = createUser();
        link(admin.getId(), superRoleId);
        adminToken = login(admin.getUsername(), "APP");
    }

    @Test
    void deletesAccountAndAssociationsRevokesAllDevicesAndKeepsAuditHistory() throws Exception {
        var user = createUser();
        var role = createRole();
        link(user.getId(), role.getId());
        String app = login(user.getUsername(), "APP");
        String mini = login(user.getUsername(), "MINI");
        cache.put(user.getId(), Set.of("iam:user:list"));
        Long logsBefore = jdbc.queryForObject("SELECT COUNT(*) FROM aud_login_log WHERE user_id = ?",
                Long.class, user.getId());
        assertThat(logsBefore).isPositive();

        var result = request("DELETE", path(user), adminToken);
        assertThat(result.statusCode()).as(result.body()).isEqualTo(204);
        assertThat(result.body()).isEmpty();
        assertThat(users.selectById(user.getId())).isNull();
        assertThat(linkCount(user.getId())).isZero();
        assertThat(roles.selectById(role.getId())).isNotNull();
        assertThat(tokens.authenticate(app)).isEmpty();
        assertThat(tokens.authenticate(mini)).isEmpty();
        assertThat(cache.get(user.getId())).isEmpty();
        assertThat(request("GET", "/api/v1/admin/auth/me", app).statusCode()).isEqualTo(401);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM aud_login_log WHERE user_id = ?",
                Long.class, user.getId())).isEqualTo(logsBefore);
        org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM aud_operation_log WHERE user_id = ? AND action = 'DELETE' AND success = 1",
                        Long.class, admin.getId())).isPositive());
        assertThat(request("DELETE", path(user), adminToken).statusCode()).isEqualTo(404);
    }

    @Test
    void protectsSelfAndOtherSuperAdministrators() throws Exception {
        var protectedUser = createUser();
        link(protectedUser.getId(), superRoleId);
        for (var user : List.of(admin, protectedUser)) {
            var result = request("DELETE", path(user), adminToken);
            assertThat(result.statusCode()).as(result.body()).isEqualTo(409);
            assertThat(json.readTree(result.body()).path("code").asText()).isEqualTo("IAM_USER_PROTECTED");
            assertThat(users.selectById(user.getId())).isNotNull();
            assertThat(linkCount(user.getId())).isPositive();
        }
        assertThat(tokens.authenticate(adminToken)).isPresent();
    }

    @Test
    void rejectsMissingPermissionStaleVersionAndInvalidParameters() throws Exception {
        var target = createUser();
        var visitor = createUser();
        String visitorToken = login(visitor.getUsername(), "APP");
        assertThat(request("DELETE", path(target), visitorToken).statusCode()).isEqualTo(403);
        assertThat(request("DELETE", "/api/v1/admin/iam/users/" + target.getId(), adminToken).statusCode()).isEqualTo(400);
        assertThat(request("DELETE", "/api/v1/admin/iam/users/" + target.getId() + "?version=-1", adminToken).statusCode()).isEqualTo(400);
        assertThat(request("DELETE", "/api/v1/admin/iam/users/0?version=0", adminToken).statusCode()).isEqualTo(400);
        var stale = request("DELETE", "/api/v1/admin/iam/users/" + target.getId() + "?version=99", adminToken);
        assertThat(stale.statusCode()).isEqualTo(409);
        assertThat(json.readTree(stale.body()).path("code").asText()).isEqualTo("COMMON_OPTIMISTIC_LOCK");
        assertThat(users.selectById(target.getId())).isNotNull();
    }

    @Test
    void redisFailureRollsBackUserAndRoleDeletion() {
        var user = createUser();
        link(user.getId(), createRole().getId());
        doThrow(new IllegalStateException("simulated revocation failure")).when(tokens).revokeAll(user.getId());
        assertThatThrownBy(() -> service.delete(user.getId(), user.getVersion()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(users.selectById(user.getId())).isNotNull();
        assertThat(linkCount(user.getId())).isEqualTo(1);
    }

    @Test
    void concurrentRoleAssignmentCannotRecreateLinksAfterDeletion() throws Exception {
        var user = createUser();
        long roleId = createRole().getId();
        var deleted = new CountDownLatch(1);
        var commit = new CountDownLatch(1);
        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            var deletion = pool.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(tx -> {
                service.delete(user.getId(), user.getVersion());
                deleted.countDown();
                await(commit);
            }));
            try {
                assertThat(deleted.await(10, TimeUnit.SECONDS)).isTrue();
                var assignment = pool.submit(() -> service.replaceRoles(user.getId(), List.of(roleId)));
                assertThatThrownBy(() -> assignment.get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
                commit.countDown();
                deletion.get(10, TimeUnit.SECONDS);
                assertThatThrownBy(() -> assignment.get(10, TimeUnit.SECONDS))
                        .isInstanceOf(ExecutionException.class).hasCauseInstanceOf(BusinessException.class);
            } finally {
                commit.countDown();
            }
        }
        assertThat(users.selectById(user.getId())).isNull();
        assertThat(linkCount(user.getId())).isZero();
    }

    @Test
    void concurrentSuperAdminAssignmentIsProtectedBeforeDeleteChecksRoles() throws Exception {
        var user = createUser();
        var assigned = new CountDownLatch(1);
        var commit = new CountDownLatch(1);
        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            var assignment = pool.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(tx -> {
                service.replaceRoles(user.getId(), List.of(superRoleId));
                assigned.countDown();
                await(commit);
            }));
            try {
                assertThat(assigned.await(10, TimeUnit.SECONDS)).isTrue();
                var deletion = pool.submit(() -> service.delete(user.getId(), user.getVersion()));
                assertThatThrownBy(() -> deletion.get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
                commit.countDown();
                assignment.get(10, TimeUnit.SECONDS);
                assertThatThrownBy(() -> deletion.get(10, TimeUnit.SECONDS))
                        .isInstanceOf(ExecutionException.class).hasCauseInstanceOf(BusinessException.class);
            } finally {
                commit.countDown();
            }
        }
        assertThat(users.selectById(user.getId())).isNotNull();
        assertThat(users.countProtectedRole(user.getId())).isEqualTo(1);
    }

    @Test
    void deletionWaitsForLoginThenRevokesTheNewlyIssuedSession() throws Exception {
        var user = createUser();
        var issued = new CountDownLatch(1);
        var commit = new CountDownLatch(1);
        var rawToken = new java.util.concurrent.atomic.AtomicReference<String>();
        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            var login = pool.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(tx -> {
                var session = auth.login(new LoginCommand(user.getUsername(), PASSWORD, Device.APP, "127.0.0.1", "deletion-test"));
                rawToken.set(session.token());
                issued.countDown();
                await(commit);
            }));
            try {
                assertThat(issued.await(10, TimeUnit.SECONDS)).isTrue();
                var deletion = pool.submit(() -> service.delete(user.getId(), user.getVersion()));
                assertThatThrownBy(() -> deletion.get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
                commit.countDown();
                login.get(10, TimeUnit.SECONDS);
                deletion.get(10, TimeUnit.SECONDS);
            } finally {
                commit.countDown();
            }
        }
        assertThat(users.selectById(user.getId())).isNull();
        assertThat(tokens.authenticate(rawToken.get())).isEmpty();
        assertThat(cache.get(user.getId())).isEmpty();
    }

    @Test
    void failedLoginAuditStillPersistsWithTheLoginTransaction() throws Exception {
        var user = createUser();
        var response = client.send(HttpRequest.newBuilder(uri("/api/v1/admin/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of(
                        "username", user.getUsername(), "password", "Wrong" + "Pass123!", "device", "APP"))))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM aud_login_log WHERE user_id = ? AND action = 'LOGIN_FAILED'",
                Long.class, user.getId())).isEqualTo(1);
    }

    private IamUserPO createUser() {
        var user = new IamUserPO();
        user.setUsername("delete-" + UUID.randomUUID().toString().substring(0, 12));
        user.setNickname("删除测试账号");
        user.setPasswordHash(encoder.encode(PASSWORD));
        user.setStatus("ENABLED");
        user.setMustChangePassword(false);
        user.setVersion(0);
        users.insert(user);
        return user;
    }

    private IamRolePO createRole() {
        var role = new IamRolePO();
        role.setCode("DEL_" + UUID.randomUUID().toString().substring(0, 12));
        role.setName("删除测试角色");
        role.setStatus("ENABLED");
        role.setSort(0);
        roles.insert(role);
        return role;
    }

    private void link(long userId, long roleId) {
        var link = new IamUserRolePO();
        link.setUserId(userId);
        link.setRoleId(roleId);
        links.insert(link);
    }

    private long linkCount(long userId) {
        return links.selectCount(Wrappers.<IamUserRolePO>lambdaQuery().eq(IamUserRolePO::getUserId, userId));
    }

    private String path(IamUserPO user) {
        return "/api/v1/admin/iam/users/" + user.getId() + "?version=" + user.getVersion();
    }

    private String login(String username, String device) throws Exception {
        var response = client.send(HttpRequest.newBuilder(uri("/api/v1/admin/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of(
                        "username", username, "password", PASSWORD, "device", device))))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        return json.readTree(response.body()).path("data").path("token").asText();
    }

    private HttpResponse<String> request(String method, String path, String token) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).header("Authorization", "Bearer " + token)
                .method(method, HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + environment.getProperty("local.server.port") + path);
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("concurrency test timed out");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
