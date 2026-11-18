package com.bizgent.mapper.row;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** CAMPAIGN 한 줄 (Y/N 컬럼은 boolean) */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignRow {

    private Long campaignId;
    private Long storeId;
    private String goalCode;
    private String customGoal;
    private String goalName;
    private Long insightId;
    private String campaignName;
    private String targetCustomer;
    private String toneManner;
    private String extraNote;
    private boolean hashtagOn;
    private boolean captionOn;
    private boolean imageOn;
    private String currentStep;
    private String lockedStep;
    private String usageMonth;
    private String campaignStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
}
