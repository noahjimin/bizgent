package com.bizgent.web;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bizgent.domain.Tags;
import com.bizgent.service.StoreService;
import com.bizgent.service.ai.AiService;
import com.bizgent.service.ai.AiService.GeneratedImage;
import com.bizgent.service.ai.AiService.StoreImageRequest;
import com.bizgent.web.form.StoreForm;

import lombok.RequiredArgsConstructor;

/** 매장 정보 관리 (SCR-U-004~006, F02) */
@Controller
@RequiredArgsConstructor
public class StoreController {

    static final List<String> CATEGORIES = List.of("카페/베이커리", "카페·디저트", "한식", "양식", "일식", "중식", "분식", "주점/바", "기타");
    static final List<String> TARGET_PRESETS = List.of("10대", "20대", "30대", "40대 이상", "직장인", "학생", "가족 단위", "1인 가구");
    static final List<String> TONE_PRESETS = List.of("친근한", "전문적인", "트렌디한", "감성적인", "신뢰감있는", "유쾌한");

    private final StoreService storeService;
    private final AiService aiService;

    @GetMapping("/store")
    public String store(@RequestParam(required = false) String imageCard, Model model) {
        var store = storeService.get();
        model.addAttribute("categories", CATEGORIES.contains(store.getCategory()) || store.getCategory() == null
                ? CATEGORIES : prepend(store.getCategory(), CATEGORIES));
        model.addAttribute("targetPresets", TARGET_PRESETS);
        model.addAttribute("tonePresets", TONE_PRESETS);
        model.addAttribute("targets", Tags.split(store.getTargetCustomer()));
        model.addAttribute("tones", Tags.split(store.getToneManner()));
        model.addAttribute("openImageCard", "open".equals(imageCard));
        return "store";
    }

    @PostMapping("/store")
    public String save(@ModelAttribute StoreForm form, RedirectAttributes redirect) {
        storeService.save(form);
        redirect.addFlashAttribute("toast", "매장 정보가 저장되었어요");
        return "redirect:/store";
    }

    /** AI 매장 대표 이미지 3장 생성 */
    @PostMapping("/api/store/hero-images")
    @ResponseBody
    public List<GeneratedImage> generateHeroImages(@RequestBody StoreImageRequest request) {
        return aiService.generateStoreImages(storeService.get(), request);
    }

    @PostMapping("/api/store/hero-image")
    @ResponseBody
    public Map<String, String> applyHeroImage(@RequestBody Map<String, String> body) {
        storeService.setHeroImage(body.get("imageUrl"));
        return Map.of("imageUrl", body.get("imageUrl"));
    }

    private static List<String> prepend(String first, List<String> rest) {
        return Stream.concat(Stream.of(first), rest.stream()).toList();
    }
}
