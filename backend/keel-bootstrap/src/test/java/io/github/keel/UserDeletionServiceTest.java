package io.github.keel;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.keel.common.core.auth.CurrentUserProvider;
import io.github.keel.common.core.exception.BusinessException;
import io.github.keel.common.security.token.TokenService;
import io.github.keel.iam.application.authorization.AuthorizationCacheEvictor;
import io.github.keel.iam.application.identity.UserApplicationService;
import io.github.keel.iam.application.organization.DepartmentApplicationService;
import io.github.keel.iam.infrastructure.persistence.mapper.*;
import io.github.keel.iam.infrastructure.persistence.po.IamUserPO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserDeletionServiceTest {
    private final IamUserMapper users = mock(IamUserMapper.class);
    private final IamUserRoleMapper links = mock(IamUserRoleMapper.class);
    private final TokenService tokens = mock(TokenService.class);
    private final AuthorizationCacheEvictor cache = mock(AuthorizationCacheEvictor.class);
    private final CurrentUserProvider current = mock(CurrentUserProvider.class);
    private final UserApplicationService service = new UserApplicationService(
            mock(DepartmentApplicationService.class), users, mock(IamRoleMapper.class), links,
            mock(PasswordEncoder.class), tokens, cache, current);

    @BeforeEach
    void currentUser() {
        when(current.currentUserId()).thenReturn(1L);
    }

    @Test
    void refusesCurrentUserWithoutChangingAccountOrSessions() {
        when(users.lockById(1L)).thenReturn(user(1L));
        assertProtected(() -> service.delete(1L, 3));
    }

    @Test
    void refusesSuperAdminWithoutChangingAccountOrSessions() {
        when(users.lockById(2L)).thenReturn(user(2L));
        when(users.countProtectedRole(2L)).thenReturn(1);
        assertProtected(() -> service.delete(2L, 3));
    }

    @Test
    void refusesStaleVersionWithoutRevokingSessions() {
        when(users.lockById(2L)).thenReturn(user(2L));
        assertThatThrownBy(() -> service.delete(2L, 2))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> org.assertj.core.api.Assertions.assertThat(e.getErrorCode().code())
                                .isEqualTo("COMMON_OPTIMISTIC_LOCK"));
        verifyNoInteractions(links, tokens, cache);
    }

    @Test
    void missingAccountIsReportedInsteadOfDeletingAssociations() {
        assertThatThrownBy(() -> service.delete(404L, 0))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> org.assertj.core.api.Assertions.assertThat(e.getErrorCode().code())
                                .isEqualTo("IAM_USER_NOT_FOUND"));
        verifyNoInteractions(links, tokens, cache);
    }

    @Test
    void successfulDeletionInvalidatesEveryDeviceAndCachedPermissions() {
        when(users.lockById(2L)).thenReturn(user(2L));
        when(users.delete(any())).thenReturn(1);
        service.delete(2L, 3);
        verify(tokens).revokeAll(2L);
        verify(cache).evictUser(2L);
    }

    private void assertProtected(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessException.class,
                e -> org.assertj.core.api.Assertions.assertThat(e.getErrorCode().code())
                        .isEqualTo("IAM_USER_PROTECTED"));
        verifyNoInteractions(links, tokens, cache);
    }

    private IamUserPO user(long id) {
        var user = new IamUserPO();
        user.setId(id);
        user.setVersion(3);
        return user;
    }
}
