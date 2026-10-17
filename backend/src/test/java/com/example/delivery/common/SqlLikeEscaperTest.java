package com.example.delivery.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SqlLikeEscaper} 的单元测试，覆盖通配符、反斜杠、普通文本和空值。
 * 转义结果会作为绑定参数传给 LIKE，因此这里只需断言字符层面的替换顺序。
 */
class SqlLikeEscaperTest {

    @Test
    void escapesPercentAndUnderscore() {
        assertThat(SqlLikeEscaper.escape("%")).isEqualTo("\\%");
        assertThat(SqlLikeEscaper.escape("_")).isEqualTo("\\_");
        assertThat(SqlLikeEscaper.escape("100%")).isEqualTo("100\\%");
        assertThat(SqlLikeEscaper.escape("a_b")).isEqualTo("a\\_b");
        assertThat(SqlLikeEscaper.escape("%_%")).isEqualTo("\\%\\_\\%");
    }

    @Test
    void escapesBackslashBeforeWildcards() {
        assertThat(SqlLikeEscaper.escape("a\\b")).isEqualTo("a\\\\b");
        // 输入的反斜杠先变成两个，随后的 % 再加一层转义，避免被二次转义。
        assertThat(SqlLikeEscaper.escape("\\%")).isEqualTo("\\\\\\%");
    }

    @Test
    void keepsOrdinaryText() {
        assertThat(SqlLikeEscaper.escape("招牌炒饭")).isEqualTo("招牌炒饭");
        assertThat(SqlLikeEscaper.escape("")).isEmpty();
        assertThat(SqlLikeEscaper.escape("spicy chicken")).isEqualTo("spicy chicken");
    }

    @Test
    void returnsNullForNullInput() {
        assertThat(SqlLikeEscaper.escape(null)).isNull();
    }
}
