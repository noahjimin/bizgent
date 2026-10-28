package com.bizgent.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.bizgent.domain.Campaign;
import com.bizgent.service.CampaignService;
import com.bizgent.service.StoreService;

import lombok.RequiredArgsConstructor;

/** 캠페인 보관함 (SCR-U-009, F13) */
@Controller
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;
    private final StoreService storeService;

    @GetMapping("/campaigns")
    public String list(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "newest") String sort, @RequestParam(defaultValue = "grid") String view,
            @RequestParam(required = false) Long campaign, Model model) {
        model.addAttribute("campaigns", campaignService.search(q, status, sort));
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("sort", sort);
        model.addAttribute("view", view);
        if (campaign != null) {
            Campaign opened = campaignService.find(campaign);
            model.addAttribute("opened", opened);
            model.addAttribute("openedMenus", String.join(" · ",
                    opened.getProductIds().stream().map(storeService.get()::productName).filter(n -> !n.isEmpty()).toList()));
        }
        return "campaigns";
    }

    /** 작성 중인 캠페인을 마지막 단계부터 이어서 만든다. */
    @PostMapping("/campaigns/{id}/resume")
    public String resume(@PathVariable Long id) {
        Campaign campaign = campaignService.resume(id);
        return "redirect:/campaigns/new/" + campaign.getResumeStep();
    }
}
