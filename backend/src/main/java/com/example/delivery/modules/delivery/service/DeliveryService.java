package com.example.delivery.modules.delivery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.delivery.common.BizException;
import com.example.delivery.common.ErrorCode;
import com.example.delivery.common.PageResponse;
import com.example.delivery.domain.entity.DeliveryRecord;
import com.example.delivery.domain.entity.DeliveryRider;
import com.example.delivery.domain.entity.Orders;
import com.example.delivery.domain.entity.UserAccount;
import com.example.delivery.domain.enums.DeliveryStatus;
import com.example.delivery.domain.enums.OrderStatus;
import com.example.delivery.domain.enums.RiderStatus;
import com.example.delivery.domain.enums.UserRole;
import com.example.delivery.domain.mapper.DeliveryRecordMapper;
import com.example.delivery.domain.mapper.DeliveryRiderMapper;
import com.example.delivery.domain.mapper.OrdersMapper;
import com.example.delivery.domain.mapper.UserAccountMapper;
import com.example.delivery.modules.delivery.dto.DeliveryDto;
import com.example.delivery.modules.order.service.OrderPushService;
import com.example.delivery.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 配送服务：管理当前骑手资料，并维护订单从待接单到已送达的配送状态。
 * 依赖骑手/配送记录/订单/用户 Mapper 和 {@link OrderPushService}；写方法均在事务内，
 * 类级入口由控制器限制 RIDER 角色，服务内进一步隔离到当前骑手自己的数据。
 */
@Service
public class DeliveryService {
    private static final int MAX_PAGE_SIZE = 200;

    private final DeliveryRiderMapper riderMapper;
    private final DeliveryRecordMapper recordMapper;
    private final OrdersMapper ordersMapper;
    private final UserAccountMapper userAccountMapper;
    private final OrderPushService orderPushService;

    public DeliveryService(DeliveryRiderMapper riderMapper, DeliveryRecordMapper recordMapper,
                           OrdersMapper ordersMapper, UserAccountMapper userAccountMapper,
                           OrderPushService orderPushService) {
        this.riderMapper = riderMapper;
        this.recordMapper = recordMapper;
        this.ordersMapper = ordersMapper;
        this.userAccountMapper = userAccountMapper;
        this.orderPushService = orderPushService;
    }

    @Transactional
    public DeliveryDto.RiderView myRider() {
        return view(requireRider());
    }

    @Transactional
    public DeliveryDto.RiderView updateProfile(DeliveryDto.RiderProfileRequest request) {
        DeliveryRider rider = requireRider();
        rider.setName(request.name().trim());
        rider.setPhone(request.phone().trim());
        rider.setVehicleType(request.vehicleType().trim());
        rider.setUpdatedAt(LocalDateTime.now());
        riderMapper.updateById(rider);
        return view(rider);
    }

    @Transactional
    public DeliveryDto.RiderView updateStatus(DeliveryDto.RiderStatusRequest request) {
        DeliveryRider rider = requireRider();
        if (request.status() == RiderStatus.SUSPENDED) {
            throw new BizException(ErrorCode.ACCESS_DENIED, "riders cannot suspend their own account");
        }
        int active = activeCount(rider.getId());
        if (active > 0 && (request.status() == RiderStatus.OFFLINE || request.status() == RiderStatus.ONLINE)) {
            throw new BizException(ErrorCode.CONFLICT, "rider has active deliveries and must remain busy");
        }
        rider.setStatus(request.status());
        rider.setActiveOrderCount(active);
        rider.setUpdatedAt(LocalDateTime.now());
        riderMapper.updateById(rider);
        return view(rider);
    }

    @Transactional
    public DeliveryDto.RiderView updateLocation(DeliveryDto.LocationRequest request) {
        if (request.longitude().compareTo(new BigDecimal("-180")) < 0
                || request.longitude().compareTo(new BigDecimal("180")) > 0
                || request.latitude().compareTo(new BigDecimal("-90")) < 0
                || request.latitude().compareTo(new BigDecimal("90")) > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "rider location is outside valid longitude/latitude bounds");
        }
        DeliveryRider rider = requireRider();
        rider.setCurrentLongitude(request.longitude());
        rider.setCurrentLatitude(request.latitude());
        rider.setUpdatedAt(LocalDateTime.now());
        riderMapper.updateById(rider);
        return view(rider);
    }

    @Transactional(readOnly = true)
    public PageResponse<DeliveryDto.View> available(int page, int size) {
        requireRider();
        int current = normalizedPage(page);
        int pageSize = normalizedSize(size);
        Page<Orders> result = ordersMapper.selectPage(new Page<>(current, pageSize),
                new LambdaQueryWrapper<Orders>()
                        .eq(Orders::getStatus, OrderStatus.READY)
                        .orderByAsc(Orders::getReadyAt)
                        .orderByAsc(Orders::getId));

        List<Long> orderIds = result.getRecords().stream().map(Orders::getId).toList();
        List<DeliveryRecord> records = orderIds.isEmpty() ? List.of() : recordMapper.selectList(
                new LambdaQueryWrapper<DeliveryRecord>().in(DeliveryRecord::getOrderId, orderIds));
        List<DeliveryDto.View> available = result.getRecords().stream()
                .filter(order -> !hasActiveAssignment(records, order.getId()))
                .map(order -> viewForOrder(records, order.getId()))
                .toList();
        return PageResponse.of(available, result.getTotal(), current, pageSize);
    }

    @Transactional
    public DeliveryDto.View accept(Long orderId) {
        DeliveryRider rider = requireRider();
        requireAcceptableRider(rider);
        Orders order = requireOrder(orderId);
        if (order.getStatus() != OrderStatus.READY) {
            throw new BizException(ErrorCode.CONFLICT, "only ready orders can be accepted by a rider");
        }

        DeliveryRecord record = recordMapper.selectOne(new LambdaQueryWrapper<DeliveryRecord>()
                .eq(DeliveryRecord::getOrderId, orderId).last("limit 1"));
        LocalDateTime now = LocalDateTime.now();
        if (record == null) {
            // 首次建配送记录时由 order_id 唯一约束阻止并发骑手重复建单。
            record = new DeliveryRecord();
            record.setOrderId(orderId);
            record.setRiderId(rider.getId());
            record.setStatus(DeliveryStatus.ASSIGNED);
            record.setAcceptedAt(now);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            recordMapper.insert(record);
        } else {
            // 已有记录采用条件更新抢接：只有记录仍可领取且未被他人占用时才会更新成功。
            int claimed = recordMapper.update(null, new LambdaUpdateWrapper<DeliveryRecord>()
                    .eq(DeliveryRecord::getId, record.getId())
                    .and(q -> q.isNull(DeliveryRecord::getRiderId).or().eq(DeliveryRecord::getRiderId, rider.getId()))
                    .in(DeliveryRecord::getStatus, DeliveryStatus.WAITING_RIDER, DeliveryStatus.CANCELLED)
                    .set(DeliveryRecord::getRiderId, rider.getId())
                    .set(DeliveryRecord::getStatus, DeliveryStatus.ASSIGNED)
                    .set(DeliveryRecord::getAcceptedAt, now)
                    .set(DeliveryRecord::getUpdatedAt, now));
            if (claimed == 0) {
                throw new BizException(ErrorCode.CONFLICT, "delivery order was already claimed by another rider");
            }
            record.setRiderId(rider.getId());
            record.setStatus(DeliveryStatus.ASSIGNED);
            record.setAcceptedAt(now);
            record.setUpdatedAt(now);
        }

        riderMapper.update(null, new LambdaUpdateWrapper<DeliveryRider>()
                .eq(DeliveryRider::getId, rider.getId())
                .setSql("active_order_count = active_order_count + 1")
                .set(DeliveryRider::getStatus, RiderStatus.BUSY)
                .set(DeliveryRider::getUpdatedAt, now));
        // 配送事件与订单事件走同一 /topic/orders/{orderId} 链路，事务提交后客户端才会收到。
        orderPushService.publishAfterCommit(orderId, order.getStatus().getCode(), "rider accepted the delivery order");
        return view(record);
    }

    @Transactional
    public DeliveryDto.View pickup(Long orderId, DeliveryDto.DeliveryNoteRequest request) {
        DeliveryRider rider = requireRider();
        DeliveryRecord record = requireOwnedRecord(orderId, rider.getId());
        requireOrder(orderId);
        LocalDateTime now = LocalDateTime.now();
        // 配送记录和订单都以期望状态做条件更新，重复或并发操作不会越过 READY/ASSIGNED 状态。
        int updated = recordMapper.update(null, new LambdaUpdateWrapper<DeliveryRecord>()
                .eq(DeliveryRecord::getId, record.getId())
                .eq(DeliveryRecord::getRiderId, rider.getId())
                .eq(DeliveryRecord::getStatus, DeliveryStatus.ASSIGNED)
                .set(DeliveryRecord::getStatus, DeliveryStatus.PICKED_UP)
                .set(DeliveryRecord::getPickedUpAt, now)
                .set(DeliveryRecord::getDeliveryNote, note(request))
                .set(DeliveryRecord::getUpdatedAt, now));
        if (updated == 0) {
            throw new BizException(ErrorCode.CONFLICT, "delivery order must be assigned to this rider before pickup");
        }

        int orderUpdated = ordersMapper.update(null, new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getId, orderId)
                .eq(Orders::getStatus, OrderStatus.READY)
                .set(Orders::getStatus, OrderStatus.PICKED_UP)
                .set(Orders::getPickedAt, now)
                .set(Orders::getUpdatedAt, now));
        if (orderUpdated == 0) {
            throw new BizException(ErrorCode.CONFLICT, "order is no longer ready for pickup");
        }

        record.setStatus(DeliveryStatus.PICKED_UP);
        record.setPickedUpAt(now);
        record.setDeliveryNote(note(request));
        record.setUpdatedAt(now);
        orderPushService.publishAfterCommit(orderId, OrderStatus.PICKED_UP.getCode(), "delivery order was picked up");
        return view(record);
    }

    @Transactional
    public DeliveryDto.View deliver(Long orderId, DeliveryDto.DeliveryNoteRequest request) {
        DeliveryRider rider = requireRider();
        DeliveryRecord record = requireOwnedRecord(orderId, rider.getId());
        requireOrder(orderId);
        LocalDateTime now = LocalDateTime.now();
        int updated = recordMapper.update(null, new LambdaUpdateWrapper<DeliveryRecord>()
                .eq(DeliveryRecord::getId, record.getId())
                .eq(DeliveryRecord::getRiderId, rider.getId())
                .eq(DeliveryRecord::getStatus, DeliveryStatus.PICKED_UP)
                .set(DeliveryRecord::getStatus, DeliveryStatus.DELIVERED)
                .set(DeliveryRecord::getDeliveredAt, now)
                .set(DeliveryRecord::getDeliveryNote, note(request))
                .set(DeliveryRecord::getUpdatedAt, now));
        if (updated == 0) {
            throw new BizException(ErrorCode.CONFLICT, "delivery order must be picked up before delivery");
        }

        int orderUpdated = ordersMapper.update(null, new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getId, orderId)
                .eq(Orders::getStatus, OrderStatus.PICKED_UP)
                .set(Orders::getStatus, OrderStatus.DELIVERED)
                .set(Orders::getDeliveredAt, now)
                .set(Orders::getUpdatedAt, now));
        if (orderUpdated == 0) {
            throw new BizException(ErrorCode.CONFLICT, "order is not currently out for delivery");
        }

        // 送达后按实际仍处于配送中的记录恢复骑手在线状态，并累计完成单量。
        int remainingActive = activeCount(rider.getId());
        riderMapper.update(null, new LambdaUpdateWrapper<DeliveryRider>()
                .eq(DeliveryRider::getId, rider.getId())
                .setSql("active_order_count = CASE WHEN active_order_count > 0 THEN active_order_count - 1 ELSE 0 END")
                .setSql("completed_count = completed_count + 1")
                .set(DeliveryRider::getStatus, remainingActive < 1 ? RiderStatus.ONLINE : RiderStatus.BUSY)
                .set(DeliveryRider::getUpdatedAt, now));

        record.setStatus(DeliveryStatus.DELIVERED);
        record.setDeliveredAt(now);
        record.setDeliveryNote(note(request));
        record.setUpdatedAt(now);
        orderPushService.publishAfterCommit(orderId, OrderStatus.DELIVERED.getCode(), "delivery order was completed");
        return view(record);
    }

    @Transactional(readOnly = true)
    public List<DeliveryDto.View> active() {
        DeliveryRider rider = requireRider();
        return recordMapper.selectList(new LambdaQueryWrapper<DeliveryRecord>()
                        .eq(DeliveryRecord::getRiderId, rider.getId())
                        .in(DeliveryRecord::getStatus, DeliveryStatus.ASSIGNED, DeliveryStatus.PICKED_UP)
                        .orderByAsc(DeliveryRecord::getAcceptedAt))
                .stream().map(DeliveryService::view).toList();
    }

    @Transactional(readOnly = true)
    public DeliveryDto.View record(Long orderId) {
        DeliveryRider rider = requireRider();
        return view(requireOwnedRecord(orderId, rider.getId()));
    }

    @Transactional(readOnly = true)
    public PageResponse<DeliveryDto.View> records(int page, int size) {
        DeliveryRider rider = requireRider();
        int current = normalizedPage(page);
        int pageSize = normalizedSize(size);
        Page<DeliveryRecord> result = recordMapper.selectPage(new Page<>(current, pageSize),
                new LambdaQueryWrapper<DeliveryRecord>()
                        .eq(DeliveryRecord::getRiderId, rider.getId())
                        .orderByDesc(DeliveryRecord::getCreatedAt)
                        .orderByDesc(DeliveryRecord::getId));
        return PageResponse.of(result.getRecords().stream().map(DeliveryService::view).toList(),
                result.getTotal(), current, pageSize);
    }

    private DeliveryRider requireRider() {
        long userId = SecurityUtils.requireCurrentUserId();
        DeliveryRider rider = riderMapper.selectOne(new LambdaQueryWrapper<DeliveryRider>()
                .eq(DeliveryRider::getUserId, userId).last("limit 1"));
        if (rider != null) {
            return rider;
        }
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        if (user.getRole() != UserRole.RIDER) {
            throw new BizException(ErrorCode.ACCESS_DENIED, "delivery operations require the rider role");
        }
        LocalDateTime now = LocalDateTime.now();
        rider = DeliveryRider.builder()
                .userId(userId)
                .name(user.getNickname() == null || user.getNickname().isBlank() ? user.getUsername() : user.getNickname())
                .phone(user.getPhone())
                .vehicleType("UNSPECIFIED")
                .status(RiderStatus.OFFLINE)
                .rating(BigDecimal.ZERO)
                .completedCount(0)
                .activeOrderCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
        riderMapper.insert(rider);
        return rider;
    }

    private void requireAcceptableRider(DeliveryRider rider) {
        if (rider.getStatus() == RiderStatus.OFFLINE || rider.getStatus() == RiderStatus.SUSPENDED) {
            throw new BizException(ErrorCode.CONFLICT, "rider must be online or busy before accepting an order");
        }
    }

    private Orders requireOrder(Long orderId) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "delivery order was not found");
        }
        return order;
    }

    private DeliveryRecord requireOwnedRecord(Long orderId, Long riderId) {
        DeliveryRecord record = recordMapper.selectOne(new LambdaQueryWrapper<DeliveryRecord>()
                .eq(DeliveryRecord::getOrderId, orderId).last("limit 1"));
        if (record == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "delivery record was not found");
        }
        if (!Objects.equals(record.getRiderId(), riderId)) {
            throw new BizException(ErrorCode.ACCESS_DENIED, "delivery record belongs to another rider");
        }
        return record;
    }

    private int activeCount(Long riderId) {
        Long count = recordMapper.selectCount(new LambdaQueryWrapper<DeliveryRecord>()
                .eq(DeliveryRecord::getRiderId, riderId)
                .in(DeliveryRecord::getStatus, DeliveryStatus.ASSIGNED, DeliveryStatus.PICKED_UP));
        return count == null ? 0 : Math.toIntExact(count);
    }

    private static boolean hasActiveAssignment(List<DeliveryRecord> records, Long orderId) {
        return records.stream().anyMatch(record -> Objects.equals(record.getOrderId(), orderId)
                && record.getRiderId() != null
                && (record.getStatus() == DeliveryStatus.ASSIGNED || record.getStatus() == DeliveryStatus.PICKED_UP));
    }

    private static DeliveryDto.View viewForOrder(List<DeliveryRecord> records, Long orderId) {
        return records.stream().filter(record -> Objects.equals(record.getOrderId(), orderId)).findFirst()
                .map(DeliveryService::view)
                .orElseGet(() -> new DeliveryDto.View(null, orderId, null, DeliveryStatus.WAITING_RIDER,
                        null, null, null, null, null, null, null, null));
    }

    private static DeliveryDto.View view(DeliveryRecord record) {
        return new DeliveryDto.View(record.getId(), record.getOrderId(), record.getRiderId(), record.getStatus(),
                record.getPickupCode(), record.getDeliveryNote(), record.getDistanceKm(), record.getAcceptedAt(),
                record.getPickedUpAt(), record.getDeliveredAt(), record.getCreatedAt(), record.getUpdatedAt());
    }

    private static DeliveryDto.RiderView view(DeliveryRider rider) {
        return new DeliveryDto.RiderView(rider.getId(), rider.getUserId(), rider.getName(), rider.getPhone(),
                rider.getVehicleType(), rider.getStatus(), rider.getCurrentLongitude(), rider.getCurrentLatitude(),
                rider.getRating(), rider.getCompletedCount(), rider.getActiveOrderCount());
    }

    private static String note(DeliveryDto.DeliveryNoteRequest request) {
        return request == null || request.deliveryNote() == null ? null : request.deliveryNote().trim();
    }

    private static int normalizedPage(int page) {
        return Math.max(page, 1);
    }

    private static int normalizedSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }
}
