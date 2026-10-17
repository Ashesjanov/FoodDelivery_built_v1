package com.example.delivery.modules.storage.service;

import com.example.delivery.common.BizException;
import com.example.delivery.common.ErrorCode;
import com.example.delivery.config.StorageProperties;
import com.example.delivery.modules.storage.dto.UploadDto;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetBucketPolicyArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 把商家上传的图片写入 MinIO 并返回公开访问地址。
 * 首次上传时惰性创建存储桶并设置匿名只读策略，因此对象存储暂不可用不会阻塞应用启动；
 * 图片格式、大小和存放路径分别由 {@link ImageFileValidator} 与方法内的路径规则约束。
 */
@Service
public class StorageService {
    private static final Logger log = LoggerFactory.getLogger(StorageService.class);
    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final MinioClient client;
    private final StorageProperties properties;
    private volatile boolean bucketReady;

    public StorageService(MinioClient client, StorageProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    /**
     * 上传单张图片。
     *
     * @param file  表单中的图片文件
     * @param scene 业务场景（例如 {@code dish}），非法值回落到默认场景
     * @return 图片的公开地址与存储元数据
     */
    public UploadDto.ImageUploadView uploadImage(MultipartFile file, String scene) {
        String contentType = ImageFileValidator.validate(file, properties.maxFileSize());
        String objectName = objectName(scene, contentType);
        try {
            ensureBucket();
            try (InputStream content = file.getInputStream()) {
                client.putObject(PutObjectArgs.builder()
                        .bucket(properties.bucket())
                        .object(objectName)
                        .stream(content, file.getSize(), -1)
                        .contentType(contentType)
                        .build());
            }
        } catch (BizException cause) {
            // ensureBucket 已把连接类问题映射成存储错误，保持原错误码向上抛出。
            throw cause;
        } catch (Exception cause) {
            log.error("Failed to upload image {} to bucket {}", objectName, properties.bucket(), cause);
            throw new BizException(ErrorCode.STORAGE_ERROR, "image upload failed", cause);
        }
        log.info("Uploaded image {} ({} bytes)", objectName, file.getSize());
        return new UploadDto.ImageUploadView(publicUrl(objectName), objectName, file.getSize(), contentType);
    }

    /** 生成 {@code scene/yyyyMMdd/uuid.ext} 形式的对象名，调用前必须已通过图片校验。 */
    String objectName(String scene, String contentType) {
        return "%s/%s/%s.%s".formatted(ImageFileValidator.scenePrefix(scene),
                LocalDate.now().format(DATE_PATH),
                UUID.randomUUID().toString().replace("-", ""),
                ImageFileValidator.extensionOf(contentType));
    }

    /** 拼装供浏览器直接访问的公开地址。 */
    String publicUrl(String objectName) {
        String base = properties.publicEndpoint();
        if (base == null || base.isBlank()) {
            base = properties.endpoint();
        }
        base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return "%s/%s/%s".formatted(base, properties.bucket(), objectName);
    }

    /** 惰性确保存储桶存在且允许匿名读取，成功后不再重复检查。 */
    private synchronized void ensureBucket() {
        if (bucketReady) {
            return;
        }
        try {
            if (!client.bucketExists(BucketExistsArgs.builder().bucket(properties.bucket()).build())) {
                client.makeBucket(MakeBucketArgs.builder().bucket(properties.bucket()).build());
                log.info("Created MinIO bucket {}", properties.bucket());
            }
            client.setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(properties.bucket())
                    .config(publicReadPolicy())
                    .build());
            bucketReady = true;
        } catch (Exception cause) {
            log.error("MinIO bucket {} is not available", properties.bucket(), cause);
            throw new BizException(ErrorCode.STORAGE_ERROR, "object storage is unavailable", cause);
        }
    }

    /** 只允许读取桶内对象的最小匿名策略，上传和删除仍需凭证。 */
    private String publicReadPolicy() {
        return """
                {"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"AWS":["*"]},\
                "Action":["s3:GetObject"],"Resource":["arn:aws:s3:::%s/*"]}]}""".formatted(properties.bucket());
    }
}
