package com.example.delivery.modules.merchant.controller;

import com.example.delivery.common.ApiResponse;
import com.example.delivery.common.PageResponse;
import com.example.delivery.domain.enums.MerchantBusinessStatus;
import com.example.delivery.modules.merchant.dto.MerchantDto;
import com.example.delivery.modules.merchant.service.MerchantService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/merchants")
public class MerchantController {
    private final MerchantService service;
    public MerchantController(MerchantService service) { this.service = service; }

    @GetMapping
    public ApiResponse<PageResponse<MerchantDto.MerchantView>> page(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword, @RequestParam(defaultValue = "false") boolean openOnly) {
        return ApiResponse.ok(service.page(page, size, categoryId, keyword, openOnly));
    }

    @GetMapping("/categories")
    public ApiResponse<List<MerchantDto.CategoryView>> categories() { return ApiResponse.ok(service.categories()); }

    @PostMapping("/categories")
    public ApiResponse<MerchantDto.CategoryView> createCategory(@Valid @RequestBody MerchantDto.CategoryRequest request) {
        return ApiResponse.created(service.createCategory(request));
    }

    @PutMapping("/categories/{id}")
    public ApiResponse<MerchantDto.CategoryView> updateCategory(@PathVariable Long id, @Valid @RequestBody MerchantDto.CategoryRequest request) {
        return ApiResponse.ok(service.updateCategory(id, request));
    }

    @DeleteMapping("/categories/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) { service.deleteCategory(id); return ApiResponse.ok(); }

    @GetMapping("/{id}")
    public ApiResponse<MerchantDto.MerchantDetailView> detail(@PathVariable Long id) { return ApiResponse.ok(service.detail(id)); }

    @PostMapping
    public ApiResponse<MerchantDto.MerchantView> create(@Valid @RequestBody MerchantDto.SaveRequest request) {
        return ApiResponse.created(service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<MerchantDto.MerchantView> update(@PathVariable Long id, @Valid @RequestBody MerchantDto.SaveRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @PatchMapping("/{id}/business-status")
    public ApiResponse<MerchantDto.MerchantView> status(@PathVariable Long id, @Valid @RequestBody MerchantDto.BusinessStatusRequest request) {
        return ApiResponse.ok(service.changeStatus(id, request.status()));
    }

    @PostMapping("/{id}/categories/{categoryId}")
    public ApiResponse<Void> addCategory(@PathVariable Long id, @PathVariable Long categoryId) { service.addCategory(id, categoryId); return ApiResponse.ok(); }

    @DeleteMapping("/{id}/categories/{categoryId}")
    public ApiResponse<Void> removeCategory(@PathVariable Long id, @PathVariable Long categoryId) { service.removeCategory(id, categoryId); return ApiResponse.ok(); }
}
