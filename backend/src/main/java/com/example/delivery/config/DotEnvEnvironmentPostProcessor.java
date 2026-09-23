package com.example.delivery.config;

import java.io.IOException;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * 在 Spring 创建配置 Bean 前查找并加载仓库根目录附近的 .env，统一 IDEA、Maven 和部署启动行为。
 * 类由 META-INF/spring.factories 注册；进程环境变量、JVM 参数等已有配置拥有更高优先级。
 */
public class DotEnvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "deliveryDotEnv";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path envFile = findEnvFile(Path.of("").toAbsolutePath());
        if (envFile == null) {
            return;
        }

        Map<String, Object> values = loadValues(envFile);
        // .env 只作为低优先级兜底，不能覆盖显式传入的环境变量或命令行参数。
        values.keySet().removeIf(environment::containsProperty);
        if (!values.isEmpty()) {
            environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, values));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    static Path findEnvFile(Path startDirectory) {
        Path current = startDirectory.toAbsolutePath().normalize();
        while (current != null) {
            Path candidate = current.resolve(".env");
            if (Files.isRegularFile(candidate) && Files.isReadable(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        return null;
    }

    static Map<String, Object> loadValues(Path envFile) {
        Map<String, Object> values = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(envFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                // 兼容 shell 风格的 export KEY=value，但不执行变量展开或任意 shell 语法。
                if (trimmed.startsWith("export ")) {
                    trimmed = trimmed.substring("export ".length()).trim();
                }
                int separator = trimmed.indexOf('=');
                if (separator <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, separator).trim();
                String value = unquote(trimmed.substring(separator + 1).trim());
                values.put(key, value);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read " + envFile, exception);
        }
        return values;
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '\"' && last == '\"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        int comment = value.indexOf(" #");
        return comment >= 0 ? value.substring(0, comment).trim() : value;
    }
}
