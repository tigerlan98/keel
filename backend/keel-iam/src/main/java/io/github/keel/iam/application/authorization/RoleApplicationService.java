package io.github.keel.iam.application.authorization;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.keel.common.core.error.CommonErrorCode;
import io.github.keel.common.core.exception.BusinessException;
import io.github.keel.common.core.result.PageResult;
import io.github.keel.iam.application.IamErrorCode;
import io.github.keel.iam.infrastructure.persistence.mapper.IamResourceMapper;
import io.github.keel.iam.infrastructure.persistence.mapper.IamRoleMapper;
import io.github.keel.iam.infrastructure.persistence.mapper.IamRoleResourceMapper;
import io.github.keel.iam.infrastructure.persistence.mapper.IamUserRoleMapper;
import io.github.keel.iam.infrastructure.persistence.po.IamResourcePO;
import io.github.keel.iam.infrastructure.persistence.po.IamRolePO;
import io.github.keel.iam.infrastructure.persistence.po.IamRoleResourcePO;
import io.github.keel.iam.infrastructure.persistence.po.IamUserRolePO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 角色查询、维护与资源授权。
 */
@Service
@RequiredArgsConstructor
public class RoleApplicationService {

    private static final String SUPER_ADMIN_CODE = "SUPER_ADMIN";
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;

    private final IamRoleMapper roleMapper;
    private final IamResourceMapper resourceMapper;
    private final IamUserRoleMapper userRoleMapper;
    private final IamRoleResourceMapper roleResourceMapper;
    private final AuthorizationCacheEvictor cacheEvictor;

    public PageResult<RoleSummaryView> page(RolePageCommand command) {
        int pageNumber = command.page() == null ? DEFAULT_PAGE : command.page();
        int pageSize = command.size() == null ? DEFAULT_SIZE : command.size();

        LambdaQueryWrapper<IamRolePO> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(command.keyword())) {
            String keyword = command.keyword().trim();
            wrapper.and(condition -> condition
                    .likeRight(IamRolePO::getName, keyword)
                    .or()
                    .likeRight(IamRolePO::getCode, keyword));
        }
        if (command.status() != null) {
            wrapper.eq(IamRolePO::getStatus, command.status().name());
        }
        applySort(wrapper, command.sort());

        Page<IamRolePO> result = roleMapper.selectPage(new Page<>(pageNumber, pageSize), wrapper);
        return new PageResult<>(
                result.getRecords().stream().map(this::toSummary).toList(),
                result.getTotal(),
                result.getCurrent(),
                result.getSize());
    }

    public RoleDetailView detail(long id) {
        return toDetail(requireRole(id));
    }

    @Transactional
    public RoleDetailView create(CreateRoleCommand command) {
        Long existing = roleMapper.selectCount(Wrappers.<IamRolePO>lambdaQuery()
                .eq(IamRolePO::getCode, command.code()));
        if (existing != null && existing > 0) {
            throw new BusinessException(IamErrorCode.ROLE_CODE_EXISTS);
        }

        IamRolePO role = new IamRolePO();
        role.setCode(command.code());
        role.setName(command.name());
        role.setDescription(command.description());
        role.setStatus(command.status() == null ? RoleStatus.ENABLED.name() : command.status().name());
        role.setSort(command.sort() == null ? 0 : command.sort());
        roleMapper.insert(role);
        return toDetail(role);
    }

    @Transactional
    public RoleDetailView update(long id, UpdateRoleCommand command) {
        IamRolePO role = requireRole(id);
        if (SUPER_ADMIN_CODE.equals(role.getCode()) && command.status() == RoleStatus.DISABLED) {
            throw new BusinessException(IamErrorCode.ROLE_PROTECTED, "内置角色不允许禁用");
        }

        role.setName(command.name());
        role.setDescription(command.description());
        role.setStatus(command.status().name());
        role.setSort(command.sort());
        role.setVersion(command.version());
        if (roleMapper.updateById(role) == 0) {
            throw new BusinessException(CommonErrorCode.OPTIMISTIC_LOCK);
        }

        cacheEvictor.evictRole(id);
        return toDetail(roleMapper.selectById(id));
    }

    @Transactional
    public void delete(long id) {
        IamRolePO role = requireRole(id);
        if (SUPER_ADMIN_CODE.equals(role.getCode())) {
            throw new BusinessException(IamErrorCode.ROLE_PROTECTED, "内置角色不允许删除");
        }

        Long references = userRoleMapper.selectCount(Wrappers.<IamUserRolePO>lambdaQuery()
                .eq(IamUserRolePO::getRoleId, id));
        if (references != null && references > 0) {
            throw new BusinessException(IamErrorCode.ROLE_IN_USE);
        }

        roleResourceMapper.delete(Wrappers.<IamRoleResourcePO>lambdaQuery()
                .eq(IamRoleResourcePO::getRoleId, id));
        roleMapper.deleteById(id);
        cacheEvictor.evictRole(id);
    }

    public List<Long> resourceIds(long roleId) {
        requireRole(roleId);
        return roleResourceMapper.selectList(Wrappers.<IamRoleResourcePO>lambdaQuery()
                        .eq(IamRoleResourcePO::getRoleId, roleId))
                .stream()
                .map(IamRoleResourcePO::getResourceId)
                .sorted()
                .toList();
    }

    @Transactional
    public void replaceResources(long roleId, List<Long> resourceIds) {
        requireRole(roleId);
        Set<Long> uniqueIds = new LinkedHashSet<>(resourceIds);
        if (uniqueIds.contains(null)) {
            throw new BusinessException(IamErrorCode.INVALID_RESOURCE_IDS);
        }
        if (!uniqueIds.isEmpty()) {
            Long existing = resourceMapper.selectCount(Wrappers.<IamResourcePO>lambdaQuery()
                    .in(IamResourcePO::getId, uniqueIds));
            if (existing == null || existing < uniqueIds.size()) {
                throw new BusinessException(IamErrorCode.INVALID_RESOURCE_IDS);
            }
        }

        roleResourceMapper.delete(Wrappers.<IamRoleResourcePO>lambdaQuery()
                .eq(IamRoleResourcePO::getRoleId, roleId));
        for (Long resourceId : uniqueIds) {
            IamRoleResourcePO link = new IamRoleResourcePO();
            link.setRoleId(roleId);
            link.setResourceId(resourceId);
            roleResourceMapper.insert(link);
        }

        cacheEvictor.evictRole(roleId);
    }

    private IamRolePO requireRole(long id) {
        IamRolePO role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(IamErrorCode.ROLE_NOT_FOUND);
        }
        return role;
    }

    private void applySort(LambdaQueryWrapper<IamRolePO> wrapper, String sort) {
        String field = "sort";
        boolean ascending = true;

        if (StringUtils.hasText(sort)) {
            String[] parts = sort.split(",", 2);
            field = parts[0].trim();
            if (parts.length > 1) {
                String direction = parts[1].trim();
                if ("desc".equalsIgnoreCase(direction)) {
                    ascending = false;
                } else if (!"asc".equalsIgnoreCase(direction)) {
                    throw new BusinessException(CommonErrorCode.BAD_REQUEST, "排序方向不支持");
                }
            }
        }

        switch (field) {
            case "sort" -> wrapper.orderBy(true, ascending, IamRolePO::getSort);
            case "createdAt" -> wrapper.orderBy(true, ascending, IamRolePO::getCreatedAt);
            case "code" -> wrapper.orderBy(true, ascending, IamRolePO::getCode);
            default -> throw new BusinessException(CommonErrorCode.BAD_REQUEST, "排序字段不支持");
        }
        wrapper.orderByAsc(IamRolePO::getId);
    }

    private RoleSummaryView toSummary(IamRolePO role) {
        return new RoleSummaryView(
                role.getId(),
                role.getCode(),
                role.getName(),
                role.getDescription(),
                RoleStatus.valueOf(role.getStatus()),
                role.getSort(),
                role.getCreatedAt());
    }

    private RoleDetailView toDetail(IamRolePO role) {
        return new RoleDetailView(
                role.getId(),
                role.getCode(),
                role.getName(),
                role.getDescription(),
                RoleStatus.valueOf(role.getStatus()),
                role.getSort(),
                role.getCreatedAt(),
                role.getUpdatedAt(),
                role.getVersion());
    }
}
