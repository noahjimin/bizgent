package com.bizgent.web;

import java.net.URI;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.support.RequestContextUtils;

import com.bizgent.service.UserSession;
import com.bizgent.domain.Member;
import com.bizgent.domain.Plan;
import com.bizgent.service.CodeService;
import com.bizgent.service.MemberService;
import com.bizgent.service.NotificationService;
import com.bizgent.service.StoreService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/** 레이아웃(사이드바, 설정 메뉴, 알림 목록)에서 공통으로 쓰는 회원·매장·알림 정보 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private static final String UPLOAD_TOO_LARGE = "파일 용량이 너무 커요. 10MB 이하 파일로 다시 올려 주세요.";

    private final UserSession session;
    private final NotificationService notificationService;
    private final MemberService memberService;
    private final StoreService storeService;
    private final CodeService codes;

    @Value("${bizgent.demo.email}")
    private String demoEmail;
    @Value("${bizgent.demo.password}")
    private String demoPassword;

    @ModelAttribute
    public void addCommon(Model model) {
        // 랜딩·약관처럼 로그인 전 화면에서도 두 요금제의 한도(PRICE_PLAN)를 보여 준다.
        model.addAttribute("freePlan", codes.pricePlan(Plan.FREE));
        model.addAttribute("proPlan", codes.pricePlan(Plan.PRO));
        // 로그인 화면 "임시 계정" 카드에 공개하는 시연 계정
        model.addAttribute("demoEmail", demoEmail);
        model.addAttribute("demoPassword", demoPassword);
        Member me = memberService.current();
        if (me == null) {
            return;
        }
        model.addAttribute("me", me);
        model.addAttribute("store", storeService.get());
        // 개발 바로가기는 시연 계정에서만 보인다.
        model.addAttribute("isDemo", memberService.isDemo());
        model.addAttribute("insightsView", session.getInsightsView());
        model.addAttribute("notifications", notificationService.findAll());
        model.addAttribute("hasDueNotification", notificationService.hasUnreadDue());
    }

    /** 업로드 한도(application.yml) 초과: 오류 페이지 대신 화면 요청은 이전 화면 + 토스트, API는 413 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> uploadTooLarge(HttpServletRequest request, HttpServletResponse response) {
        if (request.getRequestURI().startsWith("/api/")) {
            return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body(Map.of("message", UPLOAD_TOO_LARGE));
        }
        String back = backPath(request.getHeader("Referer"));
        RequestContextUtils.getOutputFlashMap(request).put("toast", UPLOAD_TOO_LARGE);
        RequestContextUtils.saveOutputFlashMap(back, request, response);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(back)).build();
    }

    /** 다른 사이트로 보내지 않도록 Referer의 경로만 쓴다. */
    private static String backPath(String referer) {
        try {
            String path = referer == null ? null : URI.create(referer).getPath();
            return path == null || path.isEmpty() ? "/" : path;
        } catch (IllegalArgumentException e) {
            return "/";
        }
    }
}
