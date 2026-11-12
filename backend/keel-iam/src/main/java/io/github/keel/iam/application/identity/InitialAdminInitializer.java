package io.github.keel.iam.application.identity;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.keel.iam.infrastructure.persistence.mapper.IamRoleMapper;
import io.github.keel.iam.infrastructure.persistence.mapper.IamUserMapper;
import io.github.keel.iam.infrastructure.persistence.mapper.IamUserRoleMapper;
import io.github.keel.iam.infrastructure.persistence.po.IamRolePO;
import io.github.keel.iam.infrastructure.persistence.po.IamUserPO;
import io.github.keel.iam.infrastructure.persistence.po.IamUserRolePO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 首次启动创建初始管理员，绑定 SUPER_ADMIN，并要求首次登录改密。
 */
@Component
public class InitialAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InitialAdminInitializer.class);
    private static final String SUPER_ADMIN_CODE = "SUPER_ADMIN";

    private final IamUserMapper userMapper;
    private final IamRoleMapper roleMapper;
    private final IamUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    public InitialAdminInitializer(IamUserMapper userMapper,
                                   IamRoleMapper roleMapper,
                                   IamUserRoleMapper userRoleMapper,
                                   PasswordEncoder passwordEncoder,
                                   @Value("${keel.admin.username:}") String username,
                                   @Value("${keel.admin.password:}") String password) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            log.info("未配置初始管理员，跳过创建");
            return;
        }

        IamUserPO existing = userMapper.selectOne(Wrappers.<IamUserPO>lambdaQuery()
                .eq(IamUserPO::getUsername, username));
        if (existing != null) {
            log.info("初始管理员已存在: {}", username);
            return;
        }

        IamRolePO superAdminRole = roleMapper.selectOne(Wrappers.<IamRolePO>lambdaQuery()
                .eq(IamRolePO::getCode, SUPER_ADMIN_CODE));
        if (superAdminRole == null) {
            log.error("SUPER_ADMIN 角色不存在，无法创建初始管理员");
            return;
        }

        IamUserPO user = new IamUserPO();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setNickname("超级管理员");
        user.setStatus("ENABLED");
        user.setMustChangePassword(true);
        userMapper.insert(user);

        IamUserRolePO link = new IamUserRolePO();
        link.setUserId(user.getId());
        link.setRoleId(superAdminRole.getId());
        userRoleMapper.insert(link);

        // 不输出密码
        log.info("已创建初始管理员: {}", username);
    }
}
