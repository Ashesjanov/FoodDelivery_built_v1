package com.example.delivery.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置所有 domain mapper 共用的 MyBatis-Plus 拦截器，并扫描 mapper 接口生成代理实现。
 * service 通过 BaseMapper 执行 CRUD 时会自动应用乐观锁和 MySQL 分页能力。
 */
@Configuration
@MapperScan("com.example.delivery.domain.mapper")
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // version 字段更新失败时返回 0 行，业务层据此处理并发修改冲突。
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        // 防止调用方一次查询超大页面，超过限制的 Page 查询会被截断到 200 条。
        pagination.setMaxLimit(200L);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}
