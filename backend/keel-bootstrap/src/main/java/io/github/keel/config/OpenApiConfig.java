package io.github.keel.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.keel.common.security.config.SecurityTokenProperties;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 分组、安全方案与响应示例。
 *
 * <p>示例通过分组自定义器挂到 SpringDoc 推断出的成功响应上，避免在 Controller 中重复声明 schema。</p>
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String ERROR_SCHEMA_REF = "#/components/schemas/ErrorResponse";

    private static final String LOGIN_EXAMPLE = """
            {"code":"OK","message":"success","data":{"user":{"id":1,"username":"admin","nickname":"超级管理员"},"superAdmin":true,"permissions":[],"menus":[],"mustChangePassword":false}}
            """;

    private static final String SESSION_EXAMPLE = """
            {"code":"OK","message":"success","data":{"user":{"id":1,"username":"admin","nickname":"超级管理员"},"superAdmin":true,"permissions":[],"menus":[],"mustChangePassword":false}}
            """;

    private static final String CSRF_EXAMPLE = """
            {"code":"OK","message":"success","data":{"token":"csrf-token"}}
            """;

    private static final String USER_PAGE_EXAMPLE = """
            {"code":"OK","message":"success","data":{"items":[{"id":2,"username":"operator","nickname":"运营","status":"ENABLED","lastLoginAt":"2026-11-12T10:00:00.000","createdAt":"2026-11-01T09:00:00.000"}],"total":1,"page":1,"size":20}}
            """;

    private static final String USER_DETAIL_EXAMPLE = """
            {"code":"OK","message":"success","data":{"id":2,"username":"operator","nickname":"运营","email":"ops@example.com","phone":null,"status":"ENABLED","mustChangePassword":false,"lastLoginAt":"2026-11-12T10:00:00.000","lastLoginIp":"127.0.0.1","roles":[{"id":3,"code":"OPS","name":"运维","status":"ENABLED"}],"createdAt":"2026-11-01T09:00:00.000","updatedAt":"2026-11-12T10:00:00.000","version":0}}
            """;

    private static final String ROLE_PAGE_EXAMPLE = """
            {"code":"OK","message":"success","data":{"items":[{"id":3,"code":"OPS","name":"运维","description":"运维角色","status":"ENABLED","sort":10,"createdAt":"2026-11-01T09:00:00.000"}],"total":1,"page":1,"size":20}}
            """;

    private static final String ROLE_DETAIL_EXAMPLE = """
            {"code":"OK","message":"success","data":{"id":3,"code":"OPS","name":"运维","description":"运维角色","status":"ENABLED","sort":10,"createdAt":"2026-11-01T09:00:00.000","updatedAt":"2026-11-12T10:00:00.000","version":0}}
            """;

    private static final String RESOURCE_TREE_EXAMPLE = """
            {"code":"OK","message":"success","data":[{"id":1,"parentId":0,"type":"DIRECTORY","code":"iam:directory","name":"身份与权限","path":"/iam","component":null,"icon":"shield","sort":20,"visible":true,"status":"ENABLED","children":[{"id":2,"parentId":1,"type":"MENU","code":"iam:user:list","name":"用户管理","path":"/iam/users","component":"iam/users/index","icon":"users","sort":201,"visible":true,"status":"ENABLED","children":[]}]}]}
            """;

    /**
     * Cookie 与 Bearer 二选一；公开端点通过 {@code @SecurityRequirements} 覆盖为无认证。
     */
    @Bean
    public OpenAPI keelOpenApi(SecurityTokenProperties securityTokenProperties) {
        return new OpenAPI()
                .info(new Info()
                        .title("Keel API")
                        .version("v1")
                        .description("Keel 管理端与门户公开接口"))
                .components(new Components()
                        .addSecuritySchemes("cookieAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name(securityTokenProperties.cookieName()))
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("opaque")))
                .addSecurityItem(new SecurityRequirement().addList("cookieAuth"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    @Bean
    public GroupedOpenApi adminOpenApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .pathsToMatch("/api/v1/admin/**")
                .addOpenApiCustomizer(OpenApiConfig::customiseDocument)
                .build();
    }

    @Bean
    public GroupedOpenApi publicOpenApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch("/api/v1/public/**")
                .addOpenApiCustomizer(OpenApiConfig::customiseDocument)
                .build();
    }

    private static void customiseDocument(OpenAPI openApi) {
        registerErrorSchemas(openApi);
        applyErrorSchema(openApi);
        addExamples(openApi);
    }

    /**
     * 分组文档会裁剪未引用的 schema，因此在自定义器中注册错误 schema。
     */
    private static void registerErrorSchemas(OpenAPI openApi) {
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }
        openApi.getComponents()
                .addSchemas("FieldError", new ObjectSchema()
                        .addProperty("field", new StringSchema())
                        .addProperty("message", new StringSchema())
                        .required(List.of("field", "message")))
                .addSchemas("ErrorResponse", new ObjectSchema()
                        .addProperty("code", new StringSchema())
                        .addProperty("message", new StringSchema())
                        .addProperty("data", new ObjectSchema()
                                .addProperty("fieldErrors", new ArraySchema()
                                        .items(new Schema<>().$ref("#/components/schemas/FieldError"))))
                        .required(List.of("code", "message")));
    }

    /**
     * 非 2xx 响应统一引用 {@code ErrorResponse}，避免 SpringDoc 复用成功响应的泛型。
     */
    private static void applyErrorSchema(OpenAPI openApi) {
        if (openApi.getPaths() == null) {
            return;
        }
        for (PathItem pathItem : openApi.getPaths().values()) {
            for (Operation operation : pathItem.readOperations()) {
                if (operation.getResponses() == null) {
                    continue;
                }
                operation.getResponses().forEach((status, response) -> {
                    if (!status.startsWith("2")) {
                        mediaTypeOf(response).setSchema(new Schema<>().$ref(ERROR_SCHEMA_REF));
                    }
                });
            }
        }
    }

    private static void addExamples(OpenAPI openApi) {
        addExample(openApi, "/api/v1/admin/auth/login", "200", "webLogin", LOGIN_EXAMPLE);
        addExample(openApi, "/api/v1/admin/auth/me", "200", "currentSession", SESSION_EXAMPLE);
        addExample(openApi, "/api/v1/admin/auth/csrf", "200", "csrfToken", CSRF_EXAMPLE);
        addExample(openApi, "/api/v1/admin/iam/users", "200", "userPage", USER_PAGE_EXAMPLE);
        addExample(openApi, "/api/v1/admin/iam/users/{id}", "200", "userDetail", USER_DETAIL_EXAMPLE);
        addExample(openApi, "/api/v1/admin/iam/roles", "200", "rolePage", ROLE_PAGE_EXAMPLE);
        addExample(openApi, "/api/v1/admin/iam/roles/{id}", "200", "roleDetail", ROLE_DETAIL_EXAMPLE);
        addExample(openApi, "/api/v1/admin/iam/resources/tree", "200", "resourceTree", RESOURCE_TREE_EXAMPLE);
    }

    private static void addExample(OpenAPI openApi, String path, String status, String name, String json) {
        PathItem pathItem = openApi.getPaths() == null ? null : openApi.getPaths().get(path);
        if (pathItem == null) {
            return;
        }
        for (Operation operation : pathItem.readOperations()) {
            ApiResponse response = operation.getResponses() == null ? null : operation.getResponses().get(status);
            if (response != null) {
                mediaTypeOf(response).addExamples(name, new Example().value(readJson(json)));
            }
        }
    }

    private static MediaType mediaTypeOf(ApiResponse response) {
        Content content = response.getContent();
        if (content == null) {
            content = new Content();
            response.setContent(content);
        }
        if (content.isEmpty()) {
            content.addMediaType("application/json", new MediaType());
        }
        return content.values().iterator().next();
    }

    private static Object readJson(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, Object.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("OpenAPI 示例不是合法 JSON", exception);
        }
    }
}
