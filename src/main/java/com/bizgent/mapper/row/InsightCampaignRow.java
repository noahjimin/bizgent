package com.bizgent.mapper.row;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** INSIGHT_CAMPAIGN 한 줄 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightCampaignRow {

    private Long insightId;
    private Long campaignId;
    private String compareRole;
    private Integer salesChangeRate;
    private long beforeAmount;
    private long afterAmount;
    private Integer likeSum;
    private Integer commentSum;
    private Integer shareSum;
    private Integer saveSum;
    private Integer reachSum;
    private String reason;
}
