package io.github.keel.iam.application.identity;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import io.github.keel.common.core.auth.CurrentUserProvider;
import io.github.keel.common.core.error.CommonErrorCode;
import io.github.keel.common.core.exception.BusinessException;
import io.github.keel.common.security.token.TokenService;
import io.github.keel.iam.application.IamErrorCode;
import io.github.keel.iam.application.authorization.AuthorizationCacheEvictor;
import io.github.keel.iam.application.authorization.RoleBriefView;
import io.github.keel.iam.application.authorization.RoleStatus;
import io.github.keel.iam.infrastructure.persistence.mapper.IamRoleMapper;
import io.github.keel.iam.infrastructure.persistence.mapper.IamUserMapper;
import io.github.keel.iam.infrastructure.persistence.mapper.IamUserRoleMapper;
import io.github.keel.iam.infrastructure.persistence.po.IamRolePO;
import io.github.keel.iam.infrastructure.persistence.po.IamUserPO;
import io.github.keel.iam.infrastructure.persistence.po.IamUserRolePO;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 用户维护：创建、资料更新、启停、重置密码、角色分配、强制下线与受保护的永久删除。 */
@Service
@RequiredArgsConstructor
public class UserApplicationService {

    private static final String STATUS_ENABLED = "ENABLED";

    private final io.github.keel.iam.application.organization.DepartmentApplicationService
            departments;
    private final IamUserMapper userMapper;
    private final IamRoleMapper roleMapper;
    private final IamUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuthorizationCacheEvictor cacheEvictor;
    private final CurrentUserProvider currentUserProvider;

    public UserDetailView detail(long id) {
        IamUserPO user = requireUser(id);
        return toDetail(user, rolesOf(id));
    }

    @Transactional
    public UserDetailView create(CreateUserCommand command) {
        departments.validateAssignment(command.departmentId(), null);
        Long existing =
                userMapper.selectCount(
                        Wrappers.<IamUserPO>lambdaQuery()
                                .eq(IamUserPO::getUsername, command.username()));
        if (existing != null && existing > 0) {
            throw new BusinessException(IamErrorCode.USERNAME_EXISTS);
        }
        Set<Long> roleIds = validateRoleIds(command.roleIds());

        IamUserPO user = new IamUserPO();
        user.setUsername(command.username());
        user.setPasswordHash(passwordEncoder.encode(command.password()));
        user.setNickname(command.nickname());
        user.setEmail(command.email());
        user.setPhone(command.phone());
        user.setDepartmentId(command.departmentId());
        user.setStatus(command.status() == null ? STATUS_ENABLED : command.status().name());
        user.setMustChangePassword(true);
        user.setVersion(0);
        userMapper.insert(user);

        roleIds.forEach(roleId -> insertUserRole(user.getId(), roleId));
        return toDetail(user, rolesOf(user.getId()));
    }

    @Transactional
    public UserDetailView update(long id, UpdateUserCommand command) {
        IamUserPO user = requireUser(id);
        user.setNickname(command.nickname());
        user.setEmail(command.email());
        user.setPhone(command.phone());
        user.setVersion(command.version());
        if (userMapper.updateById(user) == 0) {
            throw new BusinessException(CommonErrorCode.OPTIMISTIC_LOCK);
        }
        return toDetail(userMapper.selectById(id), rolesOf(id));
    }

    @Transactional
    public UserDetailView updateProfile(long id, UpdateUserProfileCommand command) {
        IamUserPO user = requireUser(id);
        departments.validateAssignment(command.departmentId(), user.getDepartmentId());
        user.setNickname(command.nickname().trim());
        user.setEmail(command.email());
        user.setPhone(command.phone());
        user.setDepartmentId(command.departmentId());
        user.setVersion(command.version());
        if (userMapper.updateById(user) == 0)
            throw new BusinessException(CommonErrorCode.OPTIMISTIC_LOCK);
        return toDetail(userMapper.selectById(id), rolesOf(id));
    }

    @Transactional
    public UserDetailView updateStatus(long id, UserStatus status, Integer version) {
        IamUserPO user = requireUser(id);
        user.setStatus(status.name());
        user.setVersion(version);
        if (userMapper.updateById(user) == 0) {
            throw new BusinessException(CommonErrorCode.OPTIMISTIC_LOCK);
        }

        if (status == UserStatus.DISABLED) {
            tokenService.revokeAll(id);
            cacheEvictor.evictUser(id);
        }
        return toDetail(userMapper.selectById(id), rolesOf(id));
    }

    @Transactional
    public void resetPassword(long id, ResetPasswordCommand command) {
        IamUserPO user = requireUser(id);
        user.setPasswordHash(passwordEncoder.encode(command.newPassword()));
        user.setMustChangePassword(true);
        user.setVersion(command.version());
        if (userMapper.updateById(user) == 0) {
            throw new BusinessException(CommonErrorCode.OPTIMISTIC_LOCK);
        }

        tokenService.revokeAll(id);
        cacheEvictor.evictUser(id);
    }

    public List<RoleBriefView> rolesOf(long userId) {
        requireUser(userId);
        List<Long> roleIds =
                userRoleMapper
                        .selectList(
                                Wrappers.<IamUserRolePO>lambdaQuery()
                                        .eq(IamUserRolePO::getUserId, userId))
                        .stream()
                        .map(IamUserRolePO::getRoleId)
                        .toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }

        Map<Long, IamRolePO> roles =
                roleMapper.selectBatchIds(roleIds).stream()
                        .collect(Collectors.toMap(IamRolePO::getId, Function.identity()));
        List<RoleBriefView> result = new ArrayList<>();
        for (Long roleId : roleIds) {
            IamRolePO role = roles.get(roleId);
            if (role != null) {
                result.add(
                        new RoleBriefView(
                                role.getId(),
                                role.getCode(),
                                role.getName(),
                                RoleStatus.valueOf(role.getStatus())));
            }
        }
        return result;
    }

    @Transactional
    public void replaceRoles(long userId, List<Long> roleIds) {
        requireLockedUser(userId);
        Set<Long> uniqueIds = validateRoleIds(roleIds);

        userRoleMapper.delete(
                Wrappers.<IamUserRolePO>lambdaQuery().eq(IamUserRolePO::getUserId, userId));
        uniqueIds.forEach(roleId -> insertUserRole(userId, roleId));
        cacheEvictor.evictUser(userId);
    }

    @Transactional
    public void delete(long userId, int version) {
        IamUserPO user = requireLockedUser(userId);
        if (Objects.equals(currentUserProvider.currentUserId(), userId)
                || userMapper.countProtectedRole(userId) > 0) {
            throw new BusinessException(IamErrorCode.USER_PROTECTED);
        }
        if (!Objects.equals(user.getVersion(), version)) {
            throw new BusinessException(CommonErrorCode.OPTIMISTIC_LOCK);
        }
        userRoleMapper.delete(
                Wrappers.<IamUserRolePO>lambdaQuery().eq(IamUserRolePO::getUserId, userId));
        if (userMapper.delete(
                        Wrappers.<IamUserPO>lambdaQuery()
                                .eq(IamUserPO::getId, userId)
                                .eq(IamUserPO::getVersion, version))
                == 0) {
            throw new BusinessException(CommonErrorCode.OPTIMISTIC_LOCK);
        }
        // 清理失败时让数据库事务回滚；已吊销的会话不恢复。
        tokenService.revokeAll(userId);
        cacheEvictor.evictUser(userId);
    }

    public void forceLogout(long userId) {
        requireUser(userId);
        tokenService.revokeAll(userId);
    }

    public void forceLogoutDevice(long userId, String loginType, String device) {
        requireUser(userId);
        tokenService.revokeDevice(userId, loginType, device);
    }

    private Set<Long> validateRoleIds(List<Long> roleIds) {
        if (roleIds == null) {
            return Set.of();
        }
        Set<Long> uniqueIds = new LinkedHashSet<>(roleIds);
        if (uniqueIds.contains(null)) {
            throw new BusinessException(IamErrorCode.INVALID_ROLE_IDS);
        }
        if (uniqueIds.isEmpty()) {
            return uniqueIds;
        }

        List<IamRolePO> roles = roleMapper.selectBatchIds(uniqueIds);
        boolean allEnabled =
                roles.size() == uniqueIds.size()
                        && roles.stream()
                                .allMatch(
                                        role -> RoleStatus.ENABLED.name().equals(role.getStatus()));
        if (!allEnabled) {
            throw new BusinessException(IamErrorCode.INVALID_ROLE_IDS);
        }
        return uniqueIds;
    }

    private void insertUserRole(long userId, long roleId) {
        IamUserRolePO link = new IamUserRolePO();
        link.setUserId(userId);
        link.setRoleId(roleId);
        userRoleMapper.insert(link);
    }

    private IamUserPO requireUser(long id) {
        IamUserPO user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(IamErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private IamUserPO requireLockedUser(long id) {
        IamUserPO user = userMapper.lockById(id);
        if (user == null) {
            throw new BusinessException(IamErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private UserDetailView toDetail(IamUserPO user, List<RoleBriefView> roles) {
        var department =
                user.getDepartmentId() == null
                        ? null
                        : departments
                                .briefs(List.of(user.getDepartmentId()))
                                .get(user.getDepartmentId());
        return new UserDetailView(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getPhone(),
                UserStatus.valueOf(user.getStatus()),
                Boolean.TRUE.equals(user.getMustChangePassword()),
                user.getLastLoginAt(),
                user.getLastLoginIp(),
                roles,
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getVersion(),
                user.getDepartmentId(),
                department == null ? null : department.name(),
                department == null ? null : department.status());
    }
}
