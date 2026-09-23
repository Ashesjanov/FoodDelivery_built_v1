package com.example.delivery.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.delivery.domain.enums.UserRole;
import com.example.delivery.domain.enums.UserStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_account")
public class UserAccount {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String phone;
    private String email;
    private String passwordHash;
    private String nickname;
    private String avatarUrl;
    private UserRole role;
    private UserStatus status;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
