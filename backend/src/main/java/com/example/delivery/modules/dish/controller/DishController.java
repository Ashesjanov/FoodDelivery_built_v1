package com.example.delivery.modules.dish.controller;

import com.example.delivery.common.ApiResponse;
import com.example.delivery.common.PageResponse;
import com.example.delivery.domain.enums.DishStatus;
import com.example.delivery.modules.dish.dto.DishDto;
import com.example.delivery.modules.dish.service.DishService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/dishes")
public class DishController {
    private final DishService service;
    public DishController(DishService service) { this.service = service; }

    @GetMapping
    public ApiResponse<PageResponse<DishDto.DishView>> page(@RequestParam Long merchantId,
            @RequestParam(required = false) Long categoryId, @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean onSaleOnly, @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(service.page(merchantId, categoryId, keyword, onSaleOnly, page, size));
    }
    @GetMapping("/{id}") public ApiResponse<DishDto.DishDetailView> detail(@PathVariable Long id) { return ApiResponse.ok(service.detail(id)); }
    @GetMapping("/{id}/specs") public ApiResponse<List<DishDto.SpecView>> specs(@PathVariable Long id) { return ApiResponse.ok(service.specs(id)); }
    @PostMapping public ApiResponse<DishDto.DishView> create(@RequestParam Long merchantId, @Valid @RequestBody DishDto.SaveRequest request) { return ApiResponse.created(service.create(merchantId, request)); }
    @PutMapping("/{id}") public ApiResponse<DishDto.DishView> update(@PathVariable Long id, @Valid @RequestBody DishDto.SaveRequest request) { return ApiResponse.ok(service.update(id, request)); }
    @PatchMapping("/{id}/status") public ApiResponse<DishDto.DishView> status(@PathVariable Long id, @Valid @RequestBody DishDto.StatusRequest request) { return ApiResponse.ok(service.status(id, request.status())); }
    @DeleteMapping("/{id}") public ApiResponse<Void> delete(@PathVariable Long id) { service.delete(id); return ApiResponse.ok(); }
    @PostMapping("/{dishId}/specs") public ApiResponse<DishDto.SpecView> addSpec(@PathVariable Long dishId, @Valid @RequestBody DishDto.SpecRequest request) { return ApiResponse.created(service.addSpec(dishId, request)); }
    @PutMapping("/{dishId}/specs/{id}") public ApiResponse<DishDto.SpecView> updateSpec(@PathVariable Long dishId, @PathVariable Long id, @Valid @RequestBody DishDto.SpecRequest request) { return ApiResponse.ok(service.updateSpec(dishId, id, request)); }
    @DeleteMapping("/{dishId}/specs/{id}") public ApiResponse<Void> deleteSpec(@PathVariable Long dishId, @PathVariable Long id) { service.deleteSpec(dishId, id); return ApiResponse.ok(); }
}
