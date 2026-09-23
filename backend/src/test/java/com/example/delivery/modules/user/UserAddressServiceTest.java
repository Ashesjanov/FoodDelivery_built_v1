package com.example.delivery.modules.user;

import com.example.delivery.common.BizException;
import com.example.delivery.common.ErrorCode;
import com.example.delivery.domain.entity.UserAddress;
import com.example.delivery.domain.mapper.UserAddressMapper;
import com.example.delivery.modules.user.dto.AddressRequest;
import com.example.delivery.modules.user.dto.AddressResponse;
import com.example.delivery.modules.user.service.UserAddressService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link UserAddressService} 所有权、地址数量上限和默认地址流转的单元测试。
 * 通过模拟 Mapper 断言数据库调用顺序和落库状态，不依赖事务基础设施。
 */
@ExtendWith(MockitoExtension.class)
class UserAddressServiceTest {
    @Mock
    private UserAddressMapper userAddressMapper;
    @InjectMocks
    private UserAddressService userAddressService;

    @Test
    void firstCreatedAddressBecomesDefault() {
        when(userAddressMapper.selectCount(any())).thenReturn(0L);
        when(userAddressMapper.insert(any(UserAddress.class))).thenAnswer(invocation -> {
            UserAddress inserted = invocation.getArgument(0);
            inserted.setId(11L);
            return 1;
        });

        AddressResponse response = userAddressService.create(7L, request(null));

        ArgumentCaptor<UserAddress> captor = ArgumentCaptor.forClass(UserAddress.class);
        verify(userAddressMapper).insert(captor.capture());
        UserAddress inserted = captor.getValue();
        assertThat(inserted.getUserId()).isEqualTo(7L);
        assertThat(inserted.getContactName()).isEqualTo("张三");
        assertThat(inserted.getIsDefault()).isTrue();
        assertThat(response.id()).isEqualTo(11L);
        assertThat(response.isDefault()).isTrue();
        verify(userAddressMapper, never()).update(any(), any());
    }

    @Test
    void creatingAnotherDefaultClearsPreviousDefault() {
        when(userAddressMapper.selectCount(any())).thenReturn(1L);
        when(userAddressMapper.insert(any(UserAddress.class))).thenAnswer(invocation -> {
            UserAddress inserted = invocation.getArgument(0);
            inserted.setId(12L);
            return 1;
        });

        userAddressService.create(7L, request(true));

        verify(userAddressMapper).update(isNull(), any());
    }

    @Test
    void addressLimitIsEnforcedBeforeInsert() {
        when(userAddressMapper.selectCount(any())).thenReturn(20L);

        assertThatThrownBy(() -> userAddressService.create(7L, request(false)))
                .isInstanceOfSatisfying(BizException.class, error -> {
                    assertThat(error.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                    assertThat(error.getErrorMessage()).contains("20 addresses");
                });
        verify(userAddressMapper, never()).insert(any(UserAddress.class));
    }

    @Test
    void onlyDefaultAddressCannotBeDemotedWhileOtherAddressesExist() {
        UserAddress address = address(11L, true);
        when(userAddressMapper.selectOne(any())).thenReturn(address);
        when(userAddressMapper.selectCount(any())).thenReturn(2L);

        assertThatThrownBy(() -> userAddressService.update(7L, 11L, request(false)))
                .isInstanceOfSatisfying(BizException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
        verify(userAddressMapper, never()).updateById(any(UserAddress.class));
    }

    @Test
    void updateCanPromoteAnAddressToDefault() {
        UserAddress address = address(11L, false);
        when(userAddressMapper.selectOne(any())).thenReturn(address);

        AddressResponse response = userAddressService.update(7L, 11L, request(true));

        verify(userAddressMapper).update(isNull(), any());
        verify(userAddressMapper).updateById(address);
        assertThat(response.isDefault()).isTrue();
        assertThat(response.detail()).isEqualTo("人民路 1 号");
    }

    @Test
    void foreignOrMissingAddressIsNotFound() {
        when(userAddressMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> userAddressService.get(7L, 99L))
                .isInstanceOfSatisfying(BizException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.ADDRESS_NOT_FOUND));
    }

    @Test
    void deletingDefaultPromotesOldestRemainingAddress() {
        UserAddress currentDefault = address(11L, true);
        UserAddress oldest = address(12L, false);
        when(userAddressMapper.selectOne(any())).thenReturn(currentDefault, oldest);

        userAddressService.delete(7L, 11L);

        verify(userAddressMapper).deleteById(11L);
        ArgumentCaptor<UserAddress> captor = ArgumentCaptor.forClass(UserAddress.class);
        verify(userAddressMapper).updateById(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(12L);
        assertThat(captor.getValue().getIsDefault()).isTrue();
    }

    @Test
    void deletingNonDefaultDoesNotChangeDefaults() {
        UserAddress address = address(11L, false);
        when(userAddressMapper.selectOne(any())).thenReturn(address);

        userAddressService.delete(7L, 11L);

        verify(userAddressMapper).deleteById(11L);
        verify(userAddressMapper, never()).updateById(any(UserAddress.class));
    }

    @Test
    void setDefaultClearsEarlierDefaultAndPersistsSelection() {
        UserAddress address = address(12L, false);
        when(userAddressMapper.selectOne(any())).thenReturn(address);

        AddressResponse response = userAddressService.setDefault(7L, 12L);

        verify(userAddressMapper).update(isNull(), any());
        verify(userAddressMapper).updateById(address);
        assertThat(response.isDefault()).isTrue();
    }

    @Test
    void listReturnsStoredAddressesInMapperOrder() {
        when(userAddressMapper.selectList(any())).thenReturn(List.of(address(12L, true), address(11L, false)));

        List<AddressResponse> addresses = userAddressService.list(7L);

        assertThat(addresses).extracting(AddressResponse::id).containsExactly(12L, 11L);
    }

    private static AddressRequest request(Boolean isDefault) {
        return new AddressRequest("张三", "13812345678", "上海市", "上海市", "浦东新区",
                "人民路 1 号", new BigDecimal("121.500000"), new BigDecimal("31.200000"), isDefault);
    }

    private static UserAddress address(Long id, boolean isDefault) {
        return UserAddress.builder()
                .id(id).userId(7L).contactName("李四").phone("13912345678")
                .province("北京市").city("北京市").district("朝阳区").detail("旧地址")
                .longitude(new BigDecimal("116.400000")).latitude(new BigDecimal("39.900000"))
                .isDefault(isDefault).createdAt(LocalDateTime.now().minusDays(2)).updatedAt(LocalDateTime.now().minusDays(1))
                .build();
    }
}
