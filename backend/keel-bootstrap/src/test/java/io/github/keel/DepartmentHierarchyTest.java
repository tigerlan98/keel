package io.github.keel;

import static org.assertj.core.api.Assertions.*;

import io.github.keel.common.core.exception.BusinessException;
import io.github.keel.iam.domain.organization.DepartmentHierarchy;
import io.github.keel.iam.domain.organization.DepartmentHierarchy.Entry;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.*;

class DepartmentHierarchyTest {
    private DepartmentHierarchy tree() {
        return new DepartmentHierarchy(
                List.of(
                        new Entry(1, 0, true),
                        new Entry(2, 1, true),
                        new Entry(3, 2, false),
                        new Entry(4, 3, false)));
    }

    @Test
    void subtreeIncludesDisabledDescendants() {
        assertThat(tree().subtree(2)).containsExactly(2L, 3L, 4L);
    }

    @Test
    void movingToDescendantCannotCreateCycle() {
        assertThatThrownBy(() -> tree().validatePlacement(2L, 4))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("下级");
    }

    @Test
    void disabledAncestorPreventsSelection() {
        assertThat(tree().selectable(4)).isFalse();
        assertThatThrownBy(() -> tree().validatePlacement(null, 3))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void movingWholeSubtreeEnforcesDepth() {
        var entries =
                new ArrayList<>(
                        LongStream.rangeClosed(1, 16)
                                .mapToObj(id -> new Entry(id, id - 1, true))
                                .toList());
        entries.add(new Entry(20, 1, true));
        entries.add(new Entry(21, 20, true));
        var h = new DepartmentHierarchy(entries);
        assertThatThrownBy(() -> h.validatePlacement(20L, 15))
                .isInstanceOf(BusinessException.class);
        h.validatePlacement(20L, 14);
    }

    @Test
    void capacityCannotProduceTruncatedTree() {
        assertThatThrownBy(
                        () ->
                                new DepartmentHierarchy(
                                        LongStream.rangeClosed(1, 1001)
                                                .mapToObj(
                                                        id -> new Entry(id, id == 1 ? 0 : 1, true))
                                                .toList()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void corruptCycleDoesNotLoopForever() {
        var h = new DepartmentHierarchy(List.of(new Entry(1, 2, true), new Entry(2, 1, true)));
        assertThatThrownBy(() -> h.selectable(1)).isInstanceOf(BusinessException.class);
    }
}
