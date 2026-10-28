package com.bizgent.web;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.stereotype.Component;

/** 화면 표시용 한국어 날짜·금액 포맷. 템플릿에서 ${@fmt.monthDay(date)}로 사용 */
@Component("fmt")
public class Fmt {

    private static final DateTimeFormatter DOT_DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    /** 10월 5일 */
    public String monthDay(LocalDateTime dateTime) {
        return dateTime == null ? "" : monthDay(dateTime.toLocalDate());
    }

    public String monthDay(LocalDate date) {
        return date == null ? "" : date.getMonthValue() + "월 " + date.getDayOfMonth() + "일";
    }

    /** 10월 5일 오전 9:00 */
    public String monthDayTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        int hour = dateTime.getHour();
        int h12 = hour % 12 == 0 ? 12 : hour % 12;
        return String.format("%s %s %d:%02d", monthDay(dateTime), hour < 12 ? "오전" : "오후", h12, dateTime.getMinute());
    }

    /** 2025.10.05 */
    public String dotDate(LocalDate date) {
        return date == null ? "" : date.format(DOT_DATE);
    }

    public String dotDate(LocalDateTime dateTime) {
        return dateTime == null ? "" : dotDate(dateTime.toLocalDate());
    }

    /** 시안 카드의 채널 표시: "인스타그램용" 또는 "기타 · 직접 입력한 분위기" */
    public String channelLabel(String channel, String note, String suffix) {
        if ("기타".equals(channel)) {
            return "기타 · " + (note == null || note.isBlank() ? "직접 입력한 분위기" : note.trim());
        }
        return channel + suffix;
    }

    /** ①②③ */
    public String circled(int index) {
        return index < 3 ? String.valueOf("①②③".charAt(index)) : String.valueOf(index + 1);
    }

    /** 12,345 */
    public String number(long value) {
        return NumberFormat.getIntegerInstance(Locale.KOREA).format(value);
    }

    /** 12,345원 */
    public String won(long value) {
        return number(value) + "원";
    }
}
