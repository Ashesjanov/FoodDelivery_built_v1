package com.example.delivery.modules.cart.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.delivery.common.BizException;
import com.example.delivery.common.ErrorCode;
import com.example.delivery.domain.entity.CartItem;
import com.example.delivery.domain.entity.Dish;
import com.example.delivery.domain.entity.DishSpec;
import com.example.delivery.domain.entity.Merchant;
import com.example.delivery.domain.enums.CartItemStatus;
import com.example.delivery.domain.enums.DishStatus;
import com.example.delivery.domain.mapper.CartItemMapper;
import com.example.delivery.domain.mapper.DishMapper;
import com.example.delivery.domain.mapper.DishSpecMapper;
import com.example.delivery.modules.cart.dto.CartDto;
import com.example.delivery.modules.merchant.service.MerchantService;
import com.example.delivery.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 维护当前登录用户的购物车，并向订单流程提供已选条目。
 * 依赖购物车、菜品、规格 Mapper 和 {@link MerchantService}；写操作使用事务，
 * 所有查询通过 {@link SecurityUtils} 限定到当前用户，同商家、在售状态、库存和规格价格约束在此维护。
 */
@Service
public class CartService {
    private final CartItemMapper cartMapper;
    private final DishMapper dishMapper;
    private final DishSpecMapper specMapper;
    private final MerchantService merchantService;

    public CartService(CartItemMapper cartMapper, DishMapper dishMapper, DishSpecMapper specMapper, MerchantService merchantService) {
        this.cartMapper = cartMapper; this.dishMapper = dishMapper; this.specMapper = specMapper; this.merchantService = merchantService;
    }

    public CartDto.CartView cart() {
        long userId = SecurityUtils.requireCurrentUserId();
        List<CartItem> items = active(userId);
        List<CartDto.ItemView> views = items.stream().map(this::view).toList();
        // 总价按持久化单价乘数量后汇总。
        BigDecimal total = views.stream().map(CartDto.ItemView::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartDto.CartView(views, total, views.size());
    }

    @Transactional
    public CartDto.ItemView add(CartDto.AddItemRequest request) {
        long userId = SecurityUtils.requireCurrentUserId();
        Dish dish = dishMapper.selectById(request.dishId());
        if (dish == null || dish.getStatus() == DishStatus.DELETED) throw new BizException(ErrorCode.NOT_FOUND);
        if (dish.getStatus() != DishStatus.ON_SALE || dish.getStock() < request.quantity()) throw new BizException(ErrorCode.CONFLICT, "商品当前不可购买");
        DishSpec spec = request.dishSpecId() == null ? null : specMapper.selectById(request.dishSpecId());
        if (request.dishSpecId() != null && (spec == null || !spec.getDishId().equals(dish.getId()))) throw new BizException(ErrorCode.BAD_REQUEST, "规格与商品不匹配");
        Merchant merchant = merchantService.requireMerchant(dish.getMerchantId());
        List<CartItem> current = active(userId);
        // 一笔订单只能结算同一商家，跨商家加购在落库前直接拒绝。
        if (!current.isEmpty() && current.stream().anyMatch(i -> !i.getMerchantId().equals(merchant.getId()))) {
            throw new BizException(ErrorCode.CONFLICT, "购物车只能包含同一商家商品");
        }
        CartItem item = current.stream().filter(i -> i.getDishId().equals(dish.getId()) && Objects.equals(i.getDishSpecId(), request.dishSpecId()))
                .findFirst().orElse(null);
        int quantity = request.quantity();
        // 同菜品同规格合并数量，并按合并后的总量再次校验库存。
        if (item != null) quantity += item.getQuantity();
        if (quantity > dish.getStock()) throw new BizException(ErrorCode.CONFLICT, "商品库存不足");
        if (item == null) {
            item = new CartItem();
            item.setUserId(userId); item.setMerchantId(merchant.getId()); item.setDishId(dish.getId());
            item.setDishSpecId(spec == null ? null : spec.getId()); item.setStatus(CartItemStatus.ACTIVE); item.setSelected(true);
        }
        item.setQuantity(quantity); item.setUnitPrice(unitPrice(dish, spec));
        item.setNote(request.note() == null ? item.getNote() : request.note());
        item.setUpdatedAt(LocalDateTime.now());
        if (item.getId() == null) cartMapper.insert(item); else cartMapper.updateById(item);
        return view(item);
    }

    @Transactional
    public CartDto.ItemView update(Long id, CartDto.UpdateItemRequest request) {
        CartItem item = require(id); Dish dish = dishMapper.selectById(item.getDishId());
        if (dish == null) throw new BizException(ErrorCode.NOT_FOUND);
        if (request.quantity() != null) {
            // 修改数量时重新读取实时库存，不能沿用加购时的库存快照。
            if (dish.getStock() < request.quantity()) throw new BizException(ErrorCode.CONFLICT, "商品库存不足");
            item.setQuantity(request.quantity());
        }
        if (request.selected() != null) item.setSelected(request.selected());
        if (request.note() != null) item.setNote(request.note());
        item.setUpdatedAt(LocalDateTime.now()); cartMapper.updateById(item); return view(item);
    }

    @Transactional
    public void selectAll(boolean selected) {
        for (CartItem item : active(SecurityUtils.requireCurrentUserId())) { item.setSelected(selected); item.setUpdatedAt(LocalDateTime.now()); cartMapper.updateById(item); }
    }

    @Transactional
    public void remove(Long id) {
        // 删除采用状态迁移，便于订单流程区分有效、已结算和已移除条目。
        CartItem item = require(id); item.setStatus(CartItemStatus.REMOVED); item.setUpdatedAt(LocalDateTime.now()); cartMapper.updateById(item);
    }

    @Transactional
    public void clear() {
        cartMapper.update(null, new LambdaUpdateWrapper<CartItem>().eq(CartItem::getUserId, SecurityUtils.requireCurrentUserId())
                .eq(CartItem::getStatus, CartItemStatus.ACTIVE).set(CartItem::getStatus, CartItemStatus.REMOVED));
    }

    public List<CartItem> selectedItems(long userId) {
        return cartMapper.selectList(new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId)
                .eq(CartItem::getStatus, CartItemStatus.ACTIVE).eq(CartItem::getSelected, true).orderByAsc(CartItem::getMerchantId));
    }

    public void markCheckedOut(List<CartItem> items) {
        // 订单创建成功后调用，防止同一购物车条目被重复结算。
        for (CartItem item : items) { item.setStatus(CartItemStatus.CHECKED_OUT); item.setUpdatedAt(LocalDateTime.now()); cartMapper.updateById(item); }
    }

    private List<CartItem> active(long userId) {
        return cartMapper.selectList(new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId)
                .eq(CartItem::getStatus, CartItemStatus.ACTIVE).orderByAsc(CartItem::getCreatedAt));
    }

    private CartItem require(Long id) {
        CartItem item = cartMapper.selectById(id);
        if (item == null || !Objects.equals(item.getUserId(), SecurityUtils.requireCurrentUserId()) || item.getStatus() != CartItemStatus.ACTIVE) throw new BizException(ErrorCode.NOT_FOUND);
        return item;
    }

    private CartDto.ItemView view(CartItem item) {
        Dish dish = dishMapper.selectById(item.getDishId());
        DishSpec spec = item.getDishSpecId() == null ? null : specMapper.selectById(item.getDishSpecId());
        BigDecimal unit = item.getUnitPrice() == null ? (dish == null ? BigDecimal.ZERO : unitPrice(dish, spec)) : item.getUnitPrice();
        return new CartDto.ItemView(item.getId(), item.getMerchantId(), item.getDishId(), item.getDishSpecId(), dish == null ? null : dish.getName(),
                spec == null ? null : spec.getName(), dish == null ? null : dish.getImageUrl(), item.getQuantity(), unit,
                unit.multiply(BigDecimal.valueOf(item.getQuantity())), item.getSelected(), item.getNote(), item.getUpdatedAt());
    }

    private BigDecimal unitPrice(Dish dish, DishSpec spec) {
        // 成交单价等于菜品基础价加所选规格的价格偏移。
        return dish.getPrice().add(spec == null || spec.getPriceOffset() == null ? BigDecimal.ZERO : spec.getPriceOffset());
    }
}
