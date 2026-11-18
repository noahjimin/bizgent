package com.bizgent.mapper.row;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** INSIGHT 한 줄 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightRow {

    private Long insightId;
    private Long storeId;
    private Long baseUploadId;
    private String insightType;
    private LocalDate salesFrom;
    private LocalDate salesTo;
    private int candidateCount;
    private String compareBasis;
    private String headline;
    private String summary;
    private String conclusion;
    private String conclusionGoalCode;
    private Long conclusionProductId;
    private LocalDateTime createdAt;
}
