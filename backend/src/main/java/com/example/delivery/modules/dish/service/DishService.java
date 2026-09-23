package com.example.delivery.modules.dish.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.delivery.common.BizException;
import com.example.delivery.common.ErrorCode;
import com.example.delivery.common.PageResponse;
import com.example.delivery.domain.entity.Dish;
import com.example.delivery.domain.entity.DishSpec;
import com.example.delivery.domain.enums.DishStatus;
import com.example.delivery.domain.mapper.DishMapper;
import com.example.delivery.domain.mapper.DishSpecMapper;
import com.example.delivery.modules.dish.dto.DishDto;
import com.example.delivery.modules.merchant.service.MerchantService;
import com.example.delivery.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class DishService {
    private final DishMapper dishMapper;
    private final DishSpecMapper specMapper;
    private final MerchantService merchantService;
    public DishService(DishMapper dishMapper, DishSpecMapper specMapper, MerchantService merchantService) {
        this.dishMapper = dishMapper; this.specMapper = specMapper; this.merchantService = merchantService;
    }

    public PageResponse<DishDto.DishView> page(Long merchantId, Long categoryId, String keyword, Boolean onSaleOnly,
                                                int page, int size) {
        Page<Dish> result = dishMapper.selectPage(new Page<>(Math.max(page, 1), Math.min(size, 200)),
                new LambdaQueryWrapper<Dish>().eq(Dish::getMerchantId, merchantId)
                        .eq(categoryId != null, Dish::getCategoryId, categoryId)
                        .like(keyword != null && !keyword.isBlank(), Dish::getName, keyword.trim())
                        .eq(onSaleOnly == null || onSaleOnly, Dish::getStatus, DishStatus.ON_SALE)
                        .ne(Dish::getStatus, DishStatus.DELETED).orderByAsc(Dish::getSortOrder).orderByDesc(Dish::getSales));
        return PageResponse.of(result.getRecords().stream().map(DishDto.DishView::from).toList(), result.getTotal(),
                Math.max(page, 1), Math.min(size, 200));
    }

    public DishDto.DishDetailView detail(Long id) {
        Dish dish = requireDish(id);
        if (dish.getStatus() == DishStatus.DELETED) throw new BizException(ErrorCode.NOT_FOUND);
        return new DishDto.DishDetailView(DishDto.DishView.from(dish), specs(id));
    }

    public List<DishDto.SpecView> specs(Long dishId) {
        return specMapper.selectList(new LambdaQueryWrapper<DishSpec>().eq(DishSpec::getDishId, dishId)
                .orderByAsc(DishSpec::getSortOrder)).stream().map(DishService::specView).toList();
    }

    @Transactional
    public DishDto.DishView create(Long merchantId, DishDto.SaveRequest request) {
        merchantService.requireOwnedMerchant(merchantId);
        Dish dish = new Dish();
        dish.setMerchantId(merchantId); dish.setName(request.name()); dish.setPrice(request.price());
        dish.setOriginalPrice(request.originalPrice() == null ? request.price() : request.originalPrice());
        dish.setStock(request.stock()); dish.setSales(0); dish.setStatus(DishStatus.ON_SALE);
        apply(dish, request); dishMapper.insert(dish); return DishDto.DishView.from(dish);
    }

    @Transactional
    public DishDto.DishView update(Long id, DishDto.SaveRequest request) {
        Dish dish = requireOwnedDish(id); apply(dish, request);
        dish.setPrice(request.price()); dish.setOriginalPrice(request.originalPrice() == null ? request.price() : request.originalPrice());
        dish.setStock(request.stock()); dishMapper.updateById(dish); return DishDto.DishView.from(dish);
    }

    @Transactional
    public DishDto.DishView status(Long id, DishStatus status) {
        Dish dish = requireOwnedDish(id);
        if (dish.getStatus() == DishStatus.DELETED) throw new BizException(ErrorCode.CONFLICT);
        dish.setStatus(status); dishMapper.updateById(dish); return DishDto.DishView.from(dish);
    }

    @Transactional
    public void delete(Long id) { status(id, DishStatus.DELETED); }

    @Transactional
    public DishDto.SpecView addSpec(Long dishId, DishDto.SpecRequest request) {
        requireOwnedDish(dishId);
        DishSpec spec = new DishSpec(); spec.setDishId(dishId); apply(spec, request); specMapper.insert(spec); return specView(spec);
    }

    @Transactional
    public DishDto.SpecView updateSpec(Long dishId, Long id, DishDto.SpecRequest request) {
        requireOwnedDish(dishId); DishSpec spec = requireSpec(dishId, id); apply(spec, request); specMapper.updateById(spec); return specView(spec);
    }

    @Transactional
    public void deleteSpec(Long dishId, Long id) {
        requireOwnedDish(dishId); specMapper.deleteById(requireSpec(dishId, id).getId());
    }

    private Dish requireDish(Long id) {
        Dish dish = dishMapper.selectById(id); if (dish == null) throw new BizException(ErrorCode.NOT_FOUND); return dish;
    }
    private Dish requireOwnedDish(Long id) { Dish dish = requireDish(id); merchantService.requireOwnedMerchant(dish.getMerchantId()); return dish; }
    private DishSpec requireSpec(Long dishId, Long id) {
        DishSpec spec = specMapper.selectById(id);
        if (spec == null || !spec.getDishId().equals(dishId)) throw new BizException(ErrorCode.NOT_FOUND); return spec;
    }
    private void apply(Dish dish, DishDto.SaveRequest r) { dish.setCategoryId(r.categoryId()); dish.setName(r.name()); dish.setDescription(r.description()); dish.setImageUrl(r.imageUrl()); dish.setSortOrder(r.sortOrder() == null ? 0 : r.sortOrder()); }
    private void apply(DishSpec spec, DishDto.SpecRequest r) { spec.setGroupName(r.groupName()); spec.setName(r.name()); spec.setPriceOffset(r.priceOffset() == null ? BigDecimal.ZERO : r.priceOffset()); spec.setSortOrder(r.sortOrder() == null ? 0 : r.sortOrder()); spec.setIsDefault(Boolean.TRUE.equals(r.isDefault())); }
    private static DishDto.SpecView specView(DishSpec s) { return new DishDto.SpecView(s.getId(), s.getDishId(), s.getGroupName(), s.getName(), s.getPriceOffset(), s.getSortOrder(), s.getIsDefault()); }
}
