package com.bizgent.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bizgent.domain.Campaign;
import com.bizgent.domain.InsightReport;
import com.bizgent.domain.Sale;
import com.bizgent.domain.Store;
import com.bizgent.mapper.SalesMapper;
import com.bizgent.service.SalesAnalysisService.CampaignRow;
import com.bizgent.service.ai.AiService;

import lombok.RequiredArgsConstructor;

/**
 * AI 캠페인 개선 방향 (F19): 분석 기록 저장·조회.
 * 마지막 분석 이후 새 매출 업로드가 없으면(INSIGHT.BASE_UPLOAD_ID 비교) 저장된 분석을 그대로 보여 주고,
 * 새 업로드가 있으면 새로 분석해서 저장한다.
 */
@Service
@RequiredArgsConstructor
public class InsightService {

    /** 비교 후보는 최근 게시한 캠페인 중 이 개수까지 본다 */
    static final int RECENT_LIMIT = 5;
    /** 후보가 이 개수 이상이어야 Good/Bad 비교를 한다 */
    static final int COMPARE_MIN = 4;
    /** 개발 바로가기 "데이터 적음"일 때 있는 것으로 보는 후보 수 */
    private static final int EARLY_DEMO_CANDIDATES = 2;

    private final UserSession session;
    private final RequestData data;
    private final AiService aiService;
    private final SalesAnalysisService salesAnalysisService;
    private final InsightRepository repository;
    private final SalesMapper salesMapper;

    /** @param created 이번 요청에서 새로 분석했는지 (화면의 "분석 중" 표시용) */
    public record Current(InsightReport report, boolean created) {
    }

    /** 분석 결과와 분석에 쓴 비교 후보 */
    public record Analysis(AiService.Insights result, List<CampaignRow> candidates) {
    }

    /** 지금 데이터 기준의 분석. 마지막 분석 이후 새 매출 업로드가 없으면 저장된 기록을 돌려준다. */
    @Transactional
    public Current current() {
        Long baseUploadId = salesMapper.findLatestUploadId(data.store().getStoreId());
        List<InsightReport> history = history();
        if (!history.isEmpty() && Objects.equals(history.get(0).getBaseUploadId(), baseUploadId)) {
            return new Current(history.get(0), false);
        }
        return new Current(analyzeAndSave(baseUploadId), true);
    }

    /** 개발 바로가기에서 데이터 양(early/enough)을 바꿨을 때: 바로 새로 분석해서 저장한다. */
    @Transactional
    public void reanalyze() {
        analyzeAndSave(salesMapper.findLatestUploadId(data.store().getStoreId()));
    }

    /** @return 없으면 null */
    public InsightReport find(Long reportId) {
        return history().stream().filter(r -> r.getReportId().equals(reportId)).findFirst().orElse(null);
    }

    /** 최근 분석부터 */
    public List<InsightReport> history() {
        return repository.findAll(data.store(), data.campaigns());
    }

    private InsightReport analyzeAndSave(Long baseUploadId) {
        Store store = data.store();
        Analysis a = analyze(store, data.campaigns(), data.sales(), null, "early".equals(session.getInsightsView()));
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        Long id = repository.save(store.getStoreId(), baseUploadId, now, a.result(), a.candidates());
        return history().stream().filter(r -> r.getReportId().equals(id)).findFirst().orElseThrow();
    }

    /**
     * @param asOf 이 날짜까지의 캠페인·매출로 분석 (null이면 전체). 시연 데이터의 지난 분석 기록을 만들 때 쓴다.
     * @param earlyDemo 개발 바로가기 "데이터 적음": 비교 후보를 가장 오래된 2개만 남겨 초기 분석을 보여 준다.
     */
    public Analysis analyze(Store store, List<Campaign> allCampaigns, List<Sale> allSales, LocalDate asOf,
            boolean earlyDemo) {
        List<Sale> sales = allSales.stream()
                .filter(s -> asOf == null || !s.getSaleDate().isAfter(asOf)).toList();
        List<Campaign> campaigns = allCampaigns.stream()
                .filter(c -> asOf == null || c.getPublishedAt() != null && !c.getPublishedAt().toLocalDate().isAfter(asOf))
                .toList();
        List<CampaignRow> candidates = candidates(salesAnalysisService.campaignResults(store, campaigns, sales), sales);
        if (earlyDemo && candidates.size() > EARLY_DEMO_CANDIDATES) {
            // 데이터가 적은 매장처럼 보이도록 가장 오래된 후보만 남긴다 (최근 게시 순 유지)
            candidates = candidates.subList(candidates.size() - EARLY_DEMO_CANDIDATES, candidates.size());
        }
        int moreNeeded = Math.max(0, COMPARE_MIN - candidates.size());
        return new Analysis(aiService.generateInsights(store, candidates, moreNeeded, sales), candidates);
    }

    /** 최근 게시한 캠페인 5개 중 게시 전 7일과 게시 후 7일 매출이 모두 있는 것 (최근 게시 순) */
    static List<CampaignRow> candidates(List<CampaignRow> results, List<Sale> sales) {
        LocalDate last = sales.stream().map(Sale::getSaleDate).max(Comparator.naturalOrder()).orElse(null);
        return results.stream().limit(RECENT_LIMIT)
                .filter(r -> last != null && !r.periodEnd().isAfter(last) && r.before() > 0 && r.after() > 0)
                .toList();
    }
}
