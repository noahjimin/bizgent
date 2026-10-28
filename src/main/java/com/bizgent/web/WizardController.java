package com.bizgent.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.bizgent.domain.AiPlan;
import com.bizgent.domain.Campaign;
import com.bizgent.domain.Draft;
import com.bizgent.domain.Product;
import com.bizgent.domain.Tags;
import com.bizgent.service.CampaignService;
import com.bizgent.service.FileStorage;
import com.bizgent.service.ForbiddenException;
import com.bizgent.service.MemberService;
import com.bizgent.service.NotFoundException;
import com.bizgent.service.StoreService;
import com.bizgent.service.WizardService;
import com.bizgent.service.ai.AiService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/** 캠페인 생성 위자드 (SCR-U-007~008, F03~F08, F11, F16) */
@Controller
@RequiredArgsConstructor
public class WizardController {

    private final WizardService wizardService;
    private final StoreService storeService;
    private final MemberService memberService;
    private final AiService aiService;
    private final CampaignService campaignService;
    private final FileStorage fileStorage;

    @GetMapping("/campaigns/new/{step}")
    public String step(@PathVariable String step, Model model) {
        if (!Campaign.STEPS.contains(step)) {
            throw new NotFoundException("없는 단계입니다: " + step);
        }
        Campaign draft = wizardService.activeDraft();
        if (draft == null) {
            return "redirect:/campaigns";
        }
        String redirect = wizardService.redirectFor(draft, step);
        if (redirect != null) {
            return "redirect:/campaigns/new/" + redirect;
        }
        // 앞 단계 입력이 아직 없으면 그 단계부터 진행한다.
        String resume = draft.getResumeStep();
        if (Campaign.STEPS.indexOf(step) > Campaign.STEPS.indexOf(resume)) {
            return "redirect:/campaigns/new/" + resume;
        }
        // 광고 이미지를 끈 캠페인은 이미지 설정을 건너뛴다.
        if ("3-2".equals(step) && !draft.getContent().isImageOn()) {
            return "redirect:/campaigns/new/4";
        }
        model.addAttribute("draft", draft);
        model.addAttribute("step", step);
        model.addAttribute("stepIndex", Campaign.STEPS.indexOf(step));
        model.addAttribute("menuNames", wizardService.menuNames(draft));
        if ("3-2".equals(step)) {
            model.addAttribute("imageSetting", wizardService.imageDefaults(draft));
            // 주인공 상품 후보: 대상 메뉴 + 매장 메뉴 (최대 3개)
            model.addAttribute("productOptions", Stream.concat(wizardService.menuNames(draft).stream(),
                    storeService.get().getProducts().stream().map(Product::getProductName)).distinct().limit(3).toList());
        }
        return "wizard/step" + step;
    }

    /* ---------- 3-1단계: 콘텐츠 설정 ---------- */

    @PostMapping("/campaigns/new/3-1")
    public String saveContent(@RequestParam(defaultValue = "") String channels,
            @RequestParam(defaultValue = "false") boolean hashtagOn, @RequestParam(defaultValue = "true") boolean captionOn,
            @RequestParam(defaultValue = "false") boolean imageOn, @RequestParam(defaultValue = "") String customChannelNote) {
        Campaign draft = requireDraft();
        List<String> list = Arrays.stream(channels.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        if (list.isEmpty()) {
            return "redirect:/campaigns/new/3-1";
        }
        wizardService.saveContent(draft, list, hashtagOn, captionOn, imageOn, customChannelNote);
        return "redirect:/campaigns/new/" + (draft.getContent().isImageOn() ? "3-2" : "4");
    }

    /* ---------- 3-2단계: 이미지 설정 ---------- */

    @PostMapping("/campaigns/new/3-2")
    public String saveImage(@RequestParam(defaultValue = "") String purpose, @RequestParam(defaultValue = "") String product,
            @RequestParam(defaultValue = "") String composition, @RequestParam(defaultValue = "") String mood,
            @RequestParam(defaultValue = "true") boolean brandColorOn, @RequestParam(defaultValue = "") String extraRequest,
            @RequestParam(required = false) MultipartFile referenceImageFile, HttpServletRequest request) {
        Campaign draft = requireDraft();
        if (composition.isBlank() || mood.isBlank()) {
            return "redirect:/campaigns/new/3-2";
        }
        List<String> ratios = new ArrayList<>();
        for (int i = 0; i < draft.getContent().getChannels().size(); i++) {
            ratios.add(request.getParameter("ratio_" + i));
        }
        String reference = fileStorage.saveImage(referenceImageFile, "reference");
        wizardService.saveImage(draft, purpose, product, composition, mood, brandColorOn, ratios, reference, extraRequest);
        return "redirect:/campaigns/new/4";
    }

    /* ---------- 4단계: 시안 비교 ---------- */

    @PostMapping("/api/wizard/drafts")
    @ResponseBody
    public List<Draft> generateDrafts() {
        Campaign draft = requireDraft();
        if (draft.getDrafts().isEmpty()) {
            draft.setDrafts(new ArrayList<>(aiService.generateDrafts(storeService.get(), draft)));
            draft.setSelectedDraftKeys(new ArrayList<>());
            wizardService.touch(draft);
        }
        return draft.getDrafts();
    }

    public record DraftUpdate(String text, List<String> hashtags, String imageUrl) {
    }

    @PostMapping("/api/wizard/drafts/{key}")
    @ResponseBody
    public Draft updateDraft(@PathVariable String key, @RequestBody DraftUpdate body) {
        Campaign draft = requireDraft();
        return wizardService.updateDraft(draft, key, body.text(),
                body.hashtags() == null ? List.of() : body.hashtags(), body.imageUrl());
    }

    public record TextRequest(String text) {
    }

    /** 구독: AI 문구 대안 3가지 */
    @PostMapping("/api/wizard/drafts/{key}/alternatives")
    @ResponseBody
    public List<String> alternatives(@PathVariable String key, @RequestBody TextRequest body) {
        requirePro();
        Draft d = requireDraft().findDraft(key);
        Draft current = new Draft(d.getKey(), d.getChannel(), body.text(), d.getHashtags(), d.getImageUrl());
        return aiService.suggestCopyAlternatives(storeService.get(), current);
    }

    public record ImageEditRequest(String imageUrl, String instruction) {
    }

    /** 구독: AI 이미지 수정 (저장은 모달의 "저장하기"에서) */
    @PostMapping("/api/wizard/drafts/{key}/image")
    @ResponseBody
    public AiService.EditedImage editImage(@PathVariable String key, @RequestBody ImageEditRequest body) {
        requirePro();
        Campaign draft = requireDraft();
        return aiService.editImage(body.imageUrl(), body.instruction(), draft.getImage().getMood());
    }

    @PostMapping("/campaigns/new/4")
    public String saveSelection(@RequestParam(defaultValue = "") String selectedKeys) {
        Campaign draft = requireDraft();
        boolean ok = wizardService.saveSelection(draft, Arrays.stream(selectedKeys.split(",")).map(String::trim).toList());
        return "redirect:/campaigns/new/" + (ok ? "5" : "4");
    }

    /* ---------- 5단계: 게시 ---------- */

    @PostMapping("/api/wizard/publish")
    @ResponseBody
    public Map<String, Long> publish() {
        Campaign campaign = wizardService.publish(requireDraft());
        return Map.of("campaignId", campaign.getCampaignId());
    }

    /** 게시 완료 카드 */
    @GetMapping("/campaigns/new/done")
    public String published(@RequestParam("campaign") Long campaignId, Model model) {
        Campaign campaign = campaignService.find(campaignId);
        if (!campaign.isPublished()) {
            return "redirect:/campaigns/new/5";
        }
        model.addAttribute("draft", campaign);
        model.addAttribute("step", "5");
        model.addAttribute("stepIndex", 5);
        model.addAttribute("menuNames", wizardService.menuNames(campaign));
        model.addAttribute("published", true);
        return "wizard/step5";
    }

    public record ReminderRequest(boolean on) {
    }

    @PostMapping("/api/campaigns/{id}/reminder")
    @ResponseBody
    public Map<String, Object> reminder(@PathVariable Long id, @RequestBody ReminderRequest body) {
        Campaign campaign = campaignService.find(id);
        wizardService.setReminder(campaign, body.on());
        return Map.of("on", campaign.getReminderAt() != null);
    }

    /* ---------- 1단계: 목표 설정 ---------- */

    @PostMapping("/campaigns/new/1")
    public String saveGoal(@RequestParam(defaultValue = "") String goalOption,
            @RequestParam(defaultValue = "") String customGoal, @RequestParam(defaultValue = "") String productIds,
            @RequestParam(defaultValue = "") String newMenuNames, @RequestParam(defaultValue = "") String targetCustomers,
            @RequestParam(defaultValue = "") String toneManners, @RequestParam(defaultValue = "") String extraNote) {
        Campaign draft = requireDraft();
        List<Long> ids = Arrays.stream(productIds.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .map(Long::valueOf).toList();
        List<String> newMenus = Arrays.stream(newMenuNames.split("\\|")).toList();
        wizardService.saveGoal(draft, goalOption, customGoal, ids, newMenus, targetCustomers, toneManners, extraNote);
        if (draft.getGoal().isEmpty() || draft.getProductIds().isEmpty()) {
            draft.setLockedStep(null);
            return "redirect:/campaigns/new/1";
        }
        return "redirect:/campaigns/new/2";
    }

    /* ---------- 2단계: AI 기획 ---------- */

    /** 처음 들어왔을 때 AI 기획 생성 */
    @PostMapping("/api/wizard/plan")
    @ResponseBody
    public AiPlan generatePlan() {
        Campaign draft = requireDraft();
        if (draft.getPlan().isEmpty()) {
            draft.setPlan(aiService.generatePlan(storeService.get(), draft));
            wizardService.touch(draft);
        }
        return draft.getPlan();
    }

    public record ReanalyzeRequest(String feedback) {
    }

    /** 구독 사용자: 원하는 방향을 적고 다시 분석 */
    @PostMapping("/api/wizard/plan/reanalyze")
    @ResponseBody
    public AiPlan reanalyzePlan(@RequestBody ReanalyzeRequest request) {
        Campaign draft = requireDraft();
        if (memberService.current().isFree()) {
            throw new ForbiddenException("구독 플랜에서 사용할 수 있어요");
        }
        draft.setPlan(aiService.reanalyzePlan(storeService.get(), draft, request.feedback()));
        wizardService.touch(draft);
        return draft.getPlan();
    }

    /** 사장님이 고친 기획을 확정 */
    @PostMapping("/campaigns/new/2")
    public String savePlan(@RequestParam(defaultValue = "") String ageGroup,
            @RequestParam(defaultValue = "") String targetCustomer, @RequestParam(defaultValue = "") String strategy,
            @RequestParam(defaultValue = "") String tones, @RequestParam(defaultValue = "") String channels) {
        Campaign draft = requireDraft();
        AiPlan plan = draft.getPlan();
        if (plan.isEmpty()) {
            return "redirect:/campaigns/new/2";
        }
        plan.setAgeGroup(ageGroup.trim());
        plan.setTargetCustomer(targetCustomer.trim());
        if (!strategy.isBlank()) {
            plan.setStrategy(strategy.trim());
        }
        plan.setTones(Tags.split(tones));
        plan.setChannels(Tags.split(channels));
        wizardService.lock(draft, "2");
        return "redirect:/campaigns/new/3-1";
    }

    private void requirePro() {
        if (memberService.current().isFree()) {
            throw new ForbiddenException("구독 플랜에서 사용할 수 있어요");
        }
    }

    private Campaign requireDraft() {
        Campaign draft = wizardService.activeDraft();
        if (draft == null) {
            throw new NotFoundException("작성 중인 캠페인이 없습니다");
        }
        return draft;
    }
}
