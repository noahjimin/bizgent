package com.bizgent.domain;

import lombok.Getter;
import lombok.Setter;

/** 요금제 (PRICE_PLAN). 무료·구독 한도는 이 테이블 한 곳에서 정한다. */
@Getter
@Setter
public class PricePlan {

    private Plan planCode;
    private String planName;
    private int monthlyPrice;
    /** 월 캠페인 이용 횟수 (캠페인 시작 시 1회 차감) */
    private int monthlyCampaignLimit;
    /** 캠페인당 게시 채널 최대 수 */
    private int maxChannelCount;
    /** 월 매장 대표 이미지 생성 횟수 (한 번 만들기 = 3장 = 1회) */
    private int monthlyImageLimit;
}
