package com.example.delivery.common;

/**
 * 转义 SQL LIKE 模式中的通配符，避免调用方把 {@code %}、{@code _} 当成模式语法。
 * 列表和搜索接口的 keyword 直接来自用户输入：不转义时 {@code %} 会匹配全表，
 * 既扩大可见数据范围，也容易触发无索引扫描。转义后按 MySQL 默认的反斜杠转义规则匹配字面量。
 */
public final class SqlLikeEscaper {

    private SqlLikeEscaper() {
    }

    /**
     * 转义 LIKE 模式中的 {@code \}、{@code %} 和 {@code _}，其余字符原样保留。
     *
     * @param value 用户输入的关键字，可为 {@code null}
     * @return 可安全传入 LIKE 的模式片段；入参为 {@code null} 时返回 {@code null}
     */
    public static String escape(String value) {
        if (value == null) {
            return null;
        }
        // 反斜杠必须先转义，否则会把后面新加的转义符再转义一次。
        return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
