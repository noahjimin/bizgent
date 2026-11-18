package com.bizgent.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.bizgent.domain.AiPlan;
import com.bizgent.domain.Campaign;
import com.bizgent.domain.CampaignStatus;
import com.bizgent.domain.ContentSetting;
import com.bizgent.domain.Draft;
import com.bizgent.domain.ImageSetting;
import com.bizgent.domain.Notification;
import com.bizgent.domain.Post;
import com.bizgent.domain.Tags;
import com.bizgent.mapper.CampaignMapper;
import com.bizgent.mapper.row.CampaignChannelRow;
import com.bizgent.mapper.row.CampaignProductRow;
import com.bizgent.mapper.row.CampaignRow;
import com.bizgent.mapper.row.DraftRow;
import com.bizgent.mapper.row.HashtagRow;
import com.bizgent.mapper.row.ImageSettingRow;
import com.bizgent.mapper.row.PlanRow;
import com.bizgent.mapper.row.PostRow;

import lombok.RequiredArgsConstructor;

/**
 * 캠페인 집합체({@link Campaign})를 여러 테이블에서 읽고 저장한다.
 * 위자드는 캠페인 객체를 고친 뒤 {@link #save}를 부르고, 여기서 바뀐 하위 기록을 다시 쓴다.
 */
@Component
@RequiredArgsConstructor
public class CampaignRepository {

    /** 3-2단계 이미지 비율 선택지. DB(IMAGE_RATIO, 10자)에는 앞의 비율만 저장한다. */
    private static final Map<String, String> RATIO_LABELS = Map.of("4:5", "4:5 세로 (추천)", "1:1", "1:1 정사각",
            "9:16", "9:16 세로", "16:9", "16:9 가로");

    private final CampaignMapper mapper;
    private final CodeService codes;

    /* ---------- 조회 ---------- */

    public List<Campaign> findAll(Long storeId) {
        return load(storeId, null);
    }

    /** @return 없으면 null */
    public Campaign find(Long storeId, Long campaignId) {
        List<Campaign> list = load(storeId, campaignId);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<Campaign> load(Long storeId, Long campaignId) {
        List<CampaignRow> rows = mapper.findCampaigns(storeId, campaignId);
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<Long, Campaign> byId = new LinkedHashMap<>();
        for (CampaignRow r : rows) {
            byId.put(r.getCampaignId(), toCampaign(r));
        }
        for (CampaignProductRow r : mapper.findProducts(storeId, campaignId)) {
            byId.get(r.getCampaignId()).getProductIds().add(r.getProductId());
        }
        for (CampaignChannelRow r : mapper.findChannels(storeId, campaignId)) {
            Campaign c = byId.get(r.getCampaignId());
            String name = r.getChannelName();
            c.getContent().getChannels().add(name);
            if (CodeService.CHANNEL_ETC.equals(r.getChannelCode())) {
                c.getContent().setCustomChannelName(nz(r.getCustomChannelName()));
                c.getContent().setCustomChannelNote(nz(r.getCustomChannelNote()));
            }
            if (r.getImageRatio() != null) {
                c.getImage().getRatios().put(name, RATIO_LABELS.getOrDefault(r.getImageRatio(), r.getImageRatio()));
            }
        }
        for (PlanRow r : mapper.findLatestPlans(storeId, campaignId)) {
            byId.get(r.getCampaignId()).setPlan(toPlan(r));
        }
        for (ImageSettingRow r : mapper.findImageSettings(storeId, campaignId)) {
            ImageSetting s = byId.get(r.getCampaignId()).getImage();
            s.setPurpose(r.getPurpose() == null ? "" : r.getPurpose());
            s.setProduct(r.getMainProduct());
            s.setMood(nz(r.getMood()));
            s.setComposition(nz(r.getComposition()));
            s.setBrandColorOn(r.isBrandColorOn());
            s.setReferenceImageUrl(nz(r.getReferenceImageUrl()));
            s.setExtraRequest(nz(r.getExtraRequest()));
        }
        Map<Long, List<String>> hashtags = mapper.findHashtags(storeId, campaignId).stream().collect(
                Collectors.groupingBy(HashtagRow::getDraftId, Collectors.mapping(HashtagRow::getHashtag, Collectors.toList())));
        for (DraftRow r : mapper.findDrafts(storeId, campaignId)) {
            Campaign c = byId.get(r.getCampaignId());
            Draft d = new Draft(r.getDraftType(), codes.channelName(r.getChannelCode()), r.getCopyText(),
                    new ArrayList<>(hashtags.getOrDefault(r.getDraftId(), List.of())), nz(r.getImageUrl()));
            d.setDraftId(r.getDraftId());
            c.getDrafts().add(d);
            if (r.isSelected()) {
                c.getSelectedDraftKeys().add(r.getDraftType());
            }
        }
        for (PostRow r : mapper.findPosts(storeId, campaignId)) {
            byId.get(r.getCampaignId()).getPosts().add(new Post(codes.channelName(r.getChannelCode()), r.getDraftType(),
                    r.getPostStatus()).reactions(r.getLikeCount(), r.getCommentCount(), r.getShareCount(),
                            r.getSaveCount(), r.getReachCount()));
        }
        for (Notification n : mapper.findReminders(storeId, campaignId, NotificationService.TYPE_CAMPAIGN_RESULT)) {
            byId.get(n.getCampaignId()).setReminderAt(n.getDueAt());
        }
        return new ArrayList<>(byId.values());
    }

    private Campaign toCampaign(CampaignRow r) {
        Campaign c = new Campaign();
        c.setCampaignId(r.getCampaignId());
        c.setTitle(nz(r.getCampaignName()));
        c.setStatus(CampaignStatus.valueOf(r.getCampaignStatus()));
        c.setCreatedAt(r.getCreatedAt());
        c.setUpdatedAt(r.getUpdatedAt());
        c.setPublishedAt(r.getPublishedAt());
        if (CodeService.GOAL_ETC.equals(r.getGoalCode())) {
            c.setGoalOption(CampaignService.CUSTOM_GOAL);
            c.setCustomGoal(nz(r.getCustomGoal()));
            c.setGoal(c.getCustomGoal());
        } else if (r.getGoalCode() != null) {
            c.setGoalOption(r.getGoalName());
            c.setGoal(r.getGoalName());
        }
        c.setInsightId(r.getInsightId());
        c.setUsageMonth(r.getUsageMonth());
        c.setTargetCustomers(Tags.split(r.getTargetCustomer()));
        c.setToneManners(Tags.split(r.getToneManner()));
        c.setExtraNote(nz(r.getExtraNote()));
        ContentSetting cs = c.getContent();
        cs.setHashtagOn(r.isHashtagOn());
        cs.setCaptionOn(r.isCaptionOn());
        cs.setImageOn(r.isImageOn());
        c.setCurrentStep(r.getCurrentStep());
        c.setLockedStep(r.getLockedStep());
        return c;
    }

    private static AiPlan toPlan(PlanRow r) {
        AiPlan p = new AiPlan(nz(r.getDirection()), nz(r.getTargetAnalysis()), nz(r.getStrategy()), nz(r.getExpectedEffect()));
        p.setAgeGroup(r.getAgeGroup());
        p.setTones(Tags.split(r.getTones()));
        p.setChannels(Tags.split(r.getRecommendedChannels()));
        p.setReach(r.getReach());
        p.setFrequency(r.getFrequency());
        p.setDuration(r.getDuration());
        p.setUserRequest(r.getUserRequest());
        p.setVersionNo(r.getVersionNo());
        p.setSource(r.getPlanSource());
        p.setSavedContent(p.snapshot());
        return p;
    }

    /* ---------- 저장 ---------- */

    /** 새 캠페인 저장 (하위 기록 포함) */
    public void insert(Campaign c, Long storeId) {
        CampaignRow row = toRow(c, storeId);
        mapper.insertCampaign(row);
        c.setCampaignId(row.getCampaignId());
        saveChildren(c);
    }

    /** 캠페인과 하위 기록(대상 메뉴, 채널, 기획, 이미지 설정, 시안)을 지금 객체 상태로 맞춘다. 게시 기록은 {@link #insertPosts}. */
    public void save(Campaign c) {
        mapper.updateCampaign(toRow(c, null));
        saveChildren(c);
    }

    /** 단계 표시만 바뀌었을 때 */
    public void saveSteps(Campaign c) {
        mapper.updateSteps(c.getCampaignId(), currentStep(c), c.getLockedStep());
    }

    public void insertPosts(Campaign c) {
        Map<String, Draft> drafts = c.getDrafts().stream().collect(Collectors.toMap(Draft::getKey, Function.identity()));
        for (Post p : c.getPosts()) {
            Draft d = drafts.get(p.getDraftKey());
            PostRow row = PostRow.builder().draftId(d.getDraftId()).postStatus(p.getStatus())
                    .postedAt(c.getPublishedAt()).likeCount(p.getLikes()).commentCount(p.getComments())
                    .shareCount(p.getShares()).saveCount(p.getSaves()).reachCount(p.getReach()).build();
            mapper.insertPost(row);
            mapper.insertPostMetric(row);
        }
    }

    private void saveChildren(Campaign c) {
        Long id = c.getCampaignId();
        mapper.deleteProducts(id);
        c.getProductIds().stream().distinct()
                .forEach(pid -> mapper.insertProduct(new CampaignProductRow(id, pid)));

        mapper.deleteChannels(id);
        for (String name : c.getContent().getChannels().stream().distinct().toList()) {
            String code = codes.channelCode(name);
            if (code == null) {
                continue;
            }
            boolean etc = CodeService.CHANNEL_ETC.equals(code);
            String ratio = c.getImage().getRatios().get(name);
            mapper.insertChannel(CampaignChannelRow.builder().campaignId(id).channelCode(code)
                    .customChannelName(etc ? cut(blankToNull(c.getContent().getCustomChannelName()), 20) : null)
                    .customChannelNote(etc ? cut(blankToNull(c.getContent().getCustomChannelNote()), 200) : null)
                    .imageRatio(ratio == null || ratio.isBlank() ? null : cut(ratio.split(" ")[0], 10)).build());
        }

        savePlan(c);

        mapper.deleteImageSetting(id);
        ImageSetting s = c.getImage();
        if (s.getPurpose() != null) {
            mapper.insertImageSetting(ImageSettingRow.builder().campaignId(id).purpose(cut(s.getPurpose(), 200))
                    .mainProduct(cut(s.getProduct(), 50)).mood(cut(s.getMood(), 200)).composition(cut(s.getComposition(), 200))
                    .brandColorOn(s.isBrandColorOn()).referenceImageUrl(blankToNull(s.getReferenceImageUrl()))
                    .extraRequest(cut(blankToNull(s.getExtraRequest()), 300)).build());
        }

        saveDrafts(c);
    }

    /** AI 기획은 생성·재분석·수정할 때마다 버전을 올려 새 행으로 남긴다. 1단계를 다시 저장해 기획이 비면 지운다. */
    private void savePlan(Campaign c) {
        AiPlan p = c.getPlan();
        if (p.isEmpty()) {
            mapper.deletePlans(c.getCampaignId());
            return;
        }
        boolean changed = p.getVersionNo() != null && !p.snapshot().equals(p.getSavedContent());
        if (p.getVersionNo() == null || changed) {
            PlanRow row = PlanRow.builder().campaignId(c.getCampaignId())
                    .planSource(changed ? "USER_EDIT" : p.getSource() == null ? "AI" : p.getSource())
                    .direction(cut(p.getDirection(), 1000)).targetAnalysis(cut(p.getTargetCustomer(), 1000))
                    .ageGroup(cut(p.getAgeGroup(), 30)).strategy(cut(p.getStrategy(), 1000))
                    .tones(cut(Tags.join(p.getTones()), 300)).recommendedChannels(cut(Tags.join(p.getChannels()), 300))
                    .expectedEffect(cut(p.getExpectedEffect(), 1000)).reach(cut(p.getReach(), 50))
                    .frequency(cut(p.getFrequency(), 50)).duration(cut(p.getDuration(), 50))
                    .userRequest(cut(p.getUserRequest(), 1000)).build();
            mapper.insertPlan(row);
            p.setVersionNo(row.getVersionNo());
            p.setSource(row.getPlanSource());
            p.setSavedContent(p.snapshot());
        }
        if (c.getLockedStep() != null && Campaign.STEPS.indexOf(c.getLockedStep()) >= Campaign.STEPS.indexOf("2")) {
            mapper.confirmLatestPlan(c.getCampaignId());
        }
    }

    private void saveDrafts(Campaign c) {
        List<Long> keep = c.getDrafts().stream().map(Draft::getDraftId).filter(java.util.Objects::nonNull).toList();
        mapper.deleteDraftsExcept(c.getCampaignId(), keep);
        for (Draft d : c.getDrafts()) {
            String code = codes.channelCode(d.getChannel());
            DraftRow row = DraftRow.builder().draftId(d.getDraftId()).campaignId(c.getCampaignId())
                    .channelCode(code == null ? CodeService.CHANNEL_ETC : code).draftType(d.getKey())
                    .copyText(cut(d.getText() == null || d.getText().isBlank() ? " " : d.getText(), 1000))
                    .imageUrl(blankToNull(d.getImageUrl())).selected(c.getSelectedDraftKeys().contains(d.getKey())).build();
            if (d.getDraftId() == null) {
                mapper.insertDraft(row);
                d.setDraftId(row.getDraftId());
            } else {
                mapper.updateDraft(row);
                mapper.deleteHashtags(d.getDraftId());
            }
            List<String> tags = d.getHashtags().stream().distinct().toList();
            for (int i = 0; i < tags.size(); i++) {
                mapper.insertHashtag(new HashtagRow(d.getDraftId(), i + 1, cut(tags.get(i), 100)));
            }
        }
    }

    private CampaignRow toRow(Campaign c, Long storeId) {
        String goalCode = null;
        String customGoal = null;
        String name = !c.getGoalOption().isEmpty() ? c.getGoalOption() : c.getGoal();
        if (CampaignService.CUSTOM_GOAL.equals(c.getGoalOption())) {
            goalCode = CodeService.GOAL_ETC;
            customGoal = blankToNull(c.getCustomGoal());
        } else if (!name.isEmpty()) {
            goalCode = codes.goalCode(name);
            if (goalCode == null) {
                // 목록에 없는 목표 문구는 '기타 (직접 입력)'으로 저장한다.
                goalCode = CodeService.GOAL_ETC;
                customGoal = name;
            }
        }
        return CampaignRow.builder().campaignId(c.getCampaignId()).storeId(storeId).goalCode(goalCode)
                .customGoal(cut(customGoal, 100)).insightId(c.getInsightId())
                .campaignName(cut(c.getTitle() == null || c.getTitle().isBlank() ? "새 캠페인" : c.getTitle(), 200))
                .targetCustomer(cut(blankToNull(Tags.join(c.getTargetCustomers())), 500))
                .toneManner(cut(blankToNull(Tags.join(c.getToneManners())), 500))
                .extraNote(cut(blankToNull(c.getExtraNote()), 300)).hashtagOn(c.getContent().isHashtagOn())
                .captionOn(c.getContent().isCaptionOn()).imageOn(c.getContent().isImageOn())
                .currentStep(currentStep(c)).lockedStep(c.getLockedStep()).usageMonth(c.getUsageMonth())
                .campaignStatus(c.getStatus().name()).createdAt(c.getCreatedAt()).updatedAt(c.getUpdatedAt())
                .publishedAt(c.getPublishedAt()).build();
    }

    private static String currentStep(Campaign c) {
        return c.getCurrentStep() != null ? c.getCurrentStep() : c.getResumeStep();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    static String cut(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
