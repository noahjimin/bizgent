package com.bizgent.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class LandingSeasonTest {

    @Test
    void seasonByMonth() {
        assertEquals("봄", LandingSeason.of(LocalDate.of(2026, 3, 1)).name());
        assertEquals("여름", LandingSeason.of(LocalDate.of(2026, 8, 31)).name());
        assertEquals("가을", LandingSeason.of(LocalDate.of(2026, 11, 2)).name());
        assertEquals("겨울", LandingSeason.of(LocalDate.of(2026, 12, 1)).name());
        assertEquals("겨울", LandingSeason.of(LocalDate.of(2027, 2, 28)).name());
        assertEquals("11월 가을은 BizGent와 함께해요", LandingSeason.of(LocalDate.of(2026, 11, 2)).phrase());
    }
}
