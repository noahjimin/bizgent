package com.bizgent.service.ai;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.bizgent.domain.AiPlan;
import com.bizgent.domain.Campaign;
import com.bizgent.domain.Draft;
import com.bizgent.domain.Sale;
import com.bizgent.domain.Store;
import com.bizgent.service.SalesAnalysisService.CampaignRow;

/**
 * AI 기능 모음 (LLM: 기획·문구·개선 방향, 이미지 생성: 광고·대표 이미지).
 * 지금은 {@link MockAiService}가 정해진 응답을 돌려주고, 실제 API 연동 시 이 인터페이스의 구현체를 추가한다.
 */
public interface AiService {

    /** 2단계 AI 기획 (F04) */
    AiPlan generatePlan(Store store, Campaign campaign);

    /** 구독 사용자의 방향 직접 입력 재분석 (F04) */
    AiPlan reanalyzePlan(Store store, Campaign campaign, String feedback);

    /** 4단계 시안 A/B 생성 (F05, F07, F11) */
    List<Draft> generateDrafts(Store store, Campaign campaign);

    /** 시안 수정 모달의 AI 문구 대안 (구독, F16) */
    List<String> suggestCopyAlternatives(Store store, Draft draft);

    /** 시안 수정 모달의 AI 이미지 수정 (구독, F16) */
    EditedImage editImage(String imageUrl, String instruction, String mood);

    /** AI 매장 대표 이미지 3장 생성 (F02) */
    List<GeneratedImage> generateStoreImages(Store store, StoreImageRequest request);

    /**
     * AI 캠페인 개선 방향 (F19).
     * @param candidates 비교 후보 캠페인 (최근 게시 순, 게시 전후 매출이 있는 것)
     * @param moreNeeded Good/Bad 비교까지 더 필요한 캠페인 수. 0이면 비교, 아니면 초기 분석
     * @param sales 분석에 사용하는 매출
     */
    Insights generateInsights(Store store, List<CampaignRow> candidates, int moreNeeded, List<Sale> sales);

    record EditedImage(String imageUrl, String description) {
    }

    record GeneratedImage(String id, String imageUrl, String alt) {
    }

    record StoreImageRequest(String mood, String scene, Long productId, String extraRequest) {
    }

    /**
     * 개선 제안 카드. evidence: 근거 수치 배지 (sample이면 예시 데이터 기반),
     * goal·productId는 "캠페인으로 만들기" 시 1단계에 미리 선택된다.
     */
    record Suggestion(String id, String title, String desc, String evidence, boolean sample, String goal,
            Long productId) {
    }

    /** 분석 대상 캠페인. role: 대상 / Good / Bad */
    record CampaignBrief(Long campaignId, String title, LocalDateTime publishedAt, String channels, String imageUrl,
            String role) {
    }

    /** Good/Bad 카드. salesChange: 대상 메뉴 매출 증가율(게시 전 7일 대비 게시 후 7일), 반응 수치는 예시 데이터 */
    record CampaignComparison(Long campaignId, String title, String channels, String menuNames, Integer salesChange,
            long before, long after, int likes, int comments, int shares, int saves, int reach, String reason) {
    }

    /** 두 캠페인 지표 막대. goodPct·badPct: 둘 중 큰 값을 100으로 본 막대 길이 */
    record MetricBar(String label, String goodText, String badText, int goodPct, int badPct, boolean sample) {
    }

    /** "무엇이 달랐나요?" 표의 한 줄 */
    record Difference(String item, String good, String bad) {
    }

    /** 결론 문장과 "이 내용으로 캠페인 만들기"에 넘길 목표·메뉴 */
    record Conclusion(String text, String goal, Long productId) {
    }

    /** view: early(초기 분석) / enough(Good·Bad 비교). targets가 비면 분석할 캠페인이 없는 상태 */
    record Insights(String view, String headline, String summary, List<CampaignBrief> targets,
            LocalDate salesFrom, LocalDate salesTo, List<Suggestion> strengths, List<Suggestion> improvements,
            List<Suggestion> slowMenus, List<Suggestion> stockPromotions, CampaignComparison good,
            CampaignComparison bad, List<MetricBar> metrics, List<Difference> differences, Conclusion conclusion) {
    }
}
