package com.example.delivery.modules.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.delivery.common.BizException;
import com.example.delivery.common.ErrorCode;
import com.example.delivery.common.PageResponse;
import com.example.delivery.domain.entity.UserAccount;
import com.example.delivery.domain.entity.UserAddress;
import com.example.delivery.domain.mapper.UserAccountMapper;
import com.example.delivery.domain.mapper.UserAddressMapper;
import com.example.delivery.modules.user.dto.AdminUserUpdateRequest;
import com.example.delivery.modules.user.dto.PasswordChangeRequest;
import com.example.delivery.modules.user.dto.UserProfileUpdateRequest;
import com.example.delivery.modules.user.dto.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** User profile and administrative account operations. */
@Service
public class UserService {
    private final UserAccountMapper userAccountMapper;
    private final UserAddressMapper userAddressMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserAccountMapper userAccountMapper, UserAddressMapper userAddressMapper,
                       PasswordEncoder passwordEncoder) {
        this.userAccountMapper = userAccountMapper;
        this.userAddressMapper = userAddressMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponse get(long userId) {
        return UserResponse.from(require(userId));
    }

    @Transactional
    public UserResponse updateProfile(long userId, UserProfileUpdateRequest request) {
        UserAccount user = require(userId);
        if (request.phone() != null) {
            user.setPhone(request.phone().trim());
        }
        if (request.nickname() != null && !request.nickname().isBlank()) {
            user.setNickname(request.nickname().trim());
        }
        user.setUpdatedAt(LocalDateTime.now());
        userAccountMapper.updateById(user);
        return UserResponse.from(user);
    }

    @Transactional
    public void changePassword(long userId, PasswordChangeRequest request) {
        UserAccount user = require(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.INVALID_CREDENTIALS, "current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "new password must differ from the current password");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userAccountMapper.updateById(user);
    }

    @Transactional
    public void deleteOwnAccount(long userId) {
        UserAccount user = require(userId);
        userAddressMapper.delete(Wrappers.<UserAddress>lambdaQuery().eq(UserAddress::getUserId, user.getId()));
        userAccountMapper.deleteById(user.getId());
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(int page, int size) {
        int current = Math.max(page, 1);
        int pageSize = Math.min(Math.max(size, 1), 200);
        LambdaQueryWrapper<UserAccount> query = Wrappers.<UserAccount>lambdaQuery()
                .orderByDesc(UserAccount::getCreatedAt)
                .orderByDesc(UserAccount::getId);
        Page<UserAccount> result = userAccountMapper.selectPage(new Page<>(current, pageSize), query);
        List<UserResponse> users = result.getRecords().stream().map(UserResponse::from).toList();
        return PageResponse.of(users, result.getTotal(), current, pageSize);
    }

    @Transactional
    public UserResponse adminUpdate(long userId, AdminUserUpdateRequest request) {
        UserAccount user = require(userId);
        if (request.phone() != null) {
            user.setPhone(request.phone().trim());
        }
        if (request.nickname() != null && !request.nickname().isBlank()) {
            user.setNickname(request.nickname().trim());
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.status() != null) {
            user.setStatus(request.status());
        }
        user.setUpdatedAt(LocalDateTime.now());
        userAccountMapper.updateById(user);
        return UserResponse.from(user);
    }

    @Transactional
    public void adminDelete(long actorId, long userId) {
        if (actorId == userId) {
            throw new BizException(ErrorCode.BAD_REQUEST, "administrators cannot delete their own account");
        }
        UserAccount user = require(userId);
        userAddressMapper.delete(Wrappers.<UserAddress>lambdaQuery().eq(UserAddress::getUserId, user.getId()));
        userAccountMapper.deleteById(user.getId());
    }

    private UserAccount require(long userId) {
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }
}
