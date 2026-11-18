package com.bizgent.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import com.bizgent.domain.Campaign;
import com.bizgent.domain.Member;
import com.bizgent.domain.PricePlan;
import com.bizgent.domain.Store;
import com.bizgent.mapper.CampaignMapper;
import com.bizgent.mapper.InsightMapper;
import com.bizgent.mapper.MemberMapper;

class CampaignResumeTest {

    @Test
    void startChargesOnceAndRecordsUsageMonthResumeDoesNotCharge() {
        UserSession session = new UserSession();
        RequestData data = mock(RequestData.class);
        CampaignRepository repository = mock(CampaignRepository.class);
        MemberMapper members = mock(MemberMapper.class);
        Member member = member(0, 4);
        Store store = new Store();
        store.setStoreId(10L);
        when(data.member()).thenReturn(member);
        when(data.store()).thenReturn(store);
        AtomicLong ids = new AtomicLong(100);
        doAnswer(inv -> {
            inv.<Campaign>getArgument(0).setCampaignId(ids.incrementAndGet());
            return null;
        }).when(repository).insert(any(), eq(10L));
        CampaignService service = new CampaignService(session, data, repository, mock(CampaignMapper.class), members,
                mock(InsightMapper.class));

        Campaign first = service.start(null);
        Campaign second = service.start(null);
        verify(members, times(2)).addUsage(eq(1L), anyString());
        assertEquals(second.getCampaignId(), session.getActiveDraftId());
        assertEquals(java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM")),
                first.getUsageMonth(), "차감한 달을 캠페인에 남긴다 (CAMPAIGN.USAGE_MONTH)");

        first.setCurrentStep("3-1");
        when(data.campaigns()).thenReturn(List.of(first, second));
        Campaign resumed = service.resume(first.getCampaignId());
        verify(members, times(2)).addUsage(eq(1L), anyString());
        assertEquals(first.getCampaignId(), session.getActiveDraftId());
        assertEquals("3-1", resumed.getContinueStep(), "마지막으로 보던 단계로 돌아온다");
    }

    @Test
    void startReturnsNullWhenMonthlyLimitReached() {
        RequestData data = mock(RequestData.class);
        when(data.member()).thenReturn(member(4, 4));
        MemberMapper members = mock(MemberMapper.class);
        CampaignService service = new CampaignService(new UserSession(), data, mock(CampaignRepository.class),
                mock(CampaignMapper.class), members, mock(InsightMapper.class));
        assertNull(service.start(null));
        verify(members, never()).addUsage(any(), any());
    }

    @Test
    void continueStepFallsBackToDataWhenNoStepRecorded() {
        Campaign campaign = new Campaign();
        assertEquals("1", campaign.getContinueStep());
    }

    private static Member member(int used, int limit) {
        PricePlan plan = new PricePlan();
        plan.setMonthlyCampaignLimit(limit);
        Member m = new Member();
        m.setMemberId(1L);
        m.setPricePlan(plan);
        m.setCampaignsUsedThisMonth(used);
        return m;
    }
}
