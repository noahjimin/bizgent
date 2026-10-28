package com.bizgent.service;

import java.text.Collator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.bizgent.demo.DemoSession;
import com.bizgent.domain.Campaign;
import com.bizgent.domain.Member;

import lombok.RequiredArgsConstructor;

/** 캠페인 조회·생성·삭제 (F13 캠페인 보관함, F14 이용 한도) */
@Service
@RequiredArgsConstructor
public class CampaignService {

    /** 1단계 마케팅 목표 (GOAL 코드 테이블) */
    public static final List<String> GOAL_OPTIONS = List.of("신메뉴/신상품 홍보", "재방문 유도", "매장 인지도 확대", "할인/이벤트 홍보",
            "신규 고객 유치", "재고 소진 프로모션", "비인기 메뉴 살리기");
    public static final String CUSTOM_GOAL = "기타 (직접 입력)";

    private final DemoSession session;

    /** 최근 수정 순 */
    public List<Campaign> findAll() {
        return session.getCampaigns().stream()
                .sorted(Comparator.comparing(Campaign::getTimestamp, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public List<Campaign> findRecent(int limit) {
        return findAll().stream().limit(limit).toList();
    }

    public Campaign find(Long campaignId) {
        return session.getCampaigns().stream()
                .filter(c -> c.getCampaignId().equals(campaignId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("캠페인을 찾을 수 없습니다: " + campaignId));
    }

    /**
     * 새 캠페인을 만들고 위자드에서 작성 중인 캠페인으로 지정한다.
     * 무료 플랜은 캠페인을 시작할 때마다 월 무료 이용 1회를 사용한다.
     *
     * @return 만든 캠페인. 무료 이용 횟수를 모두 썼으면 null
     */
    public Campaign start(CampaignPrefill prefill) {
        Member member = session.getMember();
        if (member.isCampaignLimitReached()) {
            return null;
        }
        if (member.isFree()) {
            member.setCampaignsUsedThisMonth(
                    Math.min(member.getMonthlyCampaignLimit(), member.getCampaignsUsedThisMonth() + 1));
        }

        LocalDateTime now = LocalDateTime.now();
        Campaign campaign = new Campaign();
        campaign.setCampaignId(session.nextId());
        campaign.setCreatedAt(now);
        campaign.setUpdatedAt(now);
        if (prefill != null && !prefill.isEmpty()) {
            String goal = prefill.goal() == null ? "" : prefill.goal();
            campaign.setGoalOption(goal);
            campaign.setGoal(goal);
            if (!goal.isEmpty()) {
                campaign.setTitle(goal);
            }
            if (prefill.productId() != null) {
                campaign.setProductIds(new ArrayList<>(List.of(prefill.productId())));
            }
            if (!prefill.channels().isEmpty()) {
                campaign.getContent().setChannels(new ArrayList<>(prefill.channels()));
            }
            campaign.setInsightPrefill(!"quick".equals(prefill.source()));
        }

        // 이전에 작성하던 캠페인은 새 캠페인으로 대체한다.
        Long previous = session.getActiveDraftId();
        session.getCampaigns().removeIf(c -> c.getCampaignId().equals(previous) && !c.isPublished());
        session.getCampaigns().add(campaign);
        session.setActiveDraftId(campaign.getCampaignId());
        return campaign;
    }

    /** 보관함에서 작성 중인 캠페인을 이어서 만든다. */
    public Campaign resume(Long campaignId) {
        Campaign campaign = find(campaignId);
        // 목표 옵션이 없는 이전 데이터는 1단계 입력값을 채워 준다.
        if (campaign.getGoalOption().isEmpty() && !campaign.getGoal().isEmpty()) {
            if (GOAL_OPTIONS.contains(campaign.getGoal())) {
                campaign.setGoalOption(campaign.getGoal());
            } else {
                campaign.setGoalOption(CUSTOM_GOAL);
                campaign.setCustomGoal(campaign.getGoal());
            }
        }
        session.setActiveDraftId(campaign.getCampaignId());
        return campaign;
    }

    /** 보관함 목록: 검색어·상태 필터·정렬 적용 */
    public List<Campaign> search(String query, String status, String sort) {
        String q = query == null ? "" : query.trim().toLowerCase();
        Comparator<Campaign> byTime = Comparator.comparing(Campaign::getTimestamp,
                Comparator.nullsLast(Comparator.naturalOrder()));
        Comparator<Campaign> order = switch (sort == null ? "" : sort) {
            case "name" -> Comparator.comparing(Campaign::getTitle, Collator.getInstance(Locale.KOREAN));
            case "oldest" -> byTime;
            default -> byTime.reversed();
        };
        return session.getCampaigns().stream()
                .filter(c -> "published".equals(status) ? c.isPublished() : !"draft".equals(status) || !c.isPublished())
                .filter(c -> q.isEmpty() || c.getTitle().toLowerCase().contains(q))
                .sorted(order)
                .toList();
    }

    public void delete(Long campaignId) {
        session.getCampaigns().removeIf(c -> c.getCampaignId().equals(campaignId));
        session.getNotifications().removeIf(n -> campaignId.equals(n.getCampaignId()));
        if (campaignId.equals(session.getActiveDraftId())) {
            session.setActiveDraftId(null);
        }
    }

    public void rename(Long campaignId, String title) {
        if (title == null || title.isBlank()) {
            return;
        }
        Campaign campaign = find(campaignId);
        campaign.setTitle(title.trim());
        campaign.setUpdatedAt(LocalDateTime.now());
    }

    /** 캠페인 시작 시 미리 선택할 값 (AI 개선 방향 제안, 홈 빠른 작업) */
    public record CampaignPrefill(String goal, Long productId, List<String> channels, String source) {

        public boolean isEmpty() {
            return (goal == null || goal.isBlank()) && productId == null && channels.isEmpty();
        }
    }
}
