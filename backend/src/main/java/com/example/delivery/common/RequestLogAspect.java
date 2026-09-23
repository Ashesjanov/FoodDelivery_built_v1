package com.example.delivery.common;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

/** Structured timing and audit logging for REST controllers. */
@Aspect
@Component
public class RequestLogAspect {
    private static final Logger log = LoggerFactory.getLogger(RequestLogAspect.class);

    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object logRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.nanoTime();
        HttpServletRequest request = currentRequest();
        String requestId = requestId(request);
        MDC.put("requestId", requestId);
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String method = signature.getDeclaringType().getSimpleName() + "." + signature.getName();
        String arguments = Arrays.stream(joinPoint.getArgs()).map(RequestLogAspect::safeValue).collect(Collectors.joining(", "));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String user = authentication == null ? "anonymous" : authentication.getName();
        try {
            Object result = joinPoint.proceed();
            log.info("request_id={} principal={} method={} args=[{}] duration_ms={}", requestId, user, method, arguments, elapsedMillis(start));
            return result;
        } catch (Throwable ex) {
            log.warn("request_id={} principal={} method={} args=[{}] duration_ms={} failed={}", requestId, user, method, arguments, elapsedMillis(start), ex.getMessage());
            throw ex;
        } finally {
            MDC.remove("requestId");
        }
    }

    private static HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private static String requestId(HttpServletRequest request) {
        if (request == null) {
            return UUID.randomUUID().toString();
        }
        String supplied = request.getHeader("X-Request-Id");
        return supplied == null || supplied.isBlank() ? UUID.randomUUID().toString() : supplied.replaceAll("[^a-zA-Z0-9._-]", "");
    }

    private static String safeValue(Object value) {
        if (value == null) {
            return "null";
        }
        String text = value.toString();
        return text.replaceAll("(?i)(password|token|secret)=([^,\\s}]+)", "$1=***")
                .replaceAll("(?i)(\"(?:password|token|secret)\"\\s*:\\s*\")[^\"]*(\")", "$1***$2");
    }

    private static long elapsedMillis(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }
}
