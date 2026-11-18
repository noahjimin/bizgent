package com.bizgent.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.bizgent.domain.Campaign;
import com.bizgent.domain.InsightReport;
import com.bizgent.domain.Post;
import com.bizgent.domain.Store;
import com.bizgent.mapper.InsightMapper;
import com.bizgent.mapper.row.InsightCampaignRow;
import com.bizgent.mapper.row.InsightDiffRow;
import com.bizgent.mapper.row.InsightItemRow;
import com.bizgent.mapper.row.InsightRow;
import com.bizgent.service.SalesAnalysisService.CampaignRow;
import com.bizgent.service.ai.AiService;
import com.bizgent.service.ai.AiService.CampaignBrief;
import com.bizgent.service.ai.AiService.CampaignComparison;
import com.bizgent.service.ai.AiService.Conclusion;
import com.bizgent.service.ai.AiService.Difference;
import com.bizgent.service.ai.AiService.Insights;
import com.bizgent.service.ai.AiService.Suggestion;

import lombok.RequiredArgsConstructor;

/**
 * AI 캠페인 개선 방향 분석 결과를 INSIGHT·INSIGHT_ITEM·INSIGHT_DIFF·INSIGHT_CAMPAIGN에 저장하고 다시 읽는다.
 * Good/Bad 카드의 반응 합계와 선정 이유도 저장해서 "지난 분석 보기"에서 분석 당시 값 그대로 보인다.
 */
@Component
@RequiredArgsConstructor
public class InsightRepository {

    private static final String COMPARE_BASIS = "대상 메뉴 매출 증가율";

    private final InsightMapper mapper;
    private final CodeService codes;

    /* ---------- 저장 ---------- */

    /** @param candidates 분석에 쓴 비교 후보 (대상 캠페인의 게시 전후 매출을 남기는 데 쓴다) */
    public Long save(Long storeId, Long baseUploadId, LocalDateTime createdAt, Insights r, List<CampaignRow> candidates) {
        boolean compare = "enough".equals(r.view());
        InsightRow row = InsightRow.builder().storeId(storeId).baseUploadId(baseUploadId)
                .insightType(compare ? "COMPARE" : "EARLY").salesFrom(r.salesFrom()).salesTo(r.salesTo())
                .candidateCount(candidates.size()).compareBasis(compare ? COMPARE_BASIS : null)
                .headline(cut(r.headline(), 200)).summary(cut(r.summary(), 1000))
                .conclusion(r.conclusion() == null ? null : cut(r.conclusion().text(), 1000))
                .conclusionGoalCode(r.conclusion() == null ? null : codes.goalCode(r.conclusion().goal()))
                .conclusionProductId(r.conclusion() == null ? null : r.conclusion().productId())
                .createdAt(createdAt).build();
        mapper.insertInsight(row);
        Long id = row.getInsightId();

        saveItems(id, "STRENGTH", r.strengths());
        saveItems(id, "IMPROVEMENT", r.improvements());
        saveItems(id, "SLOW_MENU", r.slowMenus());
        for (int i = 0; i < r.differences().size(); i++) {
            Difference d = r.differences().get(i);
            mapper.insertDiff(new InsightDiffRow(id, i + 1, cut(d.item(), 30), cut(nz(d.good()), 100), cut(nz(d.bad()), 100)));
        }
        if (compare) {
            mapper.insertCampaign(comparisonRow(id, "GOOD", r.good()));
            mapper.insertCampaign(comparisonRow(id, "BAD", r.bad()));
        } else {
            for (CampaignBrief t : r.targets()) {
                CampaignRow c = candidates.stream().filter(x -> x.campaign().getCampaignId().equals(t.campaignId()))
                        .findFirst().orElse(null);
                InsightCampaignRow.InsightCampaignRowBuilder b = InsightCampaignRow.builder().insightId(id)
                        .campaignId(t.campaignId()).compareRole("TARGET");
                if (c != null) {
                    List<Post> posts = c.campaign().getPosts();
                    b.salesChangeRate(c.change()).beforeAmount(c.before()).afterAmount(c.after())
                            .likeSum(posts.stream().mapToInt(Post::getLikes).sum())
                            .commentSum(posts.stream().mapToInt(Post::getComments).sum())
                            .shareSum(posts.stream().mapToInt(Post::getShares).sum())
                            .saveSum(posts.stream().mapToInt(Post::getSaves).sum())
                            .reachSum(posts.stream().mapToInt(Post::getReach).sum());
                }
                mapper.insertCampaign(b.build());
            }
        }
        return id;
    }

    private void saveItems(Long insightId, String category, List<Suggestion> items) {
        for (int i = 0; i < items.size(); i++) {
            Suggestion s = items.get(i);
            mapper.insertItem(InsightItemRow.builder().insightId(insightId).itemCategory(category).sortOrder(i + 1)
                    .title(cut(s.title(), 200)).content(cut(s.desc() == null || s.desc().isBlank() ? "-" : s.desc(), 1000))
                    .evidence(cut(s.evidence(), 100)).evidenceSample(s.sample()).goalCode(codes.goalCode(s.goal()))
                    .productId(s.productId()).build());
        }
    }

    private static InsightCampaignRow comparisonRow(Long insightId, String role, CampaignComparison c) {
        return InsightCampaignRow.builder().insightId(insightId).campaignId(c.campaignId()).compareRole(role)
                .salesChangeRate(c.salesChange()).beforeAmount(c.before()).afterAmount(c.after()).likeSum(c.likes())
                .commentSum(c.comments()).shareSum(c.shares()).saveSum(c.saves()).reachSum(c.reach())
                .reason(cut(c.reason(), 300)).build();
    }

    /* ---------- 조회 ---------- */

    /** 최근 분석부터. 캠페인 제목·채널·메뉴 이름은 지금 캠페인 기록에서 채운다. */
    public List<InsightReport> findAll(Store store, List<Campaign> campaigns) {
        Long storeId = store.getStoreId();
        Map<Long, List<InsightItemRow>> items = mapper.findItems(storeId).stream()
                .collect(Collectors.groupingBy(InsightItemRow::getInsightId));
        Map<Long, List<InsightDiffRow>> diffs = mapper.findDiffs(storeId).stream()
                .collect(Collectors.groupingBy(InsightDiffRow::getInsightId));
        Map<Long, List<InsightCampaignRow>> links = mapper.findCampaigns(storeId).stream()
                .collect(Collectors.groupingBy(InsightCampaignRow::getInsightId));
        Map<Long, Campaign> byId = campaigns.stream().collect(Collectors.toMap(Campaign::getCampaignId, c -> c));

        List<InsightReport> reports = new ArrayList<>();
        for (InsightRow r : mapper.findInsights(storeId)) {
            List<InsightItemRow> its = items.getOrDefault(r.getInsightId(), List.of());
            List<InsightCampaignRow> cs = links.getOrDefault(r.getInsightId(), List.of());
            boolean compare = "COMPARE".equals(r.getInsightType());
            CampaignComparison good = compare ? comparison(store, byId, role(cs, "GOOD")) : null;
            CampaignComparison bad = compare ? comparison(store, byId, role(cs, "BAD")) : null;
            List<CampaignBrief> targets = compare
                    ? List.of(brief(byId, role(cs, "GOOD"), "Good"), brief(byId, role(cs, "BAD"), "Bad"))
                    : cs.stream().filter(c -> "TARGET".equals(c.getCompareRole())).map(c -> brief(byId, c, "대상")).toList();
            Insights result = new Insights(compare ? "enough" : "early", r.getHeadline(), r.getSummary(), targets,
                    r.getSalesFrom(), r.getSalesTo(), suggestions(its, "STRENGTH"), suggestions(its, "IMPROVEMENT"),
                    suggestions(its, "SLOW_MENU"), good, bad,
                    compare && good != null && bad != null ? AiService.metricBars(good, bad) : List.of(),
                    diffs.getOrDefault(r.getInsightId(), List.of()).stream()
                            .map(d -> new Difference(d.getItemName(), d.getGoodValue(), d.getBadValue())).toList(),
                    r.getConclusion() == null ? null
                            : new Conclusion(r.getConclusion(), codes.goalName(r.getConclusionGoalCode()), r.getConclusionProductId()));
            reports.add(new InsightReport(r.getInsightId(), r.getCreatedAt(), r.getBaseUploadId(), result));
        }
        return reports;
    }

    private List<Suggestion> suggestions(List<InsightItemRow> items, String category) {
        String prefix = category.toLowerCase().replace("_", "-") + "-";
        return items.stream().filter(i -> category.equals(i.getItemCategory()))
                .map(i -> new Suggestion(prefix + i.getSortOrder(), i.getTitle(), i.getContent(), i.getEvidence(),
                        i.isEvidenceSample(), codes.goalName(i.getGoalCode()), i.getProductId()))
                .toList();
    }

    private static InsightCampaignRow role(List<InsightCampaignRow> rows, String role) {
        return rows.stream().filter(r -> role.equals(r.getCompareRole())).findFirst().orElse(null);
    }

    private static CampaignBrief brief(Map<Long, Campaign> byId, InsightCampaignRow r, String role) {
        Campaign c = r == null ? null : byId.get(r.getCampaignId());
        return new CampaignBrief(r == null ? null : r.getCampaignId(), c == null ? "" : c.getTitle(),
                c == null ? null : c.getPublishedAt(), c == null ? "-" : channels(c), c == null ? "" : c.getThumbnail(), role);
    }

    private static CampaignComparison comparison(Store store, Map<Long, Campaign> byId, InsightCampaignRow r) {
        if (r == null) {
            return null;
        }
        Campaign c = byId.get(r.getCampaignId());
        String menus = c == null ? "-" : String.join(", ",
                c.getProductIds().stream().map(store::productName).filter(n -> !n.isEmpty()).toList());
        return new CampaignComparison(r.getCampaignId(), c == null ? "" : c.getTitle(), c == null ? "-" : channels(c),
                menus.isEmpty() ? "-" : menus, r.getSalesChangeRate(), r.getBeforeAmount(), r.getAfterAmount(),
                nz(r.getLikeSum()), nz(r.getCommentSum()), nz(r.getShareSum()), nz(r.getSaveSum()), nz(r.getReachSum()),
                r.getReason());
    }

    private static String channels(Campaign c) {
        return c.getContent().getChannels().isEmpty() ? "-" : String.join(", ", c.getContent().getChannels());
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    private static String nz(String s) {
        return s == null || s.isBlank() ? "-" : s;
    }

    private static String cut(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
