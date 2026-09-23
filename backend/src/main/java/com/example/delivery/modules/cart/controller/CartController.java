package com.example.delivery.modules.cart.controller;

import com.example.delivery.common.ApiResponse;
import com.example.delivery.modules.cart.dto.CartDto;
import com.example.delivery.modules.cart.service.CartService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    private final CartService service;
    public CartController(CartService service) { this.service = service; }
    @GetMapping public ApiResponse<CartDto.CartView> cart() { return ApiResponse.ok(service.cart()); }
    @PostMapping("/items") public ApiResponse<CartDto.ItemView> add(@Valid @RequestBody CartDto.AddItemRequest request) { return ApiResponse.created(service.add(request)); }
    @PutMapping("/items/{id}") public ApiResponse<CartDto.ItemView> update(@PathVariable Long id, @Valid @RequestBody CartDto.UpdateItemRequest request) { return ApiResponse.ok(service.update(id, request)); }
    @PostMapping("/select-all") public ApiResponse<Void> selectAll(@Valid @RequestBody CartDto.SelectAllRequest request) { service.selectAll(request.selected()); return ApiResponse.ok(); }
    @DeleteMapping("/items/{id}") public ApiResponse<Void> remove(@PathVariable Long id) { service.remove(id); return ApiResponse.ok(); }
    @DeleteMapping public ApiResponse<Void> clear() { service.clear(); return ApiResponse.ok(); }
}
