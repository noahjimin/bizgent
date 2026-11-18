package com.bizgent.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/**
 * 회원 (MEMBERS). 현재 요금제는 SUBSCRIPTION의 ACTIVE 행(없으면 FREE), 이번 달 캠페인 이용 횟수는 MONTHLY_USAGE,
 * 매장 이미지 생성 횟수는 STORE_IMAGE의 생성 시각 묶음 수에서 읽는다.
 */
@Getter
@Setter
public class Member {

    private Long memberId;
    private String email;
    /** BCrypt 해시 */
    private String password;
    private String memberName;
    private String phone;
    private boolean termsAgreed;
    private LocalDateTime termsAgreedAt;
    /** ACTIVE / WITHDRAWN */
    private String memberStatus;
    private Plan plan = Plan.FREE;
    private PricePlan pricePlan;
    private int campaignsUsedThisMonth;
    private int storeImagesUsedThisMonth;

    public boolean isFree() {
        return plan == Plan.FREE;
    }

    /** 이용 횟수는 매월 1일 초기화 (MONTHLY_USAGE가 달마다 새 행) */
    public LocalDate getUsageResetAt() {
        return LocalDate.now().withDayOfMonth(1).plusMonths(1);
    }

    public int getMonthlyCampaignLimit() {
        return pricePlan.getMonthlyCampaignLimit();
    }

    public int getChannelLimit() {
        return pricePlan.getMaxChannelCount();
    }

    /** 이번 달 사용 횟수 (한도를 넘지 않게 표시) */
    public int getUsedThisMonth() {
        return Math.min(campaignsUsedThisMonth, getMonthlyCampaignLimit());
    }

    /** 이번 달 이용률(%) */
    public int getUsagePercent() {
        return (int) Math.round(getUsedThisMonth() * 100.0 / getMonthlyCampaignLimit());
    }

    public int getRemainingCampaigns() {
        return Math.max(0, getMonthlyCampaignLimit() - campaignsUsedThisMonth);
    }

    /** 이번 달 캠페인 이용 횟수를 모두 사용했는지 여부 (무료·구독 공통) */
    public boolean isCampaignLimitReached() {
        return campaignsUsedThisMonth >= getMonthlyCampaignLimit();
    }

    public int getRemainingStoreImages() {
        return Math.max(0, pricePlan.getMonthlyImageLimit() - storeImagesUsedThisMonth);
    }

    public String getInitial() {
        return memberName == null || memberName.isEmpty() ? "?" : memberName.substring(0, 1);
    }
}
