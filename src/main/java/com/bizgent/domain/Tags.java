package com.bizgent.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** "20대, 30대, 직장인"처럼 쉼표로 구분해 저장하는 태그 값 변환 */
public final class Tags {

    private Tags() {
    }

    public static List<String> split(String value) {
        if (value == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.stream(value.split("[,·/]")).map(String::trim).filter(s -> !s.isEmpty()).toList());
    }

    public static String join(List<String> tags) {
        return String.join(", ", tags);
    }
}
