package com.bizgent.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 매출 업로드 기록 (SALES_UPLOAD). inputType: excel / receipt / manual, dataFrom~dataTo: 올린 매출의 날짜 범위 */
public record SalesUpload(Long uploadId, LocalDateTime uploadedAt, String inputType, int rowCount,
        LocalDate dataFrom, LocalDate dataTo) {
}
