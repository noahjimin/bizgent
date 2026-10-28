package com.bizgent.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/** 캠페인 (CAMPAIGN). 기획·설정·시안·게시 결과를 한 번에 담는 집합체 */
@Getter
@Setter
public class Campaign {

    /** 위자드 단계 순서 */
    public static final List<String> STEPS = List.of("1", "2", "3-1", "3-2", "4", "5");

    private Long campaignId;
    private String title = "새 캠페인";
    private CampaignStatus status = CampaignStatus.DRAFT;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
    private LocalDateTime reminderAt;

    /** 최종 목표 문구 (선택한 목표 또는 직접 입력한 목표) */
    private String goal = "";
    /** 1단계에서 선택한 목표 옵션 */
    private String goalOption = "";
    private String customGoal = "";
    private List<Long> productIds = new ArrayList<>();
    private List<String> targetCustomers = new ArrayList<>();
    private List<String> toneManners = new ArrayList<>();
    private String extraNote = "";

    private AiPlan plan = new AiPlan();
    private ContentSetting content = new ContentSetting();
    private ImageSetting image = new ImageSetting();
    private List<Draft> drafts = new ArrayList<>();
    private List<String> selectedDraftKeys = new ArrayList<>();
    private List<Post> posts = new ArrayList<>();

    private String thumbnailUrl = "";
    /** AI 캠페인 개선 방향 제안에서 목표·메뉴가 미리 선택되어 시작된 캠페인 */
    private boolean insightPrefill;
    /** 사용자가 확정한 가장 마지막 단계. 이 단계와 이전 단계는 다시 수정할 수 없다. */
    private String lockedStep;

    public boolean isPublished() {
        return status == CampaignStatus.PUBLISHED;
    }

    public LocalDateTime getTimestamp() {
        return updatedAt != null ? updatedAt : createdAt;
    }

    public Draft findDraft(String key) {
        return drafts.stream().filter(d -> d.getKey().equals(key)).findFirst().orElse(null);
    }

    /** 4단계에서 게시할 시안으로 고른 시안 */
    public List<Draft> getSelectedDrafts() {
        return drafts.stream().filter(d -> selectedDraftKeys.contains(d.getKey())).toList();
    }

    /** 게시에 성공한 시안 키 (시안 순서대로) */
    public List<String> getPublishedDraftKeys() {
        return posts.stream()
                .filter(p -> p.getDraftKey() != null && p.isSuccess())
                .map(Post::getDraftKey)
                .distinct()
                .sorted()
                .toList();
    }

    /** 게시된 시안. 시안별 결과가 없는 이전 데이터는 선택한 시안으로 대신한다. */
    public List<Draft> getPublishedDrafts() {
        List<String> keys = getPublishedDraftKeys();
        List<String> use = !keys.isEmpty() ? keys : isPublished() ? selectedDraftKeys : List.of();
        return drafts.stream().filter(d -> use.contains(d.getKey())).toList();
    }

    /** 카드 썸네일: 처음 게시된 시안의 이미지, 없으면 thumbnailUrl */
    public String getThumbnail() {
        List<String> keys = getPublishedDraftKeys();
        if (!keys.isEmpty()) {
            Draft first = findDraft(keys.get(0));
            if (first != null && first.getImageUrl() != null && !first.getImageUrl().isEmpty()) {
                return first.getImageUrl();
            }
        }
        return thumbnailUrl;
    }

    /** 보관함 카드 콜라주용 이미지 (최대 2개) */
    public List<Draft> getCollage() {
        List<Draft> published = getPublishedDrafts();
        List<Draft> source = !published.isEmpty() ? published : drafts;
        List<Draft> images = source.stream()
                .filter(d -> d.getImageUrl() != null && !d.getImageUrl().isEmpty())
                .limit(2)
                .toList();
        if (!images.isEmpty() || thumbnailUrl == null || thumbnailUrl.isEmpty()) {
            return images;
        }
        return List.of(new Draft(null, null, null, List.of(), thumbnailUrl));
    }

    /** 작성 중인 캠페인을 이어서 진행할 위자드 단계 */
    public String getResumeStep() {
        if (goal.isEmpty() || productIds.isEmpty()) return "1";
        if (plan.isEmpty()) return "2";
        if (content.getChannels().isEmpty()) return "3-1";
        if (content.isImageOn() && image.getPurpose() == null) return "3-2";
        if (drafts.isEmpty() || selectedDraftKeys.isEmpty()) return "4";
        return "5";
    }

    /** 확정된 단계 이후로 이동할 수 있는 다음 단계 (3-2는 이미지 생성을 켠 경우에만) */
    public String nextStep(String step) {
        int next = STEPS.indexOf(step) + 1;
        if (next < STEPS.size() && STEPS.get(next).equals("3-2") && !content.isImageOn()) {
            next++;
        }
        return next < STEPS.size() ? STEPS.get(next) : null;
    }

    /** 이 단계가 이미 확정되어 다시 열 수 없는지 */
    public boolean isStepLocked(String step) {
        return lockedStep != null && STEPS.indexOf(step) <= STEPS.indexOf(lockedStep);
    }

    public String getFirstChannel() {
        return content.getChannels().isEmpty() ? "" : content.getChannels().get(0);
    }
}
