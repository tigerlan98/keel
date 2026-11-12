package io.github.keel.iam.application.authorization;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.keel.common.core.error.CommonErrorCode;
import io.github.keel.common.core.exception.BusinessException;
import io.github.keel.iam.application.IamErrorCode;
import io.github.keel.iam.infrastructure.persistence.mapper.IamResourceMapper;
import io.github.keel.iam.infrastructure.persistence.po.IamResourcePO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 资源树查询与展示属性维护。资源新增/删除仅由 Flyway 迁移执行。
 */
@Service
public class ResourceApplicationService {

    private static final String STATUS_ENABLED = "ENABLED";
    private static final long ROOT_PARENT_ID = 0L;

    private final IamResourceMapper resourceMapper;
    private final AuthorizationCacheEvictor cacheEvictor;

    public ResourceApplicationService(IamResourceMapper resourceMapper, AuthorizationCacheEvictor cacheEvictor) {
        this.resourceMapper = resourceMapper;
        this.cacheEvictor = cacheEvictor;
    }

    public List<ResourceNodeView> tree(ResourceStatus status, ResourceType type) {
        List<IamResourcePO> resources = resourceMapper.selectList(Wrappers.<IamResourcePO>lambdaQuery()
                .orderByAsc(IamResourcePO::getSort)
                .orderByAsc(IamResourcePO::getId));

        Map<Long, ResourceNodeView> nodes = new LinkedHashMap<>();
        for (IamResourcePO resource : resources) {
            if (status != null && !status.name().equals(resource.getStatus())) {
                continue;
            }
            if (type != null && !type.name().equals(resource.getType())) {
                continue;
            }
            nodes.put(resource.getId(), toNode(resource));
        }

        List<ResourceNodeView> roots = new ArrayList<>();
        for (ResourceNodeView node : nodes.values()) {
            ResourceNodeView parent = node.parentId() == null ? null : nodes.get(node.parentId());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.children().add(node);
            }
        }
        Comparator<ResourceNodeView> order = Comparator
                .comparing((ResourceNodeView node) -> node.sort() == null ? 0 : node.sort())
                .thenComparing(ResourceNodeView::id);
        sortTree(roots, order);
        return roots;
    }

    public ResourceDetailView detail(long id) {
        return toDetail(requireResource(id));
    }

    @Transactional
    public ResourceDetailView update(long id, UpdateResourceCommand command) {
        IamResourcePO resource = requireResource(id);
        validateParent(resource, command.parentId());

        resource.setName(command.name());
        resource.setParentId(command.parentId());
        resource.setSort(command.sort());
        resource.setIcon(command.icon());
        resource.setVisible(command.visible());
        resource.setVersion(command.version());
        if (resourceMapper.updateById(resource) == 0) {
            throw new BusinessException(CommonErrorCode.OPTIMISTIC_LOCK);
        }

        cacheEvictor.evictAll();
        return toDetail(resourceMapper.selectById(id));
    }

    @Transactional
    public ResourceDetailView updateStatus(long id, ResourceStatus status, Integer version) {
        IamResourcePO resource = requireResource(id);
        if (status == ResourceStatus.DISABLED
                && STATUS_ENABLED.equals(resource.getStatus())
                && isDirectoryOrMenu(resource)) {
            Long enabledChildren = resourceMapper.selectCount(Wrappers.<IamResourcePO>lambdaQuery()
                    .eq(IamResourcePO::getParentId, id)
                    .eq(IamResourcePO::getStatus, STATUS_ENABLED));
            if (enabledChildren != null && enabledChildren > 0) {
                throw new BusinessException(IamErrorCode.RESOURCE_HAS_ENABLED_CHILDREN);
            }
        }

        resource.setStatus(status.name());
        resource.setVersion(version);
        if (resourceMapper.updateById(resource) == 0) {
            throw new BusinessException(CommonErrorCode.OPTIMISTIC_LOCK);
        }

        cacheEvictor.evictAll();
        return toDetail(resourceMapper.selectById(id));
    }

    /**
     * 父级只能是根、DIRECTORY 或 MENU；BUTTON / API 必须有父级；移动不能形成环。
     */
    private void validateParent(IamResourcePO resource, long parentId) {
        boolean canBeRoot = isDirectoryOrMenu(resource);
        if (parentId == ROOT_PARENT_ID) {
            if (!canBeRoot) {
                throw new BusinessException(IamErrorCode.RESOURCE_PARENT_INVALID);
            }
            return;
        }

        if (parentId == resource.getId()) {
            throw new BusinessException(IamErrorCode.RESOURCE_PARENT_INVALID);
        }
        IamResourcePO parent = resourceMapper.selectById(parentId);
        if (parent == null || !isDirectoryOrMenu(parent)) {
            throw new BusinessException(IamErrorCode.RESOURCE_PARENT_INVALID);
        }

        long currentId = parentId;
        int guard = 0;
        while (currentId != ROOT_PARENT_ID && guard++ < 10_000) {
            if (currentId == resource.getId()) {
                throw new BusinessException(IamErrorCode.RESOURCE_PARENT_INVALID);
            }
            IamResourcePO current = resourceMapper.selectById(currentId);
            if (current == null) {
                break;
            }
            currentId = current.getParentId() == null ? ROOT_PARENT_ID : current.getParentId();
        }
    }

    private IamResourcePO requireResource(long id) {
        IamResourcePO resource = resourceMapper.selectById(id);
        if (resource == null) {
            throw new BusinessException(IamErrorCode.RESOURCE_NOT_FOUND);
        }
        return resource;
    }

    private static boolean isDirectoryOrMenu(IamResourcePO resource) {
        return ResourceType.DIRECTORY.name().equals(resource.getType())
                || ResourceType.MENU.name().equals(resource.getType());
    }

    private static void sortTree(List<ResourceNodeView> nodes, Comparator<ResourceNodeView> order) {
        nodes.sort(order);
        for (ResourceNodeView node : nodes) {
            sortTree(node.children(), order);
        }
    }

    private ResourceNodeView toNode(IamResourcePO resource) {
        return new ResourceNodeView(
                resource.getId(),
                resource.getParentId(),
                ResourceType.valueOf(resource.getType()),
                resource.getCode(),
                resource.getName(),
                resource.getPath(),
                resource.getComponent(),
                resource.getIcon(),
                resource.getSort(),
                resource.getVisible(),
                ResourceStatus.valueOf(resource.getStatus()),
                new ArrayList<>());
    }

    private ResourceDetailView toDetail(IamResourcePO resource) {
        return new ResourceDetailView(
                resource.getId(),
                resource.getParentId(),
                ResourceType.valueOf(resource.getType()),
                resource.getCode(),
                resource.getName(),
                resource.getPath(),
                resource.getComponent(),
                resource.getIcon(),
                resource.getSort(),
                resource.getVisible(),
                ResourceStatus.valueOf(resource.getStatus()),
                resource.getCreatedAt(),
                resource.getUpdatedAt(),
                resource.getVersion());
    }
}
