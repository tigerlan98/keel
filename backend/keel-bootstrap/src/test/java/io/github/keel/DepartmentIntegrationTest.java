package io.github.keel;

import static org.assertj.core.api.Assertions.*;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.*;

import io.github.keel.iam.application.identity.*;
import io.github.keel.iam.application.organization.*;
import io.github.keel.iam.infrastructure.persistence.mapper.*;
import io.github.keel.iam.infrastructure.persistence.po.*;
import io.github.keel.support.IntegrationTestSupport;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DepartmentIntegrationTest extends IntegrationTestSupport {
    @Autowired DepartmentApplicationService departments;
    @Autowired UserApplicationService users;
    @Autowired IamUserMapper userMapper;
    @Autowired IamRoleMapper roleMapper;
    @Autowired IamUserRoleMapper userRoles;
    @Autowired IamDepartmentMapper departmentMapper;
    @Autowired PasswordEncoder encoder;
    @Autowired Environment env;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
    final ObjectMapper json = new ObjectMapper();
    final HttpClient client = HttpClient.newHttpClient();
    static final String PASSWORD = "Test" + "Pass123!";
    String token;
    long root;

    String unique() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    @BeforeEach
    void prepare() throws Exception {
        root =
                departmentMapper
                        .selectOne(
                                Wrappers.<IamDepartmentPO>lambdaQuery()
                                        .eq(IamDepartmentPO::getCode, "ROOT"))
                        .getId();
        var user = new IamUserPO();
        user.setUsername("dept-" + unique());
        user.setNickname("部门管理员");
        user.setPasswordHash(encoder.encode(PASSWORD));
        user.setStatus("ENABLED");
        user.setMustChangePassword(false);
        userMapper.insert(user);
        var role =
                roleMapper.selectOne(
                        Wrappers.<IamRolePO>lambdaQuery().eq(IamRolePO::getCode, "SUPER_ADMIN"));
        var link = new IamUserRolePO();
        link.setUserId(user.getId());
        link.setRoleId(role.getId());
        userRoles.insert(link);
        var login =
                request(
                        "POST",
                        "/api/v1/admin/auth/login",
                        Map.of(
                                "username",
                                user.getUsername(),
                                "password",
                                PASSWORD,
                                "device",
                                "APP"),
                        false);
        assertThat(login.statusCode()).isEqualTo(200);
        token = json.readTree(login.body()).path("data").path("token").asText();
    }

    HttpResponse<String> request(String method, String path, Object body, boolean authenticated)
            throws Exception {
        var builder =
                HttpRequest.newBuilder(
                                URI.create(
                                        "http://localhost:"
                                                + env.getProperty("local.server.port")
                                                + path))
                        .header("Content-Type", "application/json");
        if (authenticated) builder.header("Authorization", "Bearer " + token);
        return client.send(
                builder.method(
                                method,
                                body == null
                                        ? HttpRequest.BodyPublishers.noBody()
                                        : HttpRequest.BodyPublishers.ofString(
                                                json.writeValueAsString(body)))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    DepartmentDetailView create(long parent) {
        String code = "D_" + unique();
        return departments.create(
                new CreateDepartmentCommand(parent, code, code, 0, DepartmentStatus.ENABLED));
    }

    @Test
    void rootCycleAndReferenceProtections() throws Exception {
        var a = create(root);
        var child = create(a.id());
        assertThat(
                        request(
                                        "PUT",
                                        "/api/v1/admin/iam/departments/" + a.id(),
                                        Map.of(
                                                "parentId",
                                                child.id(),
                                                "name",
                                                a.name(),
                                                "sort",
                                                0,
                                                "version",
                                                0),
                                        true)
                                .body())
                .contains("IAM_DEPARTMENT_CYCLE");
        assertThat(
                        request(
                                        "DELETE",
                                        "/api/v1/admin/iam/departments/" + root + "?version=0",
                                        null,
                                        true)
                                .body())
                .contains("IAM_DEPARTMENT_PROTECTED");
        assertThat(
                        request(
                                        "DELETE",
                                        "/api/v1/admin/iam/departments/" + a.id() + "?version=0",
                                        null,
                                        true)
                                .body())
                .contains("IAM_DEPARTMENT_HAS_CHILDREN");
        assertThat(
                        request(
                                        "PATCH",
                                        "/api/v1/admin/iam/departments/" + a.id() + "/status",
                                        Map.of("status", "DISABLED", "version", 0),
                                        true)
                                .body())
                .contains("IAM_DEPARTMENT_HAS_ENABLED_DESCENDANTS");
        var user =
                users.create(
                        new CreateUserCommand(
                                "u-" + unique(),
                                PASSWORD,
                                "测试",
                                null,
                                null,
                                List.of(),
                                child.id(),
                                UserStatus.DISABLED));
        assertThat(
                        request(
                                        "DELETE",
                                        "/api/v1/admin/iam/departments/"
                                                + child.id()
                                                + "?version=0",
                                        null,
                                        true)
                                .body())
                .contains("IAM_DEPARTMENT_IN_USE");
        assertThat(user.departmentId()).isEqualTo(child.id());
    }

    @Test
    void userProfileIsAtomicAndOldPutPreservesDepartment() throws Exception {
        var dept = create(root);
        var created =
                users.create(
                        new CreateUserCommand(
                                "u-" + unique(),
                                PASSWORD,
                                "原名",
                                "before@example.com",
                                "13800000000",
                                List.of(),
                                dept.id(),
                                null));
        var body = new LinkedHashMap<String, Object>();
        body.put("nickname", "新名");
        body.put("email", null);
        body.put("phone", null);
        body.put("departmentId", dept.id());
        body.put("version", created.version());
        var response =
                request("PUT", "/api/v1/admin/iam/users/" + created.id() + "/profile", body, true);
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        var saved = userMapper.selectById(created.id());
        assertThat(saved.getEmail()).isNull();
        assertThat(saved.getPhone()).isNull();
        assertThat(
                        request(
                                        "PUT",
                                        "/api/v1/admin/iam/users/" + created.id() + "/profile",
                                        body,
                                        true)
                                .statusCode())
                .isEqualTo(409);
        body.remove("email");
        body.put("version", saved.getVersion());
        assertThat(
                        request(
                                        "PUT",
                                        "/api/v1/admin/iam/users/" + created.id() + "/profile",
                                        body,
                                        true)
                                .statusCode())
                .isEqualTo(400);
        var old = new LinkedHashMap<String, Object>();
        old.put("nickname", "旧客户端");
        old.put("email", null);
        old.put("phone", null);
        old.put("version", saved.getVersion());
        assertThat(
                        request("PUT", "/api/v1/admin/iam/users/" + created.id(), old, true)
                                .statusCode())
                .isEqualTo(200);
        assertThat(userMapper.selectById(created.id()).getDepartmentId()).isEqualTo(dept.id());
    }

    @Test
    void departmentFiltersAndKeywordHaveStablePagination() throws Exception {
        var a = create(root);
        var child = create(a.id());
        String tag = unique();
        users.create(
                new CreateUserCommand(
                        "u-" + unique(),
                        PASSWORD,
                        tag,
                        null,
                        "13800000001",
                        List.of(),
                        child.id(),
                        null));
        var response =
                request(
                        "GET",
                        "/api/v1/admin/iam/users?departmentId=" + a.id() + "&keyword=" + tag,
                        null,
                        true);
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        var data = json.readTree(response.body()).path("data");
        assertThat(data.path("total").asInt()).isEqualTo(1);
        assertThat(data.path("items").get(0).path("roles").isArray()).isTrue();
        response =
                request(
                        "GET",
                        "/api/v1/admin/iam/users?departmentId="
                                + a.id()
                                + "&includeDescendants=false&keyword="
                                + tag,
                        null,
                        true);
        assertThat(json.readTree(response.body()).path("data").path("total").asInt()).isZero();
        assertThat(
                        request(
                                        "GET",
                                        "/api/v1/admin/iam/users?departmentId="
                                                + a.id()
                                                + "&unassignedDepartment=true",
                                        null,
                                        true)
                                .statusCode())
                .isEqualTo(400);
    }

    @Test
    void disabledDepartmentAllowsExistingProfileButNotNewAssignments() throws Exception {
        var dept = create(root);
        var user =
                users.create(
                        new CreateUserCommand(
                                "u-" + unique(),
                                PASSWORD,
                                "测试",
                                null,
                                null,
                                List.of(),
                                dept.id(),
                                null));
        departments.changeStatus(dept.id(), DepartmentStatus.DISABLED, 0);
        users.updateProfile(
                user.id(),
                new UpdateUserProfileCommand("改名", null, null, dept.id(), user.version()));
        assertThatThrownBy(
                        () ->
                                users.create(
                                        new CreateUserCommand(
                                                "u-" + unique(),
                                                PASSWORD,
                                                "测试",
                                                null,
                                                null,
                                                List.of(),
                                                dept.id(),
                                                null)))
                .hasMessageContaining("停用");
        assertThat(departments.options().toString()).contains("selectable=false");
    }

    @Test
    void concurrentOppositeMovesCannotCreateCycle() throws Exception {
        var a = create(root);
        var b = create(root);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var gate = new CountDownLatch(1);
            Callable<Boolean> first =
                    () -> {
                        gate.await();
                        try {
                            departments.update(
                                    a.id(), new UpdateDepartmentCommand(b.id(), a.name(), 0, 0));
                            return true;
                        } catch (io.github.keel.common.core.exception.BusinessException e) {
                            assertThat(e.getErrorCode().code()).isEqualTo("IAM_DEPARTMENT_CYCLE");
                            return false;
                        }
                    };
            Callable<Boolean> second =
                    () -> {
                        gate.await();
                        try {
                            departments.update(
                                    b.id(), new UpdateDepartmentCommand(a.id(), b.name(), 0, 0));
                            return true;
                        } catch (io.github.keel.common.core.exception.BusinessException e) {
                            assertThat(e.getErrorCode().code()).isEqualTo("IAM_DEPARTMENT_CYCLE");
                            return false;
                        }
                    };
            var x = pool.submit(first);
            var y = pool.submit(second);
            gate.countDown();
            assertThat(List.of(x.get(15, TimeUnit.SECONDS), y.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        }
    }

    @Test
    void versionIsRequiredAndEmptyLeafCanBeDeleted() throws Exception {
        var dept = create(root);
        assertThat(
                        request("DELETE", "/api/v1/admin/iam/departments/" + dept.id(), null, true)
                                .statusCode())
                .isEqualTo(400);
        assertThat(
                        request(
                                        "DELETE",
                                        "/api/v1/admin/iam/departments/" + dept.id() + "?version=5",
                                        null,
                                        true)
                                .statusCode())
                .isEqualTo(409);
        assertThat(
                        request(
                                        "DELETE",
                                        "/api/v1/admin/iam/departments/" + dept.id() + "?version=0",
                                        null,
                                        true)
                                .statusCode())
                .isEqualTo(204);
    }

    @Test
    void oldCreateOmitsDepartmentAndStatusButExplicitNullStatusIsRejected() throws Exception {
        var body = new LinkedHashMap<String, Object>();
        body.put("username", "u-" + unique());
        body.put("password", PASSWORD);
        body.put("nickname", "旧客户端");
        var response = request("POST", "/api/v1/admin/iam/users", body, true);
        assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
        var data = json.readTree(response.body()).path("data");
        assertThat(data.path("status").asText()).isEqualTo("ENABLED");
        assertThat(data.hasNonNull("departmentId")).isFalse();
        body.put("username", "u-" + unique());
        body.put("status", null);
        assertThat(request("POST", "/api/v1/admin/iam/users", body, true).statusCode())
                .isEqualTo(400);
    }

    @Test
    void assignmentAndDeletionCannotLeaveAnOrphan() throws Exception {
        var department = create(root);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var gate = new CountDownLatch(1);
            var assignment =
                    pool.submit(
                            () -> {
                                gate.await();
                                try {
                                    users.create(
                                            new CreateUserCommand(
                                                    "u-" + unique(),
                                                    PASSWORD,
                                                    "并发",
                                                    null,
                                                    null,
                                                    List.of(),
                                                    department.id(),
                                                    null));
                                    return true;
                                } catch (io.github.keel.common.core.exception.BusinessException e) {
                                    assertThat(e.getErrorCode().code())
                                            .isEqualTo("IAM_DEPARTMENT_NOT_FOUND");
                                    return false;
                                }
                            });
            var deletion =
                    pool.submit(
                            () -> {
                                gate.await();
                                try {
                                    departments.delete(department.id(), 0);
                                    return true;
                                } catch (io.github.keel.common.core.exception.BusinessException e) {
                                    assertThat(e.getErrorCode().code())
                                            .isEqualTo("IAM_DEPARTMENT_IN_USE");
                                    return false;
                                }
                            });
            gate.countDown();
            assertThat(
                            List.of(
                                    assignment.get(15, TimeUnit.SECONDS),
                                    deletion.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
            if (departmentMapper.selectById(department.id()) == null)
                assertThat(
                                userMapper.selectCount(
                                        Wrappers.<IamUserPO>lambdaQuery()
                                                .eq(IamUserPO::getDepartmentId, department.id())))
                        .isZero();
        }
    }

    @Autowired IamResourceMapper resources;
    @Autowired IamRoleResourceMapper roleResources;

    @Test
    void userListPermissionCanReadOptionsButCannotManageDepartments() throws Exception {
        var created =
                users.create(
                        new CreateUserCommand(
                                "u-" + unique(),
                                PASSWORD,
                                "普通账号",
                                null,
                                null,
                                List.of(),
                                null,
                                null));
        var user = userMapper.selectById(created.id());
        user.setMustChangePassword(false);
        userMapper.updateById(user);
        var role = new IamRolePO();
        role.setCode("R_" + unique());
        role.setName("仅查询用户");
        role.setStatus("ENABLED");
        role.setSort(0);
        roleMapper.insert(role);
        var resource =
                resources.selectOne(
                        Wrappers.<IamResourcePO>lambdaQuery()
                                .eq(IamResourcePO::getCode, "iam:user:list"));
        var grant = new IamRoleResourcePO();
        grant.setRoleId(role.getId());
        grant.setResourceId(resource.getId());
        roleResources.insert(grant);
        users.replaceRoles(created.id(), List.of(role.getId()));
        var login =
                request(
                        "POST",
                        "/api/v1/admin/auth/login",
                        Map.of(
                                "username",
                                created.username(),
                                "password",
                                PASSWORD,
                                "device",
                                "APP"),
                        false);
        token = json.readTree(login.body()).path("data").path("token").asText();
        var options = request("GET", "/api/v1/admin/iam/departments/options", null, true);
        assertThat(options.statusCode()).as(options.body()).isEqualTo(200);
        assertThat(json.readTree(options.body()).path("data").get(0).has("code")).isFalse();
        assertThat(request("GET", "/api/v1/admin/iam/departments/tree", null, true).statusCode())
                .isEqualTo(403);
        assertThat(
                        request(
                                        "POST",
                                        "/api/v1/admin/iam/departments",
                                        Map.of(
                                                "parentId",
                                                root,
                                                "code",
                                                "NO_" + unique(),
                                                "name",
                                                "不可创建",
                                                "sort",
                                                0,
                                                "status",
                                                "ENABLED"),
                                        true)
                                .statusCode())
                .isEqualTo(403);
    }
}
