package com.bizgent.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** 회원 (MEMBERS). 요금제는 DB 단계에서 SUBSCRIPTION으로, 월 이용 횟수는 MONTHLY_USAGE로 분리된다. */
@Getter
@Setter
public class Member {

    private Long memberId;
    private String email;
    private String password;
    private String memberName;
    private String phone;
    private boolean termsAgreed;
    private LocalDateTime termsAgreedAt;
    private Plan plan = Plan.FREE;
    private int campaignsUsedThisMonth;
    private int storeImagesUsedThisMonth;
    private LocalDate usageResetAt;

    public boolean isFree() {
        return plan == Plan.FREE;
    }

    public int getMonthlyCampaignLimit() {
        return plan.getMonthlyCampaigns();
    }

    public int getChannelLimit() {
        return plan.getMaxChannels();
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
        return Math.max(0, plan.getMonthlyStoreImages() - storeImagesUsedThisMonth);
    }

    public String getInitial() {
        return memberName == null || memberName.isEmpty() ? "?" : memberName.substring(0, 1);
    }
}
