package com.bizgent.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.bizgent.domain.Campaign;
import com.bizgent.domain.Sale;
import com.bizgent.service.SalesAnalysisService.CampaignRow;

/** 개선 방향 비교 후보: 최근 게시 5개 중 게시 전후 매출이 모두 있는 것 */
class InsightCandidatesTest {

    private static CampaignRow row(long id, LocalDate periodEnd, long before, long after) {
        Campaign c = new Campaign();
        c.setCampaignId(id);
        return new CampaignRow(c, "-", "-", periodEnd, before, after, null, "보통", 0, 0, 0, null);
    }

    @Test
    void 최근_5개_중_게시_전후_매출이_있고_게시_후_7일이_끝난_캠페인만_후보다() {
        LocalDate last = LocalDate.of(2026, 10, 12);
        List<Sale> sales = List.of(new Sale(1L, last, 1L, 1, 1000L, "manual"));
        List<CampaignRow> rows = List.of(
                row(1, last.plusDays(3), 100, 100), // 게시 후 7일이 아직 안 끝남
                row(2, last, 0, 100),               // 게시 전 매출 없음
                row(3, last.minusDays(5), 100, 200),
                row(4, last.minusDays(10), 100, 200),
                row(5, last.minusDays(15), 100, 200),
                row(6, last.minusDays(20), 100, 200)); // 최근 5개 밖

        List<Long> ids = InsightService.candidates(rows, sales).stream().map(r -> r.campaign().getCampaignId()).toList();

        assertEquals(List.of(3L, 4L, 5L), ids);
        assertEquals(List.of(), InsightService.candidates(rows, List.of()), "매출이 없으면 후보 없음");
    }
}
