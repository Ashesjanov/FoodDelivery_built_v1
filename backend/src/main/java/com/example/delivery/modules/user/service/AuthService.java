package com.example.delivery.modules.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.delivery.common.BizException;
import com.example.delivery.common.ErrorCode;
import com.example.delivery.domain.entity.UserAccount;
import com.example.delivery.domain.enums.UserRole;
import com.example.delivery.domain.enums.UserStatus;
import com.example.delivery.domain.mapper.UserAccountMapper;
import com.example.delivery.modules.user.dto.AuthResponse;
import com.example.delivery.modules.user.dto.LoginRequest;
import com.example.delivery.modules.user.dto.RegisterRequest;
import com.example.delivery.modules.user.dto.UserResponse;
import com.example.delivery.security.JwtService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 负责账号注册、凭据认证和当前用户查询。
 * 依赖 {@link UserAccountMapper}、{@link PasswordEncoder} 和 {@link JwtService}；
 * 注册使用事务，登录和当前用户查询为只读事务，用户名并发冲突统一转换为业务异常。
 */
@Service
public class AuthService {
    private final UserAccountMapper userAccountMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserAccountMapper userAccountMapper, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userAccountMapper = userAccountMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.username().trim();
        if (findByUsername(username) != null) {
            throw new BizException(ErrorCode.USER_ALREADY_EXISTS);
        }

        LocalDateTime now = LocalDateTime.now();
        UserAccount user = new UserAccount();
        // 自助注册默认创建启用状态的普通用户，特权角色只能由管理员后续授予。
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPhone(normalize(request.phone()));
        user.setNickname(isBlank(request.nickname()) ? username : request.nickname().trim());
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        try {
            userAccountMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            throw new BizException(ErrorCode.USER_ALREADY_EXISTS);
        }
        return issueToken(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        UserAccount user = findByUsername(request.username().trim());
        // 对外统一返回无效凭据，避免泄露用户名是否存在或密码是否错误。
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (!UserStatus.ACTIVE.equals(user.getStatus())) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }
        return issueToken(user);
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(Long userId) {
        return UserResponse.from(requireUser(userId));
    }

    public UserAccount requireUser(Long userId) {
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private AuthResponse issueToken(UserAccount user) {
        String token = jwtService.generateToken(user.getId(), user.getUsername(), user.getRole().name());
        return AuthResponse.bearer(token, jwtService.getExpirationSeconds(), UserResponse.from(user));
    }

    private UserAccount findByUsername(String username) {
        return userAccountMapper.selectOne(Wrappers.<UserAccount>lambdaQuery()
                .eq(UserAccount::getUsername, username)
                .last("LIMIT 1"));
    }

    private static String normalize(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
