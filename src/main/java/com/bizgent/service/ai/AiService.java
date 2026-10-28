package com.bizgent.service.ai;

import java.util.List;

import com.bizgent.domain.AiPlan;
import com.bizgent.domain.Campaign;
import com.bizgent.domain.Draft;
import com.bizgent.domain.Sale;
import com.bizgent.domain.Store;

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

    /** AI 캠페인 개선 방향 (F19). view: early(데이터 적음) / enough(충분) */
    Insights generateInsights(Store store, String view, List<Campaign> campaigns, List<Sale> sales);

    record EditedImage(String imageUrl, String description) {
    }

    record GeneratedImage(String id, String imageUrl, String alt) {
    }

    record StoreImageRequest(String mood, String scene, Long productId, String extraRequest) {
    }

    /** 개선 제안 카드. goal·productId는 "이 제안으로 새 캠페인 만들기" 시 1단계에 미리 선택된다. */
    record Suggestion(String id, String title, String desc, String goal, Long productId) {
    }

    record CampaignComparison(Long campaignId, String title, String channel, int views, int clicks, int conversions,
            double conversionRate, String reason) {
    }

    record Insights(String view, String headline, String summary, List<Suggestion> strengths,
            List<Suggestion> improvements, List<Suggestion> slowMenus, List<Suggestion> stockPromotions,
            CampaignComparison good, CampaignComparison bad) {
    }
}
