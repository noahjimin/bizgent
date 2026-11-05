package com.bizgent.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.bizgent.demo.DemoSession;
import com.bizgent.domain.Campaign;

class CampaignResumeTest {

    @Test
    void newCampaignKeepsPreviousDraftAndResumeDoesNotChargeAgain() {
        DemoSession session = new DemoSession();
        CampaignService service = new CampaignService(session);
        session.getMember().setCampaignsUsedThisMonth(0);

        Campaign first = service.start(null);
        first.setCurrentStep("3-1");
        Campaign second = service.start(null);
        assertEquals(2, session.getMember().getCampaignsUsedThisMonth(), "새 캠페인은 시작할 때마다 1회 차감");
        assertTrue(session.getCampaigns().contains(first), "작성하던 캠페인은 보관함에 남는다");
        assertEquals(second.getCampaignId(), session.getActiveDraftId());

        Campaign resumed = service.resume(first.getCampaignId());
        assertEquals(2, session.getMember().getCampaignsUsedThisMonth(), "이어서 작성하기는 차감하지 않는다");
        assertEquals(first.getCampaignId(), session.getActiveDraftId());
        assertEquals("3-1", resumed.getContinueStep(), "마지막으로 보던 단계로 돌아온다");
    }

    @Test
    void continueStepFallsBackToDataWhenNoStepRecorded() {
        Campaign campaign = new Campaign();
        assertEquals("1", campaign.getContinueStep());
    }
}
