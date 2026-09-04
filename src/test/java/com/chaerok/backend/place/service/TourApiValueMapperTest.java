package com.chaerok.backend.place.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TourApiValueMapperTest {

    @Test
    @DisplayName("값이 있으면 원래 문자열을 반환한다")
    void valueOrFallbackReturnsValue() {
        String result =
                TourApiValueMapper.valueOrFallback(
                        "공산성",
                        "기본값"
                );

        assertThat(result).isEqualTo("공산성");
    }

    @Test
    @DisplayName("값이 null이면 fallback을 반환한다")
    void valueOrFallbackReturnsFallbackWhenNull() {
        String result =
                TourApiValueMapper.valueOrFallback(
                        null,
                        "기본값"
                );

        assertThat(result).isEqualTo("기본값");
    }

    @Test
    @DisplayName("값이 빈 문자열이면 fallback을 반환한다")
    void valueOrFallbackReturnsFallbackWhenBlank() {
        String result =
                TourApiValueMapper.valueOrFallback(
                        " ",
                        "기본값"
                );

        assertThat(result).isEqualTo("기본값");
    }

    @Test
    @DisplayName("정상 숫자 문자열을 BigDecimal로 변환한다")
    void toBigDecimal() {
        BigDecimal result =
                TourApiValueMapper.toBigDecimal("36.4651");

        assertThat(result)
                .isEqualByComparingTo(new BigDecimal("36.4651"));
    }

    @Test
    @DisplayName("숫자 값이 null이면 null을 반환한다")
    void toBigDecimalReturnsNullWhenNull() {
        BigDecimal result =
                TourApiValueMapper.toBigDecimal(null);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("숫자 값이 빈 문자열이면 null을 반환한다")
    void toBigDecimalReturnsNullWhenBlank() {
        BigDecimal result =
                TourApiValueMapper.toBigDecimal(" ");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("숫자로 변환할 수 없는 값이면 null을 반환한다")
    void toBigDecimalReturnsNullWhenInvalid() {
        BigDecimal result =
                TourApiValueMapper.toBigDecimal("invalid");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("정상 숫자 문자열은 fallback 대신 변환한 값을 반환한다")
    void toBigDecimalOrFallbackReturnsValue() {
        BigDecimal fallback = new BigDecimal("1.0");

        BigDecimal result =
                TourApiValueMapper.toBigDecimalOrFallback(
                        "127.1190",
                        fallback
                );

        assertThat(result)
                .isEqualByComparingTo(new BigDecimal("127.1190"));
    }

    @Test
    @DisplayName("숫자 값이 null이면 fallback을 반환한다")
    void toBigDecimalOrFallbackReturnsFallbackWhenNull() {
        BigDecimal fallback = new BigDecimal("1.0");

        BigDecimal result =
                TourApiValueMapper.toBigDecimalOrFallback(
                        null,
                        fallback
                );

        assertThat(result).isSameAs(fallback);
    }

    @Test
    @DisplayName("숫자 값이 빈 문자열이면 fallback을 반환한다")
    void toBigDecimalOrFallbackReturnsFallbackWhenBlank() {
        BigDecimal fallback = new BigDecimal("1.0");

        BigDecimal result =
                TourApiValueMapper.toBigDecimalOrFallback(
                        " ",
                        fallback
                );

        assertThat(result).isSameAs(fallback);
    }

    @Test
    @DisplayName("숫자로 변환할 수 없는 값이면 fallback을 반환한다")
    void toBigDecimalOrFallbackReturnsFallbackWhenInvalid() {
        BigDecimal fallback = new BigDecimal("1.0");

        BigDecimal result =
                TourApiValueMapper.toBigDecimalOrFallback(
                        "invalid",
                        fallback
                );

        assertThat(result).isSameAs(fallback);
    }
}