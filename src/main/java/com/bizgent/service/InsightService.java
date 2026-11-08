package com.bizgent.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.bizgent.demo.DemoSession;
import com.bizgent.domain.Campaign;
import com.bizgent.domain.InsightReport;
import com.bizgent.domain.Sale;
import com.bizgent.service.SalesAnalysisService.CampaignRow;
import com.bizgent.service.ai.AiService;

import lombok.RequiredArgsConstructor;

/** AI 캠페인 개선 방향 (F19): 분석 기록 저장·조회. 데이터가 바뀌었을 때만 새로 분석한다. */
@Service
@RequiredArgsConstructor
public class InsightService {

    /** 비교 후보는 최근 게시한 캠페인 중 이 개수까지 본다 */
    static final int RECENT_LIMIT = 5;
    /** 후보가 이 개수 이상이어야 Good/Bad 비교를 한다 */
    static final int COMPARE_MIN = 4;
    /** 개발 바로가기 "데이터 적음"일 때 있는 것으로 보는 후보 수 */
    private static final int EARLY_DEMO_CANDIDATES = 2;

    private final DemoSession session;
    private final AiService aiService;
    private final SalesAnalysisService salesAnalysisService;

    /** @param created 이번 요청에서 새로 분석했는지 (화면의 "분석 중" 표시용) */
    public record Current(InsightReport report, boolean created) {
    }

    /** 지금 데이터 기준의 분석. 같은 데이터로 이미 분석했으면 그 기록을 돌려준다. */
    public Current current() {
        String signature = signature();
        InsightReport same = session.getInsightReports().stream()
                .filter(r -> r.getAsOf() == null && signature.equals(r.getSignature()))
                .max(Comparator.comparing(InsightReport::getCreatedAt)).orElse(null);
        if (same != null) {
            return new Current(same, false);
        }
        InsightReport report = new InsightReport(session.nextId(), LocalDateTime.now(), null, signature,
                analyze(null, "early".equals(session.getInsightsView())));
        session.getInsightReports().add(report);
        return new Current(report, true);
    }

    /** @return 없으면 null */
    public InsightReport find(Long reportId) {
        return session.getInsightReports().stream().filter(r -> r.getReportId().equals(reportId))
                .findFirst().map(this::filled).orElse(null);
    }

    /** 최근 분석부터 */
    public List<InsightReport> history() {
        return session.getInsightReports().stream().map(this::filled)
                .sorted(Comparator.comparing(InsightReport::getCreatedAt).reversed()).toList();
    }

    /** 시연용 지난 기록은 그 날짜까지의 데이터로 한 번만 분석해 고정한다. */
    private InsightReport filled(InsightReport r) {
        if (r.getResult() == null) {
            r.setResult(analyze(r.getAsOf(), false));
        }
        return r;
    }

    private AiService.Insights analyze(LocalDate asOf, boolean earlyDemo) {
        List<Sale> sales = session.getSales().stream()
                .filter(s -> asOf == null || !s.getSaleDate().isAfter(asOf)).toList();
        List<Campaign> campaigns = session.getCampaigns().stream()
                .filter(c -> asOf == null || c.getPublishedAt() != null && !c.getPublishedAt().toLocalDate().isAfter(asOf))
                .toList();
        List<CampaignRow> candidates = candidates(salesAnalysisService.campaignResults(campaigns, sales), sales);
        if (earlyDemo && candidates.size() > EARLY_DEMO_CANDIDATES) {
            // 데이터가 적은 매장처럼 보이도록 가장 오래된 후보만 남긴다 (최근 게시 순 유지)
            candidates = candidates.subList(candidates.size() - EARLY_DEMO_CANDIDATES, candidates.size());
        }
        int moreNeeded = Math.max(0, COMPARE_MIN - candidates.size());
        return aiService.generateInsights(session.getStore(), candidates, moreNeeded, sales);
    }

    /** 최근 게시한 캠페인 5개 중 게시 전 7일과 게시 후 7일 매출이 모두 있는 것 (최근 게시 순) */
    static List<CampaignRow> candidates(List<CampaignRow> results, List<Sale> sales) {
        LocalDate last = sales.stream().map(Sale::getSaleDate).max(Comparator.naturalOrder()).orElse(null);
        return results.stream().limit(RECENT_LIMIT)
                .filter(r -> last != null && !r.periodEnd().isAfter(last) && r.before() > 0 && r.after() > 0)
                .toList();
    }

    private String signature() {
        return Integer.toHexString(Objects.hash(session.getInsightsView(),
                session.getCampaigns().stream().map(c -> c.getCampaignId() + ":" + c.getStatus() + ":" + c.getUpdatedAt()).toList(),
                session.getStore().getProducts().stream().map(p -> p.getProductId() + ":" + p.getStockQty()).toList(),
                session.getSales().size()));
    }
}
