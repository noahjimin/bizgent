package com.bizgent.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bizgent.domain.AiPlan;
import com.bizgent.domain.Campaign;
import com.bizgent.domain.CampaignStatus;
import com.bizgent.domain.ContentSetting;
import com.bizgent.domain.Draft;
import com.bizgent.domain.ImageSetting;
import com.bizgent.domain.Post;
import com.bizgent.domain.Product;
import com.bizgent.domain.Store;
import com.bizgent.domain.Tags;
import com.bizgent.mapper.NotificationMapper;

import lombok.RequiredArgsConstructor;

/**
 * 캠페인 생성 위자드 (1 목표 설정 → 2 AI 기획 → 3-1 콘텐츠 설정 → 3-2 이미지 설정 → 4 시안 비교 → 5 게시).
 * 다음 단계로 넘어가면 그 단계는 확정(LOCKED_STEP)되어 다시 수정할 수 없다. 단계마다 캠페인과 하위 기록을 DB에 저장한다.
 */
@Service
@RequiredArgsConstructor
public class WizardService {

    /** 3-1단계에서 고를 수 있는 게시 채널 (CHANNEL 코드 테이블) */
    public static final List<String> CHANNELS = List.of("인스타그램", "기타", "카카오톡 채널", "블로그", "당근마켓");

    private static final Map<String, String> GOAL_TO_PURPOSE = Map.of(
            "신메뉴/신상품 홍보", "신메뉴 홍보", "할인/이벤트 홍보", "할인·이벤트", "매장 인지도 확대", "매장 분위기");

    private final UserSession session;
    private final RequestData data;
    private final CampaignRepository repository;
    private final StoreService storeService;
    private final NotificationMapper notificationMapper;

    /** 지금 작성 중인 캠페인 (없으면 null) */
    public Campaign activeDraft() {
        Long id = session.getActiveDraftId();
        if (id == null) {
            return null;
        }
        return data.campaigns().stream()
                .filter(c -> c.getCampaignId().equals(id) && !c.isPublished())
                .findFirst().orElse(null);
    }

    /**
     * 확정된 단계를 주소로 다시 열면 그다음 단계로 보낸다.
     *
     * @return 이동할 단계. 그대로 보여주면 null
     */
    public String redirectFor(Campaign draft, String step) {
        List<String> order = Campaign.STEPS;
        int current = order.indexOf(step);
        int locked = draft.getLockedStep() == null ? -1 : order.indexOf(draft.getLockedStep());
        if (locked >= 0 && current <= locked) {
            String next = draft.nextStep(draft.getLockedStep());
            // 데이터가 실제로 다음 단계를 지원할 때만 보낸다 (무한 이동 방지).
            if (next != null && order.indexOf(draft.getResumeStep()) >= order.indexOf(next)) {
                return next;
            }
        }
        return null;
    }

    public List<String> menuNames(Campaign draft) {
        Store store = data.store();
        return draft.getProductIds().stream().map(store::productName).filter(n -> !n.isEmpty()).toList();
    }

    /** 1단계 목표 설정 저장. 새로 입력한 메뉴는 매장 상품으로 추가한다. */
    @Transactional
    public void saveGoal(Campaign draft, String goalOption, String customGoal, List<Long> productIds,
            List<String> newMenuNames, String targetCustomers, String toneManners, String extraNote) {
        Store store = data.store();
        List<Long> ids = new ArrayList<>(productIds.stream().filter(id -> store.findProduct(id) != null).toList());
        for (String name : newMenuNames) {
            String trimmed = name.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            Product existing = store.getProducts().stream().filter(p -> p.getProductName().equals(trimmed)).findFirst()
                    .orElse(null);
            if (existing == null) {
                existing = storeService.addProduct(trimmed);
            }
            if (!ids.contains(existing.getProductId())) {
                ids.add(existing.getProductId());
            }
        }

        draft.setGoalOption(goalOption);
        draft.setCustomGoal(customGoal == null ? "" : customGoal.trim());
        String goal = CampaignService.CUSTOM_GOAL.equals(goalOption) ? draft.getCustomGoal() : goalOption;
        draft.setGoal(goal);
        draft.setTitle(goal.isEmpty() ? "새 캠페인" : goal);
        draft.setProductIds(ids);
        draft.setTargetCustomers(Tags.split(targetCustomers));
        draft.setToneManners(Tags.split(toneManners));
        String note = extraNote == null ? "" : extraNote.trim();
        draft.setExtraNote(note.length() > 300 ? note.substring(0, 300) : note);
        // 1단계가 바뀌면 AI 기획을 새로 만든다.
        AiPlan reset = new AiPlan();
        reset.setTargetCustomer(String.join(", ", draft.getTargetCustomers()));
        draft.setPlan(reset);
        lock(draft, "1");
    }

    /** 3-1단계 콘텐츠 설정 저장. 채널 수는 플랜의 최대 채널 수까지 */
    @Transactional
    public void saveContent(Campaign draft, List<String> channels, boolean hashtagOn, boolean captionOn,
            boolean imageOn, String customChannelName, String customChannelNote) {
        int limit = data.member().getChannelLimit();
        List<String> allowed = channels.stream().filter(CHANNELS::contains).distinct().limit(limit).toList();
        ContentSetting cs = draft.getContent();
        cs.setChannels(new ArrayList<>(allowed));
        cs.setHashtagOn(hashtagOn);
        cs.setCaptionOn(captionOn);
        cs.setImageOn(imageOn);
        boolean custom = allowed.contains("기타");
        String name = custom && customChannelName != null ? customChannelName.trim() : "";
        cs.setCustomChannelName(name.length() > 20 ? name.substring(0, 20) : name);
        cs.setCustomChannelNote(custom && customChannelNote != null ? customChannelNote.trim() : "");
        // 채널이 바뀌면 시안을 새로 만든다.
        draft.setDrafts(new ArrayList<>());
        draft.setSelectedDraftKeys(new ArrayList<>());
        lock(draft, "3-1");
    }

    /** 3-2단계 첫 화면에 보여줄 AI 추천 기본값 (저장하지 않는다) */
    public ImageSetting imageDefaults(Campaign draft) {
        ImageSetting saved = draft.getImage();
        if (saved.getPurpose() != null) {
            return saved;
        }
        ImageSetting s = new ImageSetting();
        s.setPurpose(GOAL_TO_PURPOSE.getOrDefault(draft.getGoalOption(), "신메뉴 홍보"));
        List<String> menus = menuNames(draft);
        s.setProduct(menus.isEmpty() ? "" : menus.get(0));
        s.setComposition(saved.getComposition().isEmpty() ? "사진 느낌" : saved.getComposition());
        s.setMood(saved.getMood().isEmpty() ? "따뜻한" : saved.getMood());
        s.setBrandColorOn(true);
        for (String ch : draft.getContent().getChannels()) {
            s.getRatios().put(ch, "인스타그램".equals(ch) ? "4:5 세로 (추천)" : "1:1 정사각");
        }
        return s;
    }

    /** 3-2단계 이미지 설정 저장 (채널별 비율은 CAMPAIGN_CHANNEL.IMAGE_RATIO) */
    @Transactional
    public void saveImage(Campaign draft, String purpose, String product, String composition, String mood,
            boolean brandColorOn, List<String> ratios, String referenceImageUrl, String extraRequest) {
        ImageSetting s = draft.getImage();
        s.setPurpose(purpose);
        s.setProduct(product);
        s.setComposition(composition);
        s.setMood(mood);
        s.setBrandColorOn(brandColorOn);
        s.getRatios().clear();
        List<String> channels = draft.getContent().getChannels();
        for (int i = 0; i < channels.size(); i++) {
            s.getRatios().put(channels.get(i), i < ratios.size() ? ratios.get(i) : "1:1 정사각");
        }
        // 참고 이미지는 구독 사용자만 쓸 수 있다.
        if (referenceImageUrl != null && !data.member().isFree()) {
            s.setReferenceImageUrl(referenceImageUrl);
        }
        String extra = extraRequest == null ? "" : extraRequest.trim();
        s.setExtraRequest(extra.length() > 300 ? extra.substring(0, 300) : extra);
        draft.setDrafts(new ArrayList<>());
        draft.setSelectedDraftKeys(new ArrayList<>());
        lock(draft, "3-2");
    }

    /** 4단계 AI가 만든 시안 A/B 저장 */
    @Transactional
    public void saveDrafts(Campaign draft, List<Draft> drafts) {
        draft.setDrafts(new ArrayList<>(drafts));
        draft.setSelectedDraftKeys(new ArrayList<>());
        touch(draft);
    }

    /** 4단계 시안 수정 저장 (수정하기 모달). 문구를 비우면 이전 문구를 유지한다 (COPY_TEXT 필수). */
    @Transactional
    public Draft updateDraft(Campaign draft, String key, String text, List<String> hashtags, String imageUrl) {
        Draft d = draft.findDraft(key);
        if (d == null) {
            throw new NotFoundException("시안을 찾을 수 없습니다: " + key);
        }
        String trimmed = text == null ? "" : text.trim();
        if (!trimmed.isEmpty()) {
            d.setText(trimmed.length() > 300 ? trimmed.substring(0, 300) : trimmed);
        }
        d.setHashtags(new ArrayList<>(hashtags.stream().map(t -> t.trim().replaceFirst("^#", ""))
                .filter(t -> !t.isEmpty()).distinct().toList()));
        d.setImageUrl(imageUrl == null ? "" : imageUrl);
        touch(draft);
        return d;
    }

    /** 4단계 시안 선택 확정 (DRAFT.SELECTED_YN) */
    @Transactional
    public boolean saveSelection(Campaign draft, List<String> keys) {
        List<String> valid = draft.getDrafts().stream().map(Draft::getKey).filter(keys::contains).toList();
        if (valid.isEmpty()) {
            return false;
        }
        draft.setSelectedDraftKeys(new ArrayList<>(valid));
        lock(draft, "4");
        return true;
    }

    /**
     * 5단계 게시 (모의). 선택한 시안을 한 번에 게시하고(POST), 반응은 예시 데이터로 POST_METRIC에 SAMPLE로 남긴다.
     * 7일 뒤 성과·매출 입력 알림을 예약한다. 실제 SNS 연동(F12)은 이 메서드에서 채널 API를 호출하도록 확장한다.
     */
    @Transactional
    public Campaign publish(Campaign draft) {
        List<Draft> selected = draft.getDrafts().stream()
                .filter(d -> draft.getSelectedDraftKeys().contains(d.getKey())).toList();
        if (selected.isEmpty()) {
            throw new IllegalStateException("게시할 시안이 없습니다");
        }
        LocalDateTime now = LocalDateTime.now();
        draft.setStatus(CampaignStatus.PUBLISHED);
        draft.setPublishedAt(now);
        draft.setUpdatedAt(now);
        draft.setLockedStep("5");
        draft.setCurrentStep("5");
        draft.setPosts(new ArrayList<>(selected.stream()
                // 반응 수치는 인스타그램 연동 전까지 캠페인 ID로 만든 예시 값
                .map(d -> new Post(d.getChannel(), d.getKey(), Post.SUCCESS).reactions(
                        120 + (int) (draft.getCampaignId() % 7) * 17, 8 + (int) (draft.getCampaignId() % 5),
                        5 + (int) (draft.getCampaignId() % 4), 14 + (int) (draft.getCampaignId() % 6) * 2,
                        1800 + (int) (draft.getCampaignId() % 9) * 230))
                .toList()));
        repository.save(draft);
        repository.insertPosts(draft);
        session.setActiveDraftId(null);
        setReminder(draft, true);
        return draft;
    }

    /** 게시 완료 카드의 "7일 뒤 알림 받기" (NOTIFICATION.DUE_AT) */
    @Transactional
    public void setReminder(Campaign campaign, boolean on) {
        notificationMapper.deleteByCampaign(campaign.getCampaignId(), NotificationService.TYPE_CAMPAIGN_RESULT);
        if (!on || campaign.getPublishedAt() == null) {
            campaign.setReminderAt(null);
            return;
        }
        LocalDateTime remindAt = campaign.getPublishedAt().plusDays(7);
        campaign.setReminderAt(remindAt);
        notificationMapper.insert(data.member().getMemberId(), campaign.getCampaignId(),
                NotificationService.TYPE_CAMPAIGN_RESULT, "캠페인 성과와 매출을 입력할 시간이에요", remindAt);
    }

    /** 단계를 확정하고 지금 입력값을 저장한다. */
    public void lock(Campaign draft, String step) {
        draft.setLockedStep(step);
        touch(draft);
    }

    /** 지금 입력값을 저장한다 (단계 확정 없이). */
    public void touch(Campaign draft) {
        draft.setUpdatedAt(LocalDateTime.now());
        repository.save(draft);
    }

    /** 보던 단계(CURRENT_STEP)·확정 단계(LOCKED_STEP)만 저장한다. */
    public void saveSteps(Campaign draft) {
        repository.saveSteps(draft);
    }
}
