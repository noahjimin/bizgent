package com.bizgent.mapper.row;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** CAMPAIGN_PRODUCT 한 줄 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignProductRow {

    private Long campaignId;
    private Long productId;
}
