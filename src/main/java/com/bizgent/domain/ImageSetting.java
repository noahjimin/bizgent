package com.bizgent.domain;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/** 3-2단계 이미지 설정 (IMAGE_SETTING) */
@Getter
@Setter
public class ImageSetting {

    private String mood = "";
    private String composition = "";
    private String referenceImageUrl = "";
    /** null이면 아직 3-2단계를 거치지 않은 상태 */
    private String purpose;
    private String product;
    private boolean brandColorOn = true;
    /** 채널별 이미지 비율 */
    private Map<String, String> ratios = new LinkedHashMap<>();
    private String extraRequest = "";
}
