package com.bizgent.domain;

import java.time.LocalDateTime;

/** 매출 업로드 기록 (SALES_UPLOAD). inputType: excel / receipt / manual */
public record SalesUpload(Long uploadId, LocalDateTime uploadedAt, String inputType, int rowCount) {
}
