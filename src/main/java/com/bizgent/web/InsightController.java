package com.bizgent.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bizgent.domain.InsightReport;
import com.bizgent.service.CampaignService;
import com.bizgent.service.InsightService;
import com.bizgent.service.MemberService;

import lombok.RequiredArgsConstructor;

/** AI 캠페인 개선 방향 (SCR-U-011, F19) · 구독 관리 및 크레딧 (SCR-U-012, F14·F15) */
@Controller
@RequiredArgsConstructor
public class InsightController {

    private final InsightService insightService;
    private final MemberService memberService;
    private final CampaignService campaignService;

    /** @param report 지난 분석 기록 ID (없으면 지금 데이터 기준 분석) */
    @GetMapping("/insights")
    public String insights(@RequestParam(required = false) Long report, Model model) {
        InsightService.Current current = insightService.current();
        InsightReport shown = report == null ? null : insightService.find(report);
        if (report != null && shown == null) {
            model.addAttribute("toast", "분석 기록을 찾을 수 없어 최신 분석을 보여드려요.");
        }
        if (shown == null) {
            shown = current.report();
        }
        boolean latest = shown == current.report();
        model.addAttribute("report", shown);
        model.addAttribute("result", shown.getResult());
        model.addAttribute("latest", latest);
        model.addAttribute("history", insightService.history());
        model.addAttribute("currentId", current.report().getReportId());
        // 새로 분석했을 때만 "분석 중" 화면을 잠깐 보여 준다.
        model.addAttribute("analyzing", latest && current.created());
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
