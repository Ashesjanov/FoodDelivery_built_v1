package com.example.delivery.modules.storage;

import com.example.delivery.common.BizException;
import com.example.delivery.common.ErrorCode;
import com.example.delivery.config.StorageProperties;
import com.example.delivery.modules.storage.dto.UploadDto;
import com.example.delivery.modules.storage.service.ImageFileValidator;
import com.example.delivery.modules.storage.service.StorageService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link StorageService} 与 {@link ImageFileValidator} 的单元测试。
 * 用模拟的 {@link MinioClient} 断言对象名、桶策略和上传参数，不依赖真实对象存储。
 */
@ExtendWith(MockitoExtension.class)
class StorageServiceTest {
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

    @Mock
    private MinioClient minioClient;

    private StorageService service() {
        return new StorageService(minioClient, new StorageProperties(
                "http://127.0.0.1:9000", null, "food_delivery", "secret", "food-delivery", MAX_FILE_SIZE));
    }

    @Test
    void uploadsImageAndReturnsPublicUrl() throws Exception {
        when(minioClient.bucketExists(any())).thenReturn(false);
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

        UploadDto.ImageUploadView uploaded = service().uploadImage(pngFile("图片"), "dish");

        assertThat(uploaded.contentType()).isEqualTo("image/png");
        assertThat(uploaded.objectName()).matches("dish/\\d{8}/[0-9a-f]{32}\\.png");
        assertThat(uploaded.url()).isEqualTo("http://127.0.0.1:9000/food-delivery/" + uploaded.objectName());
        assertThat(uploaded.size()).isPositive();

        ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient).makeBucket(any());
        verify(minioClient).setBucketPolicy(any());
        verify(minioClient).putObject(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo("food-delivery");
        assertThat(captor.getValue().object()).isEqualTo(uploaded.objectName());
        assertThat(captor.getValue().contentType()).isEqualTo("image/png");
    }

    @Test
    void reusesExistingBucketWithoutRecreatingIt() throws Exception {
        when(minioClient.bucketExists(any())).thenReturn(true);
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

        StorageService service = service();
        service.uploadImage(pngFile("first"), null);
        service.uploadImage(pngFile("second"), null);

        verify(minioClient, times(2)).putObject(any(PutObjectArgs.class));
        verify(minioClient, never()).makeBucket(any());
        // 桶策略只在首次上传时设置一次，后续上传复用缓存结果。
        verify(minioClient, times(1)).setBucketPolicy(any());
    }

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> service().uploadImage(empty, "dish"))
                .isInstanceOf(BizException.class)
                .extracting(exception -> ((BizException) exception).getErrorCode())
                .isEqualTo(ErrorCode.EMPTY_FILE);
    }

    @Test
    void rejectsOversizedFile() {
        byte[] oversized = new byte[(int) MAX_FILE_SIZE + 1];
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", oversized);

        assertThatThrownBy(() -> service().uploadImage(file, "dish"))
                .isInstanceOf(BizException.class)
                .extracting(exception -> ((BizException) exception).getErrorCode())
                .isEqualTo(ErrorCode.FILE_TOO_LARGE);
    }

    @Test
    void rejectsUnsupportedContentType() {
        MockMultipartFile file = new MockMultipartFile("file", "note.pdf", "application/pdf", "%PDF-1.7".getBytes(StandardCharsets.US_ASCII));

        assertThatThrownBy(() -> service().uploadImage(file, "dish"))
                .isInstanceOf(BizException.class)
                .extracting(exception -> ((BizException) exception).getErrorCode())
                .isEqualTo(ErrorCode.UNSUPPORTED_FILE_TYPE);
    }

    @Test
    void rejectsContentThatDoesNotMatchDeclaredType() {
        // 声明为 PNG，实际是 JPEG 文件头，必须拒绝而不是写入存储桶。
        MockMultipartFile file = new MockMultipartFile("file", "fake.png", "image/png", jpegBytes());

        assertThatThrownBy(() -> service().uploadImage(file, "dish"))
                .isInstanceOf(BizException.class)
                .extracting(exception -> ((BizException) exception).getErrorCode())
                .isEqualTo(ErrorCode.UNSUPPORTED_FILE_TYPE);
    }

    @Test
    void mapsStorageFailuresToStorageError() throws Exception {
        when(minioClient.bucketExists(any())).thenThrow(new java.io.IOException("minio is down"));

        assertThatThrownBy(() -> service().uploadImage(pngFile("图片"), "dish"))
                .isInstanceOf(BizException.class)
                .extracting(exception -> ((BizException) exception).getErrorCode())
                .isEqualTo(ErrorCode.STORAGE_ERROR);
    }

    @Test
    void acceptsEverySupportedImageHeader() {
        assertThat(ImageFileValidator.validate(pngFile("png"), MAX_FILE_SIZE)).isEqualTo("image/png");
        assertThat(ImageFileValidator.validate(new MockMultipartFile("file", "a.jpg", "image/jpeg", jpegBytes()), MAX_FILE_SIZE))
                .isEqualTo("image/jpeg");
        assertThat(ImageFileValidator.validate(new MockMultipartFile("file", "a.gif", "image/gif", "GIF89a".getBytes(StandardCharsets.US_ASCII)), MAX_FILE_SIZE))
                .isEqualTo("image/gif");
        assertThat(ImageFileValidator.validate(new MockMultipartFile("file", "a.webp", "image/webp", webpBytes()), MAX_FILE_SIZE))
                .isEqualTo("image/webp");
    }

    @Test
    void sanitizesSceneIntoObjectPath() {
        assertThat(ImageFileValidator.scenePrefix("dish")).isEqualTo("dish");
        assertThat(ImageFileValidator.scenePrefix(" Merchant-Logo ")).isEqualTo("merchant-logo");
        assertThat(ImageFileValidator.scenePrefix("../../etc")).isEqualTo("dish");
        assertThat(ImageFileValidator.scenePrefix(null)).isEqualTo("dish");
    }

    private static MockMultipartFile pngFile(String content) {
        byte[] png = new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D};
        return new MockMultipartFile("file", "dish.png", "image/png", concat(png, content.getBytes(StandardCharsets.UTF_8)));
    }

    private static byte[] jpegBytes() {
        return new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 'J', 'F', 'I', 'F', 0x00, 0x01};
    }

    private static byte[] webpBytes() {
        return concat("RIFF".getBytes(StandardCharsets.US_ASCII),
                new byte[]{0x1A, 0x00, 0x00, 0x00},
                "WEBPVP8 ".getBytes(StandardCharsets.US_ASCII));
    }

    private static byte[] concat(byte[]... parts) {
        int length = 0;
        for (byte[] part : parts) {
            length += part.length;
        }
        byte[] merged = new byte[length];
        int offset = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, merged, offset, part.length);
            offset += part.length;
        }
        return merged;
    }
}
