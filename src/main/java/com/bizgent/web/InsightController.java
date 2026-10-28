package com.bizgent.web;

import java.util.Objects;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.bizgent.demo.DemoSession;
import com.bizgent.service.ai.AiService;

import lombok.RequiredArgsConstructor;

/** AI 캠페인 개선 방향 (SCR-U-011, F19) · 구독 관리 및 크레딧 (SCR-U-012, F14·F15) */
@Controller
@RequiredArgsConstructor
public class InsightController {

    private final AiService aiService;
    private final DemoSession session;

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
    public String subscription() {
        return "subscription";
    }
}
