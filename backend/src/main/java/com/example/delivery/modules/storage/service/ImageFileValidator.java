package com.example.delivery.modules.storage.service;

import com.example.delivery.common.BizException;
import com.example.delivery.common.ErrorCode;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 图片上传的格式、大小与内容校验，供 {@link StorageService} 在写入对象存储前调用。
 * 同时校验声明的 MIME 类型和文件头魔数，避免把重命名后的非图片文件写入存储桶；
 * 校验通过后返回规范化的 MIME 类型，用于决定对象后缀。
 */
public final class ImageFileValidator {

    /** 允许上传的图片 MIME 类型与其对象后缀。 */
    private static final Map<String, String> EXTENSIONS = extensions();

    /** 可上传图片的场景前缀，决定对象在桶内的目录，避免 request 参数影响对象路径。 */
    private static final String DEFAULT_SCENE = "dish";

    private ImageFileValidator() {
    }

    /**
     * 校验单个上传文件。
     *
     * @return 规范化的 MIME 类型，例如 {@code image/png}
     * @throws BizException 文件为空、超过大小上限、类型不支持或内容与声明类型不符
     */
    public static String validate(MultipartFile file, long maxFileSize) {
        if (file == null || file.isEmpty() || file.getSize() <= 0) {
            throw new BizException(ErrorCode.EMPTY_FILE);
        }
        if (file.getSize() > maxFileSize) {
            throw new BizException(ErrorCode.FILE_TOO_LARGE, "image must not exceed " + maxFileSize + " bytes");
        }
        String declared = normalizeContentType(file.getContentType());
        if (declared == null) {
            throw new BizException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }
        if (!declared.equals(sniffContentType(file))) {
            throw new BizException(ErrorCode.UNSUPPORTED_FILE_TYPE, "image content does not match its declared type");
        }
        return declared;
    }

    /** 返回 MIME 类型对应的对象后缀，调用前必须已通过 {@link #validate}。 */
    public static String extensionOf(String contentType) {
        return EXTENSIONS.getOrDefault(normalizeContentType(contentType), "jpg");
    }

    /** 把场景名收敛为安全的目录名，非法值回落到默认场景。 */
    public static String scenePrefix(String scene) {
        if (scene == null || scene.isBlank()) {
            return DEFAULT_SCENE;
        }
        String candidate = scene.trim().toLowerCase(Locale.ROOT);
        return candidate.matches("[a-z0-9][a-z0-9-]{0,31}") ? candidate : DEFAULT_SCENE;
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null) {
            return null;
        }
        String normalized = contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
        if ("image/jpg".equals(normalized)) {
            normalized = "image/jpeg";
        }
        return EXTENSIONS.containsKey(normalized) ? normalized : null;
    }

    /** 通过文件头判断图片真实类型，无法识别时返回 {@code null}。 */
    private static String sniffContentType(MultipartFile file) {
        byte[] header = new byte[12];
        int length;
        try (InputStream input = file.getInputStream()) {
            length = input.readNBytes(header, 0, header.length);
        } catch (IOException cause) {
            throw new BizException(ErrorCode.UNSUPPORTED_FILE_TYPE, "uploaded file cannot be read", cause);
        }
        if (length >= 8 && (header[0] & 0xFF) == 0x89 && header[1] == 'P' && header[2] == 'N' && header[3] == 'G') {
            return "image/png";
        }
        if (length >= 3 && (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (length >= 6 && ascii(header, 0, 6).startsWith("GIF8")) {
            return "image/gif";
        }
        if (length >= 12 && "RIFF".equals(ascii(header, 0, 4)) && "WEBP".equals(ascii(header, 8, 4))) {
            return "image/webp";
        }
        return null;
    }

    private static String ascii(byte[] bytes, int offset, int length) {
        return new String(bytes, offset, length, StandardCharsets.US_ASCII);
    }

    private static Map<String, String> extensions() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("image/jpeg", "jpg");
        map.put("image/png", "png");
        map.put("image/webp", "webp");
        map.put("image/gif", "gif");
        return Map.copyOf(map);
    }
}
