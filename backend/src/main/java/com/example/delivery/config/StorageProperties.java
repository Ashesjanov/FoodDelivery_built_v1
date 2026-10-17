package com.example.delivery.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 对象存储（MinIO）配置，绑定 {@code storage.minio} 前缀。
 * {@code endpoint} 供后端上传使用，{@code publicEndpoint} 是写入响应、供浏览器直接访问图片的地址，
 * 部署时二者可能不同（例如后端走容器内网名称、浏览器走公网域名）。
 *
 * @param endpoint       后端访问 MinIO 的地址
 * @param publicEndpoint 返回给前端的公开访问地址，缺省时与 endpoint 相同
 * @param accessKey      MinIO 访问密钥
 * @param secretKey      MinIO 密钥
 * @param bucket         存放图片的桶，服务启动后首次上传时自动创建
 * @param maxFileSize    单张图片允许的最大字节数
 */
@ConfigurationProperties(prefix = "storage.minio")
public record StorageProperties(String endpoint, String publicEndpoint, String accessKey, String secretKey,
                                String bucket, long maxFileSize) {

    public StorageProperties {
        if (publicEndpoint == null || publicEndpoint.isBlank()) {
            publicEndpoint = endpoint;
        }
        if (bucket == null || bucket.isBlank()) {
            bucket = "food-delivery";
        }
        if (maxFileSize <= 0) {
            maxFileSize = 5L * 1024 * 1024;
        }
    }
}
