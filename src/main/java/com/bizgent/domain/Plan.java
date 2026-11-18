package com.bizgent.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 요금제 (PRICE_PLAN.PLAN_CODE). 한도 값은 PROJECT.md v0.08 "무료 체험 횟수 및 요금제 구조" 기준 */
@Getter
@RequiredArgsConstructor
public enum Plan {
    FREE(4, 2, 1), PRO(8, 2, 2);

    /** 월 캠페인 이용 횟수 (캠페인 시작 시 1회 차감) */
    private final int monthlyCampaigns;
    /** 캠페인당 게시 채널 최대 수 */
    private final int maxChannels;
    /** 월 매장 대표 이미지 생성 횟수 (한 번 만들기 = 3장 = 1회) */
    private final int monthlyStoreImages;
}
