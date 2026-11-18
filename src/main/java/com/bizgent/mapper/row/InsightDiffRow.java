package com.bizgent.mapper.row;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** INSIGHT_DIFF 한 줄 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightDiffRow {

    private Long insightId;
    private int diffSeq;
    private String itemName;
    private String goodValue;
    private String badValue;
}
