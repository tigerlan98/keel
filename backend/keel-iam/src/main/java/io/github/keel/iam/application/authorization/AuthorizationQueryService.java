package io.github.keel.iam.application.authorization;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.keel.iam.infrastructure.persistence.mapper.IamResourceMapper;
import io.github.keel.iam.infrastructure.persistence.po.IamResourcePO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 当前用户的权限码与菜单树查询。
 */
@Service
public class AuthorizationQueryService {

    private static final String STATUS_ENABLED = "ENABLED";
    private static final String TYPE_DIRECTORY = "DIRECTORY";
    private static final String TYPE_MENU = "MENU";

    private final IamResourceMapper resourceMapper;
    private final IamPermissionProvider permissionProvider;

    public AuthorizationQueryService(IamResourceMapper resourceMapper, IamPermissionProvider permissionProvider) {
        this.resourceMapper = resourceMapper;
        this.permissionProvider = permissionProvider;
    }

    /**
     * 超级管理员的权限码集合为空，鉴权时直接绕过；普通用户返回授权码。
     */
    public List<String> permissions(long userId, boolean superAdmin) {
        if (superAdmin) {
            return List.of();
        }
        return permissionProvider.permissionsOf(userId).stream().sorted().toList();
    }

    /**
     * 超级管理员返回全部启用且可见的目录/菜单；普通用户只返回被授权资源及其祖先链。
     */
    public List<MenuView> menus(long userId, boolean superAdmin) {
        List<IamResourcePO> resources = resourceMapper.selectList(Wrappers.<IamResourcePO>lambdaQuery()
                .in(IamResourcePO::getType, TYPE_DIRECTORY, TYPE_MENU)
                .orderByAsc(IamResourcePO::getSort)
                .orderByAsc(IamResourcePO::getId));

        Map<Long, IamResourcePO> resourceMap = new HashMap<>();
        for (IamResourcePO resource : resources) {
            resourceMap.put(resource.getId(), resource);
        }

        Set<Long> included = superAdmin
                ? new HashSet<>(resourceMap.keySet())
                : resolveIncludedResourceIds(userId, resourceMap);

        Map<Long, MenuView> nodes = new LinkedHashMap<>();
        for (IamResourcePO resource : resources) {
            if (!included.contains(resource.getId()) || !isRenderable(resource)) {
                continue;
            }
            nodes.put(resource.getId(), new MenuView(
                    resource.getId(),
                    resource.getParentId(),
                    resource.getType(),
                    resource.getCode(),
                    resource.getName(),
                    resource.getPath(),
                    resource.getComponent(),
                    resource.getIcon(),
                    resource.getSort(),
                    resource.getVisible(),
                    new ArrayList<>()));
        }

        List<MenuView> roots = new ArrayList<>();
        for (MenuView node : nodes.values()) {
            MenuView parent = node.parentId() == null ? null : nodes.get(node.parentId());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.children().add(node);
            }
        }
        Comparator<MenuView> order = Comparator
                .comparing((MenuView menu) -> menu.sort() == null ? 0 : menu.sort())
                .thenComparing(MenuView::id);
        sortTree(roots, order);
        return roots;
    }

    /**
     * 授权资源（含按钮）沿父链向上标记目录/菜单，确保菜单树包含必要祖先。
     */
    private Set<Long> resolveIncludedResourceIds(long userId, Map<Long, IamResourcePO> resourceMap) {
        Set<Long> included = new HashSet<>();
        for (Long grantedId : resourceMapper.selectGrantedResourceIds(userId)) {
            Long currentId = grantedId;
            while (currentId != null && currentId != 0L) {
                IamResourcePO current = resourceMap.get(currentId);
                if (current == null) {
                    break;
                }
                if (isDirectoryOrMenu(current) && isRenderable(current)) {
                    included.add(current.getId());
                }
                currentId = current.getParentId();
            }
        }
        return included;
    }

    private static void sortTree(List<MenuView> nodes, Comparator<MenuView> order) {
        nodes.sort(order);
        for (MenuView node : nodes) {
            sortTree(node.children(), order);
        }
    }

    private static boolean isRenderable(IamResourcePO resource) {
        return STATUS_ENABLED.equals(resource.getStatus()) && Boolean.TRUE.equals(resource.getVisible());
    }

    private static boolean isDirectoryOrMenu(IamResourcePO resource) {
        return TYPE_DIRECTORY.equals(resource.getType()) || TYPE_MENU.equals(resource.getType());
    }
}
