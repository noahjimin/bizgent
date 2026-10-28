package com.bizgent.domain;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

/** 회원 (MEMBERS). 요금제·크레딧은 DB 단계에서 SUBSCRIPTION / CREDIT_HISTORY로 분리된다. */
@Getter
@Setter
public class Member {

    private Long memberId;
    private String email;
    private String password;
    private String memberName;
    private String phone;
    private boolean termsAgreed;
    private Plan plan = Plan.FREE;
    private int credits;
    private int campaignsUsedThisMonth;
    private LocalDate usageResetAt;
    private int monthlyCampaignLimit = 3;

    public boolean isFree() {
        return plan == Plan.FREE;
    }

    /** 이번 달 사용 횟수 (한도를 넘지 않게 표시) */
    public int getUsedThisMonth() {
        return Math.min(campaignsUsedThisMonth, monthlyCampaignLimit);
    }

    /** 이번 달 이용률(%) */
    public int getUsagePercent() {
        return (int) Math.round(getUsedThisMonth() * 100.0 / monthlyCampaignLimit);
    }

    public int getRemainingCampaigns() {
        return Math.max(0, monthlyCampaignLimit - campaignsUsedThisMonth);
    }

    /** 무료 플랜에서 이번 달 캠페인 이용 횟수를 모두 사용했는지 여부 */
    public boolean isCampaignLimitReached() {
        return isFree() && campaignsUsedThisMonth >= monthlyCampaignLimit;
    }

    public String getInitial() {
        return memberName == null || memberName.isEmpty() ? "?" : memberName.substring(0, 1);
    }
}
