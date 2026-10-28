package com.bizgent.domain;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/** 3-1단계 콘텐츠 설정 (CAMPAIGN.HASHTAG_YN·AD_IMAGE_YN, CAMPAIGN_CHANNEL) */
@Getter
@Setter
public class ContentSetting {

    private List<String> channels = new ArrayList<>();
    private boolean hashtagOn;
    private boolean imageOn;
    private boolean captionOn = true;
    private String customChannelNote = "";
}
