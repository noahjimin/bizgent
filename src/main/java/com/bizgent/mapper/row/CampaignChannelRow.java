package com.bizgent.mapper.row;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** CAMPAIGN_CHANNEL 한 줄 (+ CHANNEL_NAME) */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignChannelRow {

    private Long campaignId;
    private String channelCode;
    private String channelName;
    private String customChannelName;
    private String customChannelNote;
    private String imageRatio;
}
