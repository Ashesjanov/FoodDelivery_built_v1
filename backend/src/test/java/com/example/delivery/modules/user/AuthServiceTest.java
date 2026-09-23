package com.example.delivery.modules.user;

import com.example.delivery.common.BizException;
import com.example.delivery.common.ErrorCode;
import com.example.delivery.domain.entity.UserAccount;
import com.example.delivery.domain.enums.UserRole;
import com.example.delivery.domain.enums.UserStatus;
import com.example.delivery.domain.mapper.UserAccountMapper;
import com.example.delivery.modules.user.dto.AuthResponse;
import com.example.delivery.modules.user.dto.LoginRequest;
import com.example.delivery.modules.user.dto.RegisterRequest;
import com.example.delivery.modules.user.service.AuthService;
import com.example.delivery.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AuthService} 注册、登录和用户查询的单元测试。
 * 通过模拟 Mapper、密码编码器和 JWT 协作组件，验证输入规范化、凭据结果、
 * 账号状态和令牌签发，不依赖数据库或安全容器。
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserAccountMapper userAccountMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @InjectMocks
    private AuthService authService;

    @Test
    void registerNormalizesInputAndCreatesActiveCustomer() {
        when(userAccountMapper.selectOne(any())).thenReturn(null);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userAccountMapper.insert(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount inserted = invocation.getArgument(0);
            inserted.setId(101L);
            return 1;
        });
        when(jwtService.generateToken(101L, "alice", "CUSTOMER")).thenReturn("signed-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.register(
                new RegisterRequest(" alice ", "password123", " 13812345678 ", "  "));

        ArgumentCaptor<UserAccount> userCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountMapper).insert(userCaptor.capture());
        UserAccount inserted = userCaptor.getValue();
        assertThat(inserted.getUsername()).isEqualTo("alice");
        assertThat(inserted.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(inserted.getPhone()).isEqualTo("13812345678");
        assertThat(inserted.getNickname()).isEqualTo("alice");
        assertThat(inserted.getRole()).isEqualTo(UserRole.CUSTOMER);
        assertThat(inserted.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(response.token()).isEqualTo("signed-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600L);
        assertThat(response.user().id()).isEqualTo(101L);
    }

    @Test
    void registerRejectsExistingUsername() {
        when(userAccountMapper.selectOne(any())).thenReturn(UserAccount.builder().id(1L).username("alice").build());

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("alice", "password123", null, null)))
                .isInstanceOfSatisfying(BizException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.USER_ALREADY_EXISTS));
        verify(userAccountMapper, never()).insert(any(UserAccount.class));
    }

    @Test
    void registerMapsConcurrentDuplicateInsertToBusinessConflict() {
        when(userAccountMapper.selectOne(any())).thenReturn(null);
        when(passwordEncoder.encode(any())).thenReturn("encoded-password");
        when(userAccountMapper.insert(any(UserAccount.class))).thenThrow(new DuplicateKeyException("duplicate"));

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("alice", "password123", null, null)))
                .isInstanceOfSatisfying(BizException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.USER_ALREADY_EXISTS));
    }

    @Test
    void loginIssuesTokenForActiveUser() {
        UserAccount user = activeUser();
        when(userAccountMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        when(jwtService.generateToken(101L, "alice", "CUSTOMER")).thenReturn("signed-token");
        when(jwtService.getExpirationSeconds()).thenReturn(7200L);

        AuthResponse response = authService.login(new LoginRequest(" alice ", "password123"));

        assertThat(response.token()).isEqualTo("signed-token");
        assertThat(response.user().username()).isEqualTo("alice");
    }

    @Test
    void loginRejectsWrongPassword() {
        when(userAccountMapper.selectOne(any())).thenReturn(activeUser());
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "wrong-password")))
                .isInstanceOfSatisfying(BizException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    @Test
    void loginRejectsUnknownUsernameWithoutComparingPassword() {
        when(userAccountMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody", "password123")))
                .isInstanceOfSatisfying(BizException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void loginRejectsDisabledAccountAfterCredentialCheck() {
        UserAccount user = UserAccount.builder()
                .id(101L).username("alice").passwordHash("encoded-password")
                .role(UserRole.CUSTOMER).status(UserStatus.DISABLED).build();
        when(userAccountMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "password123")))
                .isInstanceOfSatisfying(BizException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_DISABLED));
        verify(jwtService, never()).generateToken(any(), any(), any());
    }

    @Test
    void currentUserRejectsUnknownId() {
        when(userAccountMapper.selectById(404L)).thenReturn(null);

        assertThatThrownBy(() -> authService.currentUser(404L))
                .isInstanceOfSatisfying(BizException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    private static UserAccount activeUser() {
        return UserAccount.builder()
                .id(101L).username("alice").passwordHash("encoded-password")
                .role(UserRole.CUSTOMER).status(UserStatus.ACTIVE).build();
    }
}
