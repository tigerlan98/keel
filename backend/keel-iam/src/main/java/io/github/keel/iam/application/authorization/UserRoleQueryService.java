package io.github.keel.iam.application.authorization;

import io.github.keel.iam.infrastructure.persistence.mapper.IamRoleMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserRoleQueryService {
    private final IamRoleMapper mapper;

    public Map<Long, List<RoleBriefView>> rolesOf(List<Long> ids) {
        Map<Long, List<RoleBriefView>> result = new HashMap<>();
        if (!ids.isEmpty())
            mapper.rolesByUsers(ids)
                    .forEach(
                            row ->
                                    result.computeIfAbsent(
                                                    row.userId(), ignored -> new ArrayList<>())
                                            .add(
                                                    new RoleBriefView(
                                                            row.id(),
                                                            row.code(),
                                                            row.name(),
                                                            RoleStatus.valueOf(row.status()))));
        return result;
    }
}
