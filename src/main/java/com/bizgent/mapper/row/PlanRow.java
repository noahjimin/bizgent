package com.bizgent.mapper.row;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** AI_PLAN 한 줄 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanRow {

    private Long aiPlanId;
    private Long campaignId;
    private Integer versionNo;
    private String planSource;
    private String direction;
    private String targetAnalysis;
    private String ageGroup;
    private String strategy;
    private String tones;
    private String recommendedChannels;
    private String expectedEffect;
    private String reach;
    private String frequency;
    private String duration;
    private String userRequest;
    private boolean confirmed;
}
