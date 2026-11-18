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

import com.bizgent.demo.DemoSeed;
import com.bizgent.service.UserSession;
import com.bizgent.domain.Campaign;
import com.bizgent.service.CampaignService;
import com.bizgent.service.CampaignService.CampaignPrefill;
import com.bizgent.service.InsightService;
import com.bizgent.service.MemberService;
import com.bizgent.service.StoreService;

import lombok.RequiredArgsConstructor;

/** 홈 대시보드(F10), 새 캠페인 시작, 계정 관리, 개발 바로가기 */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final CampaignService campaignService;
    private final MemberService memberService;
    private final UserSession session;
    private final StoreService storeService;
    private final InsightService insightService;
    private final DemoSeed demoSeed;

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

    /* ---------- 개발 바로가기 (시연 계정 전용: LoginCheckInterceptor가 그 밖에는 403) ---------- */

    /** 이번 달 캠페인 이용 횟수(MONTHLY_USAGE)와 매장 이미지 생성 횟수(STORE_IMAGE)를 함께 소진하거나 초기화한다. */
    @PostMapping("/dev/usage")
    public String devUsage(@RequestParam int used, @RequestHeader(value = "Referer", required = false) String referer) {
        demoSeed.setUsage(memberService.current(), storeService.get(), Math.max(0, used));
        return "redirect:" + backPath(referer);
    }

    /** 결제 없이 무료 ↔ 구독 플랜 전환 (SUBSCRIPTION 추가·해지) */
    @PostMapping("/dev/plan")
    public String devPlan(@RequestParam String plan, @RequestHeader(value = "Referer", required = false) String referer) {
        if ("pro".equals(plan)) {
            memberService.subscribe();
        } else {
            memberService.cancelSubscription();
        }
        return "redirect:" + backPath(referer);
    }

    /** AI 개선 방향 데이터 양: 바꾸면 바로 새로 분석해서 기록으로 남긴다. */
    @PostMapping("/dev/insights-view")
    public String devInsightsView(@RequestParam String view,
            @RequestHeader(value = "Referer", required = false) String referer) {
        session.setInsightsView("enough".equals(view) ? "enough" : "early");
        insightService.reanalyze();
        return "redirect:" + backPath(referer);
    }

    /** 시연 계정의 데이터만 지우고 처음 상태로 되돌린 뒤 로그아웃한다. */
    @PostMapping("/dev/reset")
    public String devReset() {
        demoSeed.reset();
        memberService.logout();
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
