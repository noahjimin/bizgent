package com.bizgent.web;

import java.time.LocalDate;

/**
 * 랜딩 히어로의 계절 색. 팀 이름 "사계절"의 뜻(사계절 내내 함께)을 담아 접속한 달의 계절 색을 쓴다.
 * 화면설계서 범위 밖으로 팀이 정한 추가 요소다.
 */
public record LandingSeason(int month, String name, String bgFrom, String bgTo, String accent) {

    public static LandingSeason of(LocalDate date) {
        int m = date.getMonthValue();
        if (m >= 3 && m <= 5) {
            return new LandingSeason(m, "봄", "#ffe4ef", "#fff6fa", "#be185d");
        }
        if (m >= 6 && m <= 8) {
            return new LandingSeason(m, "여름", "#d9f7e3", "#f3fdf6", "#166534");
        }
        if (m >= 9 && m <= 11) {
            return new LandingSeason(m, "가을", "#ffe6d2", "#fff7f0", "#c2410c");
        }
        return new LandingSeason(m, "겨울", "#dbeafe", "#f3f8ff", "#1d4ed8");
    }

    /** 히어로 위쪽 문구, 예: "11월 가을은 BizGent와 함께해요" */
    public String phrase() {
        return month + "월 " + name + "은 BizGent와 함께해요";
    }
}
