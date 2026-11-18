package com.bizgent.mapper.row;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** INSIGHT_ITEM 한 줄 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightItemRow {

    private Long itemId;
    private Long insightId;
    private String itemCategory;
    private int sortOrder;
    private String title;
    private String content;
    private String evidence;
    private boolean evidenceSample;
    private String goalCode;
    private Long productId;
}
