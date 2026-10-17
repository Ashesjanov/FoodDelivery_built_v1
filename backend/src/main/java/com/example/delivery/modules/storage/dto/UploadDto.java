package com.example.delivery.modules.storage.dto;

/**
 * 图片上传的响应结构，供商家在创建或编辑菜品时回填 {@code imageUrl}。
 *
 * @param url         图片的公开访问地址，可直接写入菜品
 * @param objectName  对象存储内的相对路径
 * @param size        文件字节数
 * @param contentType 服务端确认的图片 MIME 类型
 */
public final class UploadDto {
    private UploadDto() {
    }

    public record ImageUploadView(String url, String objectName, long size, String contentType) {
    }
}
