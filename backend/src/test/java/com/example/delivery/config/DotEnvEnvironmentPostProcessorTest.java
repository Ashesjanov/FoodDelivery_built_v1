package com.example.delivery.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 .env 查找、简单 shell 风格解析和低优先级属性合并规则。
 * 测试只调用 DotEnvEnvironmentPostProcessor 的包内工具方法，不启动完整 Spring 上下文。
 */
class DotEnvEnvironmentPostProcessorTest {

    @Test
    void findsEnvFileFromChildDirectory(@TempDir Path tempDir) throws Exception {
        Path envFile = tempDir.resolve(".env");
        Files.writeString(envFile, "JWT_SECRET=from-dot-env\n");

        assertThat(DotEnvEnvironmentPostProcessor.findEnvFile(tempDir.resolve("backend"))).isEqualTo(envFile);
    }

    @Test
    void loadsDotEnvWithoutOverridingExistingProperties(@TempDir Path tempDir) throws Exception {
        Path envFile = tempDir.resolve(".env");
        Files.writeString(envFile, "JWT_SECRET=from-dot-env\nMYSQL_PORT=3307\n");

        StandardEnvironment environment = new StandardEnvironment();
        environment.getSystemProperties().put("JWT_SECRET", "from-system");
        Map<String, Object> values = DotEnvEnvironmentPostProcessor.loadValues(envFile);
        values.keySet().removeIf(environment::containsProperty);

        assertThat(values).containsEntry("MYSQL_PORT", "3307").doesNotContainKey("JWT_SECRET");
    }

    @Test
    void parsesQuotedAndExportedValues(@TempDir Path tempDir) throws Exception {
        Path envFile = tempDir.resolve(".env");
        Files.writeString(envFile, "# comment\nexport JWT_SECRET=\"secret-value\"\nEMPTY=\n");

        assertThat(DotEnvEnvironmentPostProcessor.loadValues(envFile))
                .containsEntry("JWT_SECRET", "secret-value")
                .containsEntry("EMPTY", "");
    }
}
