package io.github.keel.iam.domain.organization;

import io.github.keel.common.core.exception.BusinessException;
import io.github.keel.iam.application.IamErrorCode;

import java.util.*;

/** Bounded adjacency tree. No persistence or Web dependencies. */
public final class DepartmentHierarchy {
    public static final int MAX_NODES = 1000;
    public static final int MAX_DEPTH = 16;

    public record Entry(long id, long parentId, boolean enabled) {}

    private final Map<Long, Entry> entries = new LinkedHashMap<>();

    public DepartmentHierarchy(List<Entry> source) {
        if (source.size() > MAX_NODES)
            throw new BusinessException(IamErrorCode.DEPARTMENT_LIMIT_EXCEEDED);
        source.forEach(entry -> entries.put(entry.id(), entry));
    }

    public Set<Long> subtree(long id) {
        require(id);
        Set<Long> result = new LinkedHashSet<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(id);
        while (!queue.isEmpty()) {
            long current = queue.removeFirst();
            if (!result.add(current)) throw new BusinessException(IamErrorCode.DEPARTMENT_CYCLE);
            entries.values().stream()
                    .filter(e -> e.parentId() == current)
                    .forEach(e -> queue.add(e.id()));
        }
        return result;
    }

    public boolean selectable(long id) {
        Set<Long> visited = new HashSet<>();
        long current = id;
        while (current != 0) {
            if (!visited.add(current)) throw new BusinessException(IamErrorCode.DEPARTMENT_CYCLE);
            Entry entry = require(current);
            if (!entry.enabled()) return false;
            current = entry.parentId();
        }
        return true;
    }

    public void validatePlacement(Long movingId, long parentId) {
        Set<Long> moving = movingId == null ? Set.of() : subtree(movingId);
        if (moving.contains(parentId)) throw new BusinessException(IamErrorCode.DEPARTMENT_CYCLE);
        if (!selectable(parentId)) throw new BusinessException(IamErrorCode.DEPARTMENT_DISABLED);
        int parentDepth = depth(parentId);
        int height =
                movingId == null
                        ? 1
                        : moving.stream()
                                .mapToInt(id -> depth(id) - depth(movingId) + 1)
                                .max()
                                .orElse(1);
        if (parentDepth + height > MAX_DEPTH)
            throw new BusinessException(IamErrorCode.DEPARTMENT_DEPTH_EXCEEDED);
    }

    public boolean hasEnabledDescendants(long id) {
        return subtree(id).stream().anyMatch(child -> child != id && require(child).enabled());
    }

    private int depth(long id) {
        Set<Long> visited = new HashSet<>();
        long current = id;
        while (current != 0) {
            if (!visited.add(current)) throw new BusinessException(IamErrorCode.DEPARTMENT_CYCLE);
            current = require(current).parentId();
        }
        return visited.size();
    }

    private Entry require(long id) {
        Entry value = entries.get(id);
        if (value == null) throw new BusinessException(IamErrorCode.DEPARTMENT_NOT_FOUND);
        return value;
    }
}
