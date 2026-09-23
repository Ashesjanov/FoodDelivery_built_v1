package com.example.delivery.modules.user.controller;

import com.example.delivery.common.ApiResponse;
import com.example.delivery.modules.user.dto.AddressRequest;
import com.example.delivery.modules.user.dto.AddressResponse;
import com.example.delivery.modules.user.service.UserAddressService;
import com.example.delivery.security.CurrentUser;
import com.example.delivery.security.UserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "User addresses")
@RestController
@RequestMapping("/api/users/me/addresses")
@PreAuthorize("isAuthenticated()")
public class UserAddressController {
    private final UserAddressService userAddressService;

    public UserAddressController(UserAddressService userAddressService) {
        this.userAddressService = userAddressService;
    }

    @GetMapping
    public ApiResponse<List<AddressResponse>> list(@CurrentUser UserPrincipal principal) {
        return ApiResponse.ok(userAddressService.list(principal.userId()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponse>> create(@CurrentUser UserPrincipal principal,
                                                               @Valid @RequestBody AddressRequest request) {
        AddressResponse address = userAddressService.create(principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(address));
    }

    @GetMapping("/{addressId}")
    public ApiResponse<AddressResponse> get(@CurrentUser UserPrincipal principal, @PathVariable long addressId) {
        return ApiResponse.ok(userAddressService.get(principal.userId(), addressId));
    }

    @PutMapping("/{addressId}")
    public ApiResponse<AddressResponse> update(@CurrentUser UserPrincipal principal, @PathVariable long addressId,
                                                @Valid @RequestBody AddressRequest request) {
        return ApiResponse.ok(userAddressService.update(principal.userId(), addressId, request));
    }

    @DeleteMapping("/{addressId}")
    public ApiResponse<Void> delete(@CurrentUser UserPrincipal principal, @PathVariable long addressId) {
        userAddressService.delete(principal.userId(), addressId);
        return ApiResponse.ok();
    }

    @PutMapping("/{addressId}/default")
    public ApiResponse<AddressResponse> setDefault(@CurrentUser UserPrincipal principal,
                                                   @PathVariable long addressId) {
        return ApiResponse.ok(userAddressService.setDefault(principal.userId(), addressId));
    }
}
