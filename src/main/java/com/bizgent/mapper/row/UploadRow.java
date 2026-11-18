package com.bizgent.mapper.row;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** SALES_UPLOAD 한 줄 (저장용) */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadRow {

    private Long uploadId;
    private Long storeId;
    private String inputType;
    private String fileName;
    private int recordCount;
    private LocalDate dataFrom;
    private LocalDate dataTo;
    private LocalDateTime uploadedAt;
}
