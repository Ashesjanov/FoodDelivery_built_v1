package com.example.delivery.modules.storage.controller;

import com.example.delivery.common.ApiResponse;
import com.example.delivery.modules.storage.dto.UploadDto;
import com.example.delivery.modules.storage.service.StorageService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片上传接口，入口为 {@code /api/merchant/uploads}。
 * 路径前缀由 {@code SecurityConfig} 限定为商家或管理员，返回值可直接回填到创建菜品的 {@code imageUrl}。
 */
@RestController
@RequestMapping("/api/merchant/uploads")
public class UploadController {
    private final StorageService storageService;

    public UploadController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UploadDto.ImageUploadView> uploadImage(@RequestParam("file") MultipartFile file,
                                                             @RequestParam(value = "scene", required = false) String scene) {
        return ApiResponse.created(storageService.uploadImage(file, scene));
    }
}
