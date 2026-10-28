package com.bizgent.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.bizgent.demo.DemoSession;
import com.bizgent.domain.AiPlan;
import com.bizgent.domain.Campaign;
import com.bizgent.domain.CampaignStatus;
import com.bizgent.domain.ContentSetting;
import com.bizgent.domain.Draft;
import com.bizgent.domain.ImageSetting;
import com.bizgent.domain.Notification;
import com.bizgent.domain.Post;
import com.bizgent.domain.Product;
import com.bizgent.domain.Store;
import com.bizgent.domain.Tags;

import lombok.RequiredArgsConstructor;

/**
 * 캠페인 생성 위자드 (1 목표 설정 → 2 AI 기획 → 3-1 콘텐츠 설정 → 3-2 이미지 설정 → 4 시안 비교 → 5 게시).
 * 다음 단계로 넘어가면 그 단계는 확정(lockedStep)되어 다시 수정할 수 없다.
 */
@Service
@RequiredArgsConstructor
public class WizardService {

    /** 3-1단계에서 고를 수 있는 게시 채널 (CHANNEL 코드 테이블) */
    public static final List<String> CHANNELS = List.of("인스타그램", "기타", "카카오톡 채널", "블로그", "당근마켓");

    private static final Map<String, String> GOAL_TO_PURPOSE = Map.of(
            "신메뉴/신상품 홍보", "신메뉴 홍보", "할인/이벤트 홍보", "할인·이벤트", "매장 인지도 확대", "매장 분위기");

    private final DemoSession session;

    /** 지금 작성 중인 캠페인 (없으면 null) */
    public Campaign activeDraft() {
        Long id = session.getActiveDraftId();
        if (id == null) {
            return null;
        }
        return session.getCampaigns().stream()
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
        Store store = session.getStore();
        return draft.getProductIds().stream().map(store::productName).filter(n -> !n.isEmpty()).toList();
    }

    /** 1단계 목표 설정 저장. 새로 입력한 메뉴는 매장 상품으로 추가한다. */
    public void saveGoal(Campaign draft, String goalOption, String customGoal, List<Long> productIds,
            List<String> newMenuNames, String targetCustomers, String toneManners, String extraNote) {
        Store store = session.getStore();
        List<Long> ids = new ArrayList<>(productIds);
        for (String name : newMenuNames) {
            String trimmed = name.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            Product existing = store.getProducts().stream().filter(p -> p.getProductName().equals(trimmed)).findFirst()
                    .orElse(null);
            if (existing == null) {
                existing = new Product(session.nextId(), trimmed, 0, 0, null, null);
                store.getProducts().add(existing);
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
        draft.setExtraNote(extraNote == null ? "" : extraNote.trim());
        // 1단계가 바뀌면 AI 기획을 새로 만든다.
        AiPlan reset = new AiPlan();
        reset.setTargetCustomer(String.join(", ", draft.getTargetCustomers()));
        draft.setPlan(reset);
        lock(draft, "1");
    }

    /** 3-1단계 콘텐츠 설정 저장. 채널 수는 무료 2개, 구독 3개까지 */
    public void saveContent(Campaign draft, List<String> channels, boolean hashtagOn, boolean captionOn,
            boolean imageOn, String customChannelNote) {
        int limit = session.getMember().isFree() ? 2 : 3;
        List<String> allowed = channels.stream().filter(CHANNELS::contains).distinct().limit(limit).toList();
        ContentSetting cs = draft.getContent();
        cs.setChannels(new ArrayList<>(allowed));
        cs.setHashtagOn(hashtagOn);
        cs.setCaptionOn(captionOn);
        cs.setImageOn(imageOn);
        cs.setCustomChannelNote(allowed.contains("기타") && customChannelNote != null ? customChannelNote.trim() : "");
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

    /** 3-2단계 이미지 설정 저장 */
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
        if (referenceImageUrl != null && !session.getMember().isFree()) {
            s.setReferenceImageUrl(referenceImageUrl);
        }
        s.setExtraRequest(extraRequest == null ? "" : extraRequest.trim());
        draft.setDrafts(new ArrayList<>());
        draft.setSelectedDraftKeys(new ArrayList<>());
        lock(draft, "3-2");
    }

    /** 4단계 시안 수정 저장 (수정하기 모달) */
    public Draft updateDraft(Campaign draft, String key, String text, List<String> hashtags, String imageUrl) {
        Draft d = draft.findDraft(key);
        if (d == null) {
            throw new NotFoundException("시안을 찾을 수 없습니다: " + key);
        }
        String trimmed = text == null ? "" : text.trim();
        d.setText(trimmed.length() > 300 ? trimmed.substring(0, 300) : trimmed);
        d.setHashtags(new ArrayList<>(hashtags.stream().map(t -> t.trim().replaceFirst("^#", ""))
                .filter(t -> !t.isEmpty()).distinct().toList()));
        d.setImageUrl(imageUrl == null ? "" : imageUrl);
        touch(draft);
        return d;
    }

    /** 4단계 시안 선택 확정 */
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
     * 5단계 게시 (모의). 선택한 시안을 한 번에 게시하고, 7일 뒤 성과·매출 입력 알림을 예약한다.
     * 실제 SNS 연동(F12)은 이 메서드에서 채널 API를 호출하도록 확장한다.
     */
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
        selected.stream().map(Draft::getImageUrl).filter(u -> u != null && !u.isEmpty()).findFirst()
                .ifPresent(draft::setThumbnailUrl);
        draft.setPosts(new ArrayList<>(selected.stream()
                .map(d -> new Post(d.getChannel(), d.getKey(), "success", 0, 0, 0)).toList()));
        session.setActiveDraftId(null);
        setReminder(draft, true);
        return draft;
    }

    /** 게시 완료 카드의 "7일 뒤 알림 받기" */
    public void setReminder(Campaign campaign, boolean on) {
        session.getNotifications().removeIf(n -> campaign.getCampaignId().equals(n.getCampaignId()));
        if (!on || campaign.getPublishedAt() == null) {
            campaign.setReminderAt(null);
            return;
        }
        LocalDateTime remindAt = campaign.getPublishedAt().plusDays(7);
        campaign.setReminderAt(remindAt);
        session.getNotifications().add(new Notification(session.nextId(), campaign.getCampaignId(),
                "캠페인 성과와 매출을 입력할 시간이에요", remindAt, false));
    }

    public void lock(Campaign draft, String step) {
        draft.setLockedStep(step);
        draft.setUpdatedAt(LocalDateTime.now());
    }

    public void touch(Campaign draft) {
        draft.setUpdatedAt(LocalDateTime.now());
    }
}
