package io.github.keel;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.keel.iam.infrastructure.persistence.mapper.IamUserMapper;
import io.github.keel.iam.infrastructure.persistence.po.IamUserPO;
import io.github.keel.support.IntegrationTestSupport;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 1A 技术底座验证：Flyway 多模块扫描、MyBatis-Plus CRUD/分页、Redis 连接。
 */
@SpringBootTest
@ActiveProfiles("test")
class PersistenceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private Flyway flyway;

    @Autowired
    private IamUserMapper userMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void migrationsRunInOrderAndSeedsAreLoaded() {
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.info().applied()).hasSize(12);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("9004");

        assertThat(count("SELECT COUNT(*) FROM iam_role WHERE code = 'SUPER_ADMIN'")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM iam_user WHERE username = 'admin'")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM iam_resource")).isEqualTo(22);
        assertThat(count("SELECT COUNT(*) FROM sys_config")).isEqualTo(10);
    }

    @Test
    void mybatisPlusCrudAndPaginationWork() {
        for (int index = 1; index <= 3; index++) {
            IamUserPO user = new IamUserPO();
            user.setUsername("phase1a-user-" + index);
            user.setPasswordHash("$2a$10$0123456789012345678901234567890123456789012345678901");
            user.setNickname("测试用户" + index);
            user.setStatus("ENABLED");
            user.setMustChangePassword(false);

            assertThat(userMapper.insert(user)).isEqualTo(1);
            assertThat(user.getId()).isNotNull();
            assertThat(user.getCreatedAt()).isNotNull();
            assertThat(user.getCreatedBy()).isZero();
        }

        Page<IamUserPO> page = userMapper.selectPage(
                new Page<>(1, 2),
                Wrappers.<IamUserPO>lambdaQuery()
                        .likeRight(IamUserPO::getUsername, "phase1a-user-")
                        .orderByAsc(IamUserPO::getId));

        assertThat(page.getTotal()).isEqualTo(3);
        assertThat(page.getRecords()).hasSize(2);
        assertThat(page.getRecords())
                .extracting(IamUserPO::getUsername)
                .containsExactly("phase1a-user-1", "phase1a-user-2");

        IamUserPO toUpdate = userMapper.selectById(page.getRecords().getFirst().getId());
        toUpdate.setNickname("更新后的昵称");
        assertThat(userMapper.updateById(toUpdate)).isEqualTo(1);

        IamUserPO updated = userMapper.selectById(toUpdate.getId());
        assertThat(updated.getNickname()).isEqualTo("更新后的昵称");
        assertThat(updated.getVersion()).isEqualTo(1);
    }

    @Test
    void redisConnectionWorks() {
        redisTemplate.opsForValue().set("keel:phase1a", "ok", Duration.ofMinutes(1));
        assertThat(redisTemplate.opsForValue().get("keel:phase1a")).isEqualTo("ok");
    }

    private int count(String sql) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }
}
