package com.bizgent.mapper.row;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** IMAGE_SETTING 한 줄 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageSettingRow {

    private Long campaignId;
    private String purpose;
    private String mainProduct;
    private String mood;
    private String composition;
    private boolean brandColorOn;
    private String referenceImageUrl;
    private String extraRequest;
}
