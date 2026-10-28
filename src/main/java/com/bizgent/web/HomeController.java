package com.bizgent.web;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bizgent.demo.DemoSession;
import com.bizgent.domain.Campaign;
import com.bizgent.domain.Plan;
import com.bizgent.service.CampaignService;
import com.bizgent.service.CampaignService.CampaignPrefill;
import com.bizgent.service.MemberService;

import lombok.RequiredArgsConstructor;

/** 홈 대시보드(F10), 새 캠페인 시작, 계정 관리, 개발 바로가기 */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final CampaignService campaignService;
    private final MemberService memberService;
    private final DemoSession session;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("recent", campaignService.findRecent(10));
        return "home";
    }

    /** 사이드바 "만들기", 홈 버튼, 개선 방향 제안 등 모든 새 캠페인의 시작점 */
    @PostMapping("/campaigns/new")
    public String startCampaign(@RequestParam(required = false) String goal,
            @RequestParam(required = false) Long productId, @RequestParam(required = false) String channels,
            @RequestParam(required = false) String source,
            @RequestHeader(value = "Referer", required = false) String referer, RedirectAttributes redirect) {
        List<String> channelList = channels == null || channels.isBlank() ? List.of()
                : Arrays.stream(channels.split(",")).map(String::trim).toList();
        Campaign campaign = campaignService.start(new CampaignPrefill(goal, productId, channelList, source));
        if (campaign == null) {
            redirect.addFlashAttribute("upgradeNotice", true);
            return "redirect:" + backPath(referer);
        }
        return "redirect:/campaigns/new/1";
    }

    @PostMapping("/account")
    public String updateAccount(@RequestParam String memberName, @RequestParam(required = false) String phone,
            @RequestHeader(value = "Referer", required = false) String referer, RedirectAttributes redirect) {
        if (!memberName.isBlank()) {
            memberService.updateAccount(memberName, phone);
            redirect.addFlashAttribute("toast", "계정 정보가 저장되었어요");
        }
        return "redirect:" + backPath(referer);
    }

    /* ---------- 개발 바로가기 (시연 데이터 조작) ---------- */

    @PostMapping("/dev/usage")
    public String devUsage(@RequestParam int used, @RequestHeader(value = "Referer", required = false) String referer) {
        session.getMember().setCampaignsUsedThisMonth(used);
        return "redirect:" + backPath(referer);
    }

    /** 결제 없이 무료 ↔ 구독 플랜 전환 (구독 전용 기능 시연용) */
    @PostMapping("/dev/plan")
    public String devPlan(@RequestParam String plan, @RequestHeader(value = "Referer", required = false) String referer) {
        session.getMember().setPlan("pro".equals(plan) ? Plan.PRO : Plan.FREE);
        return "redirect:" + backPath(referer);
    }

    @PostMapping("/dev/insights-view")
    public String devInsightsView(@RequestParam String view,
            @RequestHeader(value = "Referer", required = false) String referer) {
        session.setInsightsView("enough".equals(view) ? "enough" : "early");
        return "redirect:" + backPath(referer);
    }

    @PostMapping("/dev/reset")
    public String devReset() {
        session.reset();
        return "redirect:/landing";
    }

    /** 같은 사이트의 이전 화면 경로 (없으면 홈) */
    static String backPath(String referer) {
        if (referer == null) {
            return "/";
        }
        int start = referer.indexOf('/', referer.indexOf("//") + 2);
        return start < 0 ? "/" : referer.substring(start);
    }
}
