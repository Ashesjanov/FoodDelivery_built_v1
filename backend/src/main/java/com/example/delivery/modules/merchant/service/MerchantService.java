package com.example.delivery.modules.merchant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.delivery.common.BizException;
import com.example.delivery.common.ErrorCode;
import com.example.delivery.common.PageResponse;
import com.example.delivery.domain.entity.Merchant;
import com.example.delivery.domain.entity.MerchantCategory;
import com.example.delivery.domain.entity.MerchantCategoryRel;
import com.example.delivery.domain.enums.MerchantBusinessStatus;
import com.example.delivery.domain.mapper.MerchantCategoryMapper;
import com.example.delivery.domain.mapper.MerchantCategoryRelMapper;
import com.example.delivery.domain.mapper.MerchantMapper;
import com.example.delivery.modules.merchant.dto.MerchantDto;
import com.example.delivery.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 提供商家发现查询和商家分类、归属管理。
 * 依赖商家、分类和关联关系 Mapper；目录及关联写操作使用事务，
 * 商家写入由 {@link #requireOwnedMerchant(Long)} 鉴权，分类维护仅限管理员。
 */
@Service
public class MerchantService {
    private final MerchantMapper merchantMapper;
    private final MerchantCategoryMapper categoryMapper;
    private final MerchantCategoryRelMapper categoryRelMapper;

    public MerchantService(MerchantMapper merchantMapper, MerchantCategoryMapper categoryMapper,
                           MerchantCategoryRelMapper categoryRelMapper) {
        this.merchantMapper = merchantMapper;
        this.categoryMapper = categoryMapper;
        this.categoryRelMapper = categoryRelMapper;
    }

    public PageResponse<MerchantDto.MerchantView> page(int page, int size, Long categoryId, String keyword, boolean openOnly) {
        Page<Merchant> result = merchantMapper.selectPage(new Page<>(Math.max(page, 1), Math.min(size, 200)),
                new LambdaQueryWrapper<Merchant>()
                        .eq(categoryId != null, Merchant::getId, categoryIds(categoryId))
                        .and(keyword != null && !keyword.isBlank(), q -> q.like(Merchant::getName, keyword.trim())
                                .or().like(Merchant::getAddress, keyword.trim()))
                        .eq(openOnly, Merchant::getBusinessStatus, MerchantBusinessStatus.OPEN)
                        .orderByDesc(Merchant::getRating).orderByDesc(Merchant::getMonthlySales));
        List<MerchantDto.MerchantView> views = result.getRecords().stream().map(MerchantDto.MerchantView::from).toList();
        return PageResponse.of(views, result.getTotal(), Math.max(page, 1), Math.min(size, 200));
    }

    private List<Long> categoryIds(Long categoryId) {
        return categoryRelMapper.selectList(new LambdaQueryWrapper<MerchantCategoryRel>()
                .eq(MerchantCategoryRel::getCategoryId, categoryId)).stream().map(MerchantCategoryRel::getMerchantId).toList();
    }

    public MerchantDto.MerchantDetailView detail(Long id) {
        Merchant merchant = requireMerchant(id);
        List<Long> categories = categoryRelMapper.selectList(new LambdaQueryWrapper<MerchantCategoryRel>()
                        .eq(MerchantCategoryRel::getMerchantId, id).orderByAsc(MerchantCategoryRel::getSortOrder)).stream()
                .map(MerchantCategoryRel::getCategoryId).toList();
        return new MerchantDto.MerchantDetailView(MerchantDto.MerchantView.from(merchant), categories);
    }

    @Transactional
    public MerchantDto.MerchantView create(MerchantDto.SaveRequest request) {
        Merchant merchant = new Merchant();
        merchant.setOwnerId(SecurityUtils.requireCurrentUserId());
        merchant.setName(request.name());
        // 新商家初始为 PREPARING，后续仅所有者或管理员可切换营业状态。
        merchant.setBusinessStatus(MerchantBusinessStatus.PREPARING);
        merchant.setRating(BigDecimal.ZERO);
        merchant.setMonthlySales(0);
        apply(merchant, request);
        merchantMapper.insert(merchant);
        replaceCategories(merchant.getId(), request.categoryIds());
        return MerchantDto.MerchantView.from(merchant);
    }

    @Transactional
    public MerchantDto.MerchantView update(Long id, MerchantDto.SaveRequest request) {
        Merchant merchant = requireOwnedMerchant(id);
        apply(merchant, request);
        merchantMapper.updateById(merchant);
        replaceCategories(id, request.categoryIds());
        return MerchantDto.MerchantView.from(merchant);
    }

    public MerchantDto.MerchantView changeStatus(Long id, MerchantBusinessStatus status) {
        Merchant merchant = requireOwnedMerchant(id);
        merchant.setBusinessStatus(status);
        merchant.setUpdatedAt(LocalDateTime.now());
        merchantMapper.updateById(merchant);
        return MerchantDto.MerchantView.from(merchant);
    }

    public List<MerchantDto.CategoryView> categories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<MerchantCategory>()
                        .eq(MerchantCategory::getEnabled, true).orderByAsc(MerchantCategory::getSortOrder)).stream()
                .map(c -> new MerchantDto.CategoryView(c.getId(), c.getName(), c.getIconUrl(), c.getSortOrder(), c.getEnabled())).toList();
    }

    @Transactional
    public MerchantDto.CategoryView createCategory(MerchantDto.CategoryRequest request) {
        requireAdmin();
        MerchantCategory category = new MerchantCategory();
        category.setName(request.name());
        category.setIconUrl(request.iconUrl());
        category.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        category.setEnabled(request.enabled() == null || request.enabled());
        categoryMapper.insert(category);
        return new MerchantDto.CategoryView(category.getId(), category.getName(), category.getIconUrl(), category.getSortOrder(), category.getEnabled());
    }

    @Transactional
    public MerchantDto.CategoryView updateCategory(Long id, MerchantDto.CategoryRequest request) {
        requireAdmin();
        MerchantCategory category = categoryMapper.selectById(id);
        if (category == null) throw new BizException(ErrorCode.NOT_FOUND);
        category.setName(request.name());
        category.setIconUrl(request.iconUrl());
        if (request.sortOrder() != null) category.setSortOrder(request.sortOrder());
        if (request.enabled() != null) category.setEnabled(request.enabled());
        categoryMapper.updateById(category);
        return new MerchantDto.CategoryView(category.getId(), category.getName(), category.getIconUrl(), category.getSortOrder(), category.getEnabled());
    }

    @Transactional
    public void deleteCategory(Long id) {
        requireAdmin();
        categoryMapper.deleteById(id);
        categoryRelMapper.delete(new LambdaQueryWrapper<MerchantCategoryRel>().eq(MerchantCategoryRel::getCategoryId, id));
    }

    @Transactional
    public void addCategory(Long merchantId, Long categoryId) {
        requireOwnedMerchant(merchantId);
        if (categoryMapper.selectById(categoryId) == null) throw new BizException(ErrorCode.NOT_FOUND);
        Long count = categoryRelMapper.selectCount(new LambdaQueryWrapper<MerchantCategoryRel>()
                .eq(MerchantCategoryRel::getMerchantId, merchantId).eq(MerchantCategoryRel::getCategoryId, categoryId));
        if (count == 0) {
            MerchantCategoryRel rel = new MerchantCategoryRel();
            rel.setMerchantId(merchantId);
            rel.setCategoryId(categoryId);
            rel.setSortOrder(0);
            categoryRelMapper.insert(rel);
        }
    }

    @Transactional
    public void removeCategory(Long merchantId, Long categoryId) {
        requireOwnedMerchant(merchantId);
        categoryRelMapper.delete(new LambdaQueryWrapper<MerchantCategoryRel>()
                .eq(MerchantCategoryRel::getMerchantId, merchantId).eq(MerchantCategoryRel::getCategoryId, categoryId));
    }

    public Merchant requireMerchant(Long id) {
        Merchant merchant = merchantMapper.selectById(id);
        if (merchant == null) throw new BizException(ErrorCode.NOT_FOUND);
        return merchant;
    }

    public Merchant requireOwnedMerchant(Long id) {
        Merchant merchant = requireMerchant(id);
        // 这是菜品和分类关系写操作共用的所有权、管理员鉴权入口。
        if (!SecurityUtils.hasRole("ADMIN") && !Objects.equals(merchant.getOwnerId(), SecurityUtils.requireCurrentUserId())) {
            throw new BizException(ErrorCode.ACCESS_DENIED);
        }
        return merchant;
    }

    private void requireAdmin() {
        if (!SecurityUtils.hasRole("ADMIN")) throw new BizException(ErrorCode.ACCESS_DENIED);
    }

    private void apply(Merchant merchant, MerchantDto.SaveRequest request) {
        merchant.setName(request.name());
        merchant.setDescription(request.description());
        merchant.setLogoUrl(request.logoUrl());
        merchant.setContactName(request.contactName());
        merchant.setContactPhone(request.contactPhone());
        merchant.setProvince(request.province());
        merchant.setCity(request.city());
        merchant.setDistrict(request.district());
        merchant.setAddress(request.address());
        merchant.setLongitude(request.longitude());
        merchant.setLatitude(request.latitude());
        merchant.setBusinessHours(request.businessHours());
        merchant.setMinOrderAmount(nz(request.minOrderAmount()));
        merchant.setDeliveryFee(nz(request.deliveryFee()));
        merchant.setPackagingFee(nz(request.packagingFee()));
    }

    private void replaceCategories(Long merchantId, List<Long> categoryIds) {
        // 在同一事务中整体替换关联，避免详情接口返回不完整的分类列表。
        categoryRelMapper.delete(new LambdaQueryWrapper<MerchantCategoryRel>().eq(MerchantCategoryRel::getMerchantId, merchantId));
        if (categoryIds == null) return;
        int sort = 0;
        for (Long categoryId : categoryIds.stream().filter(Objects::nonNull).distinct().toList()) {
            if (categoryMapper.selectById(categoryId) == null) continue;
            MerchantCategoryRel rel = new MerchantCategoryRel();
            rel.setMerchantId(merchantId);
            rel.setCategoryId(categoryId);
            rel.setSortOrder(sort++);
            categoryRelMapper.insert(rel);
        }
    }

    private BigDecimal nz(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
