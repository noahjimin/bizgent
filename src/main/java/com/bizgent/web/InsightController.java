package com.bizgent.web;

import java.util.Objects;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bizgent.demo.DemoSession;
import com.bizgent.service.CampaignService;
import com.bizgent.service.MemberService;
import com.bizgent.service.ai.AiService;

import lombok.RequiredArgsConstructor;

/** AI 캠페인 개선 방향 (SCR-U-011, F19) · 구독 관리 및 크레딧 (SCR-U-012, F14·F15) */
@Controller
@RequiredArgsConstructor
public class InsightController {

    private final AiService aiService;
    private final DemoSession session;
    private final MemberService memberService;
    private final CampaignService campaignService;

    @GetMapping("/insights")
    public String insights(Model model) {
        String view = session.getInsightsView();
        model.addAttribute("result", aiService.generateInsights(session.getStore(), view, session.getCampaigns(),
                session.getSales()));
        // 같은 데이터로 다시 열 때는 분석 중 화면을 건너뛰도록 화면에 데이터 서명을 넘긴다.
        model.addAttribute("signature", Integer.toHexString(Objects.hash(view,
                session.getCampaigns().stream().map(c -> c.getCampaignId() + ":" + c.getStatus() + ":" + c.getUpdatedAt()).toList(),
                session.getStore().getProducts().stream().map(p -> p.getProductId() + ":" + p.getStockQty()).toList(),
                session.getSales().size())));
        return "insights";
    }

    @GetMapping("/subscription")
    public String subscription(@RequestParam(required = false) Long resume, Model model) {
        // 캠페인 생성 중 '요금제 보기'로 왔으면 작성하던 캠페인으로 돌아가는 버튼을 보여 준다.
        model.addAttribute("resumeCampaign", resume == null ? null : campaignService.findAll().stream()
                .filter(c -> c.getCampaignId().equals(resume) && !c.isPublished()).findFirst().orElse(null));
        return "subscription";
    }

    /** 구독하기 (SCR-U-010 ⑤): 상태값 모의 결제로 구독 플랜 전환 */
    @PostMapping("/subscription")
    public String subscribe(@RequestParam(required = false) Long resume, RedirectAttributes redirect) {
        memberService.subscribe();
        redirect.addFlashAttribute("toast", "구독 플랜으로 전환되었어요");
        return "redirect:/subscription" + (resume == null ? "" : "?resume=" + resume);
    }
}
