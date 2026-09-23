package com.example.delivery.modules.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.delivery.common.BizException;
import com.example.delivery.common.ErrorCode;
import com.example.delivery.domain.entity.UserAddress;
import com.example.delivery.domain.mapper.UserAddressMapper;
import com.example.delivery.modules.user.dto.AddressRequest;
import com.example.delivery.modules.user.dto.AddressResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Transactional address ownership and default-address invariants. */
@Service
public class UserAddressService {
    private static final int MAX_ADDRESSES_PER_USER = 20;

    private final UserAddressMapper userAddressMapper;

    public UserAddressService(UserAddressMapper userAddressMapper) {
        this.userAddressMapper = userAddressMapper;
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> list(long userId) {
        return userAddressMapper.selectList(Wrappers.<UserAddress>lambdaQuery()
                        .eq(UserAddress::getUserId, userId)
                        .orderByDesc(UserAddress::getIsDefault)
                        .orderByDesc(UserAddress::getUpdatedAt)
                        .orderByDesc(UserAddress::getId)).stream()
                .map(AddressResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AddressResponse get(long userId, long addressId) {
        return AddressResponse.from(requireOwned(userId, addressId));
    }

    @Transactional
    public AddressResponse create(long userId, AddressRequest request) {
        long count = countForUser(userId);
        if (count >= MAX_ADDRESSES_PER_USER) {
            throw new BizException(ErrorCode.CONFLICT, "a user may store at most " + MAX_ADDRESSES_PER_USER + " addresses");
        }

        boolean defaultAddress = request.isDefault() == null ? count == 0 : request.isDefault();
        if (defaultAddress && count > 0) {
            clearDefault(userId);
        }
        LocalDateTime now = LocalDateTime.now();
        UserAddress address = new UserAddress();
        address.setUserId(userId);
        apply(address, request);
        address.setIsDefault(defaultAddress);
        address.setCreatedAt(now);
        address.setUpdatedAt(now);
        userAddressMapper.insert(address);
        return AddressResponse.from(address);
    }

    @Transactional
    public AddressResponse update(long userId, long addressId, AddressRequest request) {
        UserAddress address = requireOwned(userId, addressId);
        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
        boolean defaultAddress = request.isDefault() == null ? wasDefault : request.isDefault();
        if (!defaultAddress && wasDefault && countForUser(userId) > 1) {
            throw new BizException(ErrorCode.BAD_REQUEST, "set another address as default before clearing this one");
        }
        if (defaultAddress && !wasDefault) {
            clearDefault(userId);
        }
        apply(address, request);
        address.setIsDefault(defaultAddress);
        address.setUpdatedAt(LocalDateTime.now());
        userAddressMapper.updateById(address);
        return AddressResponse.from(address);
    }

    @Transactional
    public void delete(long userId, long addressId) {
        UserAddress address = requireOwned(userId, addressId);
        userAddressMapper.deleteById(address.getId());
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            promoteOldestDefault(userId);
        }
    }

    @Transactional
    public AddressResponse setDefault(long userId, long addressId) {
        UserAddress address = requireOwned(userId, addressId);
        clearDefault(userId);
        address.setIsDefault(true);
        address.setUpdatedAt(LocalDateTime.now());
        userAddressMapper.updateById(address);
        return AddressResponse.from(address);
    }

    private UserAddress requireOwned(long userId, long addressId) {
        UserAddress address = userAddressMapper.selectOne(Wrappers.<UserAddress>lambdaQuery()
                .eq(UserAddress::getId, addressId)
                .eq(UserAddress::getUserId, userId)
                .last("LIMIT 1"));
        if (address == null) {
            throw new BizException(ErrorCode.ADDRESS_NOT_FOUND);
        }
        return address;
    }

    private long countForUser(long userId) {
        Long count = userAddressMapper.selectCount(Wrappers.<UserAddress>lambdaQuery().eq(UserAddress::getUserId, userId));
        return count == null ? 0 : count;
    }

    private void clearDefault(long userId) {
        userAddressMapper.update(null, Wrappers.<UserAddress>update()
                .set("is_default", false)
                .eq("user_id", userId)
                .eq("is_default", true));
    }

    private void promoteOldestDefault(long userId) {
        UserAddress next = userAddressMapper.selectOne(Wrappers.<UserAddress>lambdaQuery()
                .eq(UserAddress::getUserId, userId)
                .orderByAsc(UserAddress::getCreatedAt)
                .orderByAsc(UserAddress::getId)
                .last("LIMIT 1"));
        if (next != null) {
            next.setIsDefault(true);
            next.setUpdatedAt(LocalDateTime.now());
            userAddressMapper.updateById(next);
        }
    }

    private static void apply(UserAddress address, AddressRequest request) {
        address.setContactName(request.contactName().trim());
        address.setPhone(request.phone().trim());
        address.setProvince(request.province().trim());
        address.setCity(request.city().trim());
        address.setDistrict(request.district() == null ? null : request.district().trim());
        address.setDetail(request.detail().trim());
        address.setLongitude(request.longitude());
        address.setLatitude(request.latitude());
    }
}
