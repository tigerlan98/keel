package io.github.keel.iam.application.organization;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import io.github.keel.common.core.error.CommonErrorCode;
import io.github.keel.common.core.exception.BusinessException;
import io.github.keel.iam.application.IamErrorCode;
import io.github.keel.iam.domain.organization.DepartmentHierarchy;
import io.github.keel.iam.infrastructure.persistence.mapper.DepartmentUserCount;
import io.github.keel.iam.infrastructure.persistence.mapper.IamDepartmentMapper;
import io.github.keel.iam.infrastructure.persistence.po.IamDepartmentPO;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentApplicationService {
    private final IamDepartmentMapper mapper;

    public List<DepartmentNodeView> tree() {
        List<IamDepartmentPO> rows = readTree();
        Map<Long, Long> counts = counts();
        Map<Long, DepartmentNodeView> nodes = new LinkedHashMap<>();
        for (var row : rows)
            nodes.put(
                    row.getId(),
                    new DepartmentNodeView(
                            row.getId(),
                            row.getParentId(),
                            row.getCode(),
                            row.getName(),
                            row.getSort(),
                            status(row),
                            row.getVersion(),
                            counts.getOrDefault(row.getId(), 0L),
                            new ArrayList<>()));
        List<DepartmentNodeView> roots = new ArrayList<>();
        nodes.values()
                .forEach(
                        node -> {
                            if (node.parentId() == 0) roots.add(node);
                            else {
                                var parent = nodes.get(node.parentId());
                                if (parent == null)
                                    throw new BusinessException(CommonErrorCode.INTERNAL_ERROR);
                                parent.children().add(node);
                            }
                        });
        return roots;
    }

    public List<DepartmentOptionView> options() {
        List<IamDepartmentPO> rows = readTree();
        DepartmentHierarchy hierarchy = hierarchy(rows);
        Map<Long, DepartmentOptionView> nodes = new LinkedHashMap<>();
        for (var row : rows)
            nodes.put(
                    row.getId(),
                    new DepartmentOptionView(
                            row.getId(),
                            row.getParentId(),
                            row.getName(),
                            status(row),
                            hierarchy.selectable(row.getId()),
                            new ArrayList<>()));
        List<DepartmentOptionView> roots = new ArrayList<>();
        nodes.values()
                .forEach(
                        node -> {
                            if (node.parentId() == 0) roots.add(node);
                            else nodes.get(node.parentId()).children().add(node);
                        });
        return roots;
    }

    public Map<Long, DepartmentBriefView> briefs(Collection<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return mapper.selectBatchIds(ids).stream()
                .collect(
                        Collectors.toMap(
                                IamDepartmentPO::getId,
                                row ->
                                        new DepartmentBriefView(
                                                row.getId(), row.getName(), status(row))));
    }

    public Set<Long> filterIds(long id, boolean includeDescendants) {
        DepartmentHierarchy hierarchy = hierarchy(readTree());
        Set<Long> descendants = hierarchy.subtree(id);
        return includeDescendants ? descendants : Set.of(id);
    }

    public DepartmentDetailView detail(long id) {
        return view(require(id));
    }

    @Transactional
    public DepartmentDetailView create(CreateDepartmentCommand command) {
        List<IamDepartmentPO> rows = lockTree();
        if (rows.size() >= DepartmentHierarchy.MAX_NODES)
            throw new BusinessException(IamErrorCode.DEPARTMENT_LIMIT_EXCEEDED);
        hierarchy(rows).validatePlacement(null, command.parentId());
        String code = command.code().trim(), name = command.name().trim();
        if (rows.stream().anyMatch(row -> row.getCode().equalsIgnoreCase(code)))
            throw new BusinessException(IamErrorCode.DEPARTMENT_CODE_EXISTS);
        checkName(rows, null, command.parentId(), name);
        IamDepartmentPO row = new IamDepartmentPO();
        row.setParentId(command.parentId());
        row.setCode(code);
        row.setName(name);
        row.setSort(command.sort());
        row.setStatus(command.status().name());
        row.setVersion(0);
        try {
            mapper.insert(row);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(IamErrorCode.DEPARTMENT_NAME_EXISTS);
        }
        return view(require(row.getId()));
    }

    @Transactional
    public DepartmentDetailView update(long id, UpdateDepartmentCommand command) {
        List<IamDepartmentPO> rows = lockTree();
        IamDepartmentPO row = requireIn(rows, id);
        checkVersion(row, command.version());
        if (root(row)) {
            if (command.parentId() != 0)
                throw new BusinessException(IamErrorCode.DEPARTMENT_PROTECTED);
        } else if (!Objects.equals(row.getParentId(), command.parentId())) {
            if (command.parentId() == 0)
                throw new BusinessException(CommonErrorCode.VALIDATION_FAILED);
            hierarchy(rows).validatePlacement(id, command.parentId());
        }
        String name = command.name().trim();
        checkName(rows, id, command.parentId(), name);
        row.setParentId(command.parentId());
        row.setName(name);
        row.setSort(command.sort());
        try {
            if (mapper.updateById(row) == 0)
                throw new BusinessException(CommonErrorCode.OPTIMISTIC_LOCK);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(IamErrorCode.DEPARTMENT_NAME_EXISTS);
        }
        return view(require(id));
    }

    @Transactional
    public DepartmentDetailView changeStatus(long id, DepartmentStatus status, int version) {
        List<IamDepartmentPO> rows = lockTree();
        IamDepartmentPO row = requireIn(rows, id);
        checkVersion(row, version);
        if (root(row)) throw new BusinessException(IamErrorCode.DEPARTMENT_PROTECTED);
        var hierarchy = hierarchy(rows);
        if (status == DepartmentStatus.DISABLED && hierarchy.hasEnabledDescendants(id))
            throw new BusinessException(IamErrorCode.DEPARTMENT_HAS_ENABLED_DESCENDANTS);
        if (status == DepartmentStatus.ENABLED && !hierarchy.selectable(row.getParentId()))
            throw new BusinessException(IamErrorCode.DEPARTMENT_DISABLED);
        row.setStatus(status.name());
        if (mapper.updateById(row) == 0)
            throw new BusinessException(CommonErrorCode.OPTIMISTIC_LOCK);
        return view(require(id));
    }

    @Transactional
    public void delete(long id, int version) {
        List<IamDepartmentPO> rows = lockTree();
        var row = requireIn(rows, id);
        checkVersion(row, version);
        if (root(row)) throw new BusinessException(IamErrorCode.DEPARTMENT_PROTECTED);
        if (rows.stream().anyMatch(child -> child.getParentId() == id))
            throw new BusinessException(IamErrorCode.DEPARTMENT_HAS_CHILDREN);
        if (mapper.lockFirstUser(id) != null)
            throw new BusinessException(IamErrorCode.DEPARTMENT_IN_USE);
        if (mapper.delete(
                        Wrappers.<IamDepartmentPO>lambdaQuery()
                                .eq(IamDepartmentPO::getId, id)
                                .eq(IamDepartmentPO::getVersion, version))
                == 0) throw new BusinessException(CommonErrorCode.OPTIMISTIC_LOCK);
    }

    /** Caller holds the user transaction; shared ROOT lock excludes topology writes. */
    public void validateAssignment(Long departmentId, Long previousId) {
        if (departmentId == null) return;
        if (mapper.shareRoot() == null) throw new BusinessException(CommonErrorCode.INTERNAL_ERROR);
        var row = mapper.lockAssignment(departmentId);
        if (row == null) throw new BusinessException(IamErrorCode.DEPARTMENT_NOT_FOUND);
        if (!Objects.equals(previousId, departmentId)
                && !hierarchy(mapper.shareTree()).selectable(departmentId))
            throw new BusinessException(IamErrorCode.DEPARTMENT_DISABLED);
    }

    private List<IamDepartmentPO> lockTree() {
        if (mapper.lockRoot() == null) throw new BusinessException(CommonErrorCode.INTERNAL_ERROR);
        var rows = mapper.lockTree();
        hierarchy(rows);
        return rows;
    }

    private List<IamDepartmentPO> readTree() {
        var rows = mapper.tree();
        hierarchy(rows);
        return rows;
    }

    private DepartmentHierarchy hierarchy(List<IamDepartmentPO> rows) {
        return new DepartmentHierarchy(
                rows.stream()
                        .map(
                                row ->
                                        new DepartmentHierarchy.Entry(
                                                row.getId(),
                                                row.getParentId(),
                                                status(row) == DepartmentStatus.ENABLED))
                        .toList());
    }

    private Map<Long, Long> counts() {
        return mapper.userCounts().stream()
                .collect(
                        Collectors.toMap(
                                DepartmentUserCount::departmentId, DepartmentUserCount::total));
    }

    private DepartmentDetailView view(IamDepartmentPO row) {
        return new DepartmentDetailView(
                row.getId(),
                row.getParentId(),
                row.getCode(),
                row.getName(),
                row.getSort(),
                status(row),
                row.getVersion(),
                counts().getOrDefault(row.getId(), 0L),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private void checkName(List<IamDepartmentPO> rows, Long id, long parentId, String name) {
        if (rows.stream()
                .anyMatch(
                        row ->
                                !Objects.equals(row.getId(), id)
                                        && row.getParentId() == parentId
                                        && row.getName().equalsIgnoreCase(name)))
            throw new BusinessException(IamErrorCode.DEPARTMENT_NAME_EXISTS);
    }

    private IamDepartmentPO require(long id) {
        var row = mapper.selectById(id);
        if (row == null) throw new BusinessException(IamErrorCode.DEPARTMENT_NOT_FOUND);
        return row;
    }

    private IamDepartmentPO requireIn(List<IamDepartmentPO> rows, long id) {
        return rows.stream()
                .filter(row -> row.getId() == id)
                .findFirst()
                .orElseThrow(() -> new BusinessException(IamErrorCode.DEPARTMENT_NOT_FOUND));
    }

    private boolean root(IamDepartmentPO row) {
        return "ROOT".equals(row.getCode());
    }

    private DepartmentStatus status(IamDepartmentPO row) {
        return DepartmentStatus.valueOf(row.getStatus());
    }

    private void checkVersion(IamDepartmentPO row, int version) {
        if (row.getVersion() != version)
            throw new BusinessException(CommonErrorCode.OPTIMISTIC_LOCK);
    }
}
