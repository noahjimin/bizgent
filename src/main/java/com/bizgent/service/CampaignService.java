package com.bizgent.service;

import java.text.Collator;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bizgent.domain.Campaign;
import com.bizgent.domain.Member;
import com.bizgent.mapper.CampaignMapper;
import com.bizgent.mapper.InsightMapper;
import com.bizgent.mapper.MemberMapper;

import lombok.RequiredArgsConstructor;

/** 캠페인 시작·조회·보관함 (F03, F15): CAMPAIGN, MONTHLY_USAGE */
@Service
@RequiredArgsConstructor
public class CampaignService {

    /** 1단계 마케팅 목표 (GOAL 코드 테이블) */
    public static final List<String> GOAL_OPTIONS = List.of("신메뉴/신상품 홍보", "재방문 유도", "매장 인지도 확대", "할인/이벤트 홍보",
            "신규 고객 유치", "재고 소진 프로모션", "비인기 메뉴 살리기");
    public static final String CUSTOM_GOAL = "기타 (직접 입력)";

    private static final DateTimeFormatter YYYYMM = DateTimeFormatter.ofPattern("yyyyMM");

    private final UserSession session;
    private final RequestData data;
    private final CampaignRepository repository;
    private final CampaignMapper campaignMapper;
    private final MemberMapper memberMapper;
    private final InsightMapper insightMapper;

    /** 최근 수정 순 */
    public List<Campaign> findAll() {
        return data.campaigns().stream()
                .sorted(Comparator.comparing(Campaign::getTimestamp, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public List<Campaign> findRecent(int limit) {
        return findAll().stream().limit(limit).toList();
    }

    public Campaign find(Long campaignId) {
        return data.campaigns().stream()
                .filter(c -> c.getCampaignId().equals(campaignId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("캠페인을 찾을 수 없습니다: " + campaignId));
    }

    /**
     * 새 캠페인을 만들고 위자드에서 작성 중인 캠페인으로 지정한다.
     * 무료·구독 모두 캠페인을 시작할 때마다 이번 달 이용 횟수(MONTHLY_USAGE) 1회를 쓰고, 그 달을 CAMPAIGN.USAGE_MONTH에 남긴다.
     * 이어서 작성할 때는 resume()이라 다시 차감하지 않는다.
     *
     * @return 만든 캠페인. 이번 달 이용 횟수를 모두 썼으면 null
     */
    @Transactional
    public Campaign start(CampaignPrefill prefill) {
        Member member = data.member();
        if (member.isCampaignLimitReached()) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        String month = now.format(YYYYMM);

        Campaign campaign = new Campaign();
        campaign.setCreatedAt(now);
        campaign.setUpdatedAt(now);
        campaign.setUsageMonth(month);
        if (prefill != null && !prefill.isEmpty()) {
            String goal = prefill.goal() == null ? "" : prefill.goal();
            campaign.setGoalOption(goal);
            // '기타 (직접 입력)'은 선택만 해 두고 목표 문구는 1단계에서 직접 입력받는다.
            if (!goal.isEmpty() && !CUSTOM_GOAL.equals(goal)) {
                campaign.setGoal(goal);
                campaign.setTitle(goal);
            }
            if (prefill.productId() != null && data.store().findProduct(prefill.productId()) != null) {
                campaign.setProductIds(new ArrayList<>(List.of(prefill.productId())));
            }
            if (!prefill.channels().isEmpty()) {
                campaign.getContent().setChannels(new ArrayList<>(prefill.channels()));
            }
            if ("insight".equals(prefill.source())) {
                campaign.setInsightId(insightMapper.findLatestId(data.store().getStoreId()));
            }
        }
        repository.insert(campaign, data.store().getStoreId());
        memberMapper.addUsage(member.getMemberId(), month);
        session.setActiveDraftId(campaign.getCampaignId());
        data.clear();
        return campaign;
    }

    /** 보관함에서 작성 중인 캠페인을 이어서 만든다. */
    public Campaign resume(Long campaignId) {
        Campaign campaign = find(campaignId);
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
        return data.campaigns().stream()
                .filter(c -> "published".equals(status) ? c.isPublished() : !"draft".equals(status) || !c.isPublished())
                .filter(c -> q.isEmpty() || c.getTitle().toLowerCase().contains(q))
                .sorted(order)
                .toList();
    }

    /**
     * 캠페인 삭제. 대상 메뉴·채널·기획·이미지 설정·시안·해시태그·게시·반응·알림은 FK ON DELETE CASCADE로 함께 지워진다.
     * AI 개선 방향 분석(INSIGHT_CAMPAIGN)에 쓰인 캠페인은 지난 분석 기록을 지키기 위해 지우지 않는다.
     *
     * @return 지웠으면 true, 분석 기록에 쓰여 지우지 못했으면 false
     */
    @Transactional
    public boolean delete(Long campaignId) {
        find(campaignId);
        if (campaignMapper.countInsightLinks(campaignId) > 0) {
            return false;
        }
        campaignMapper.deleteCampaign(data.store().getStoreId(), campaignId);
        if (campaignId.equals(session.getActiveDraftId())) {
            session.setActiveDraftId(null);
        }
        data.clear();
        return true;
    }

    public void rename(Long campaignId, String title) {
        if (title == null || title.isBlank()) {
            return;
        }
        Campaign campaign = find(campaignId);
        campaign.setTitle(title.trim());
        campaign.setUpdatedAt(LocalDateTime.now());
        repository.save(campaign);
    }

    /** 캠페인 시작 시 미리 선택할 값 (AI 개선 방향 제안, 홈 빠른 작업) */
    public record CampaignPrefill(String goal, Long productId, List<String> channels, String source) {

        public boolean isEmpty() {
            return (goal == null || goal.isBlank()) && productId == null && channels.isEmpty();
        }
    }
}
