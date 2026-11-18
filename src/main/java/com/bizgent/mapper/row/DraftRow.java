package com.bizgent.mapper.row;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DRAFT 한 줄 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DraftRow {

    private Long draftId;
    private Long campaignId;
    private String channelCode;
    private String draftType;
    private String copyText;
    private String imageUrl;
    private boolean selected;
}
