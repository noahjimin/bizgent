package com.bizgent.web;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.bizgent.demo.DemoSession;

import lombok.RequiredArgsConstructor;

/** 레이아웃(사이드바, 설정 메뉴)에서 공통으로 쓰는 회원·매장 정보 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final DemoSession session;

    @ModelAttribute
    public void addCommon(Model model) {
        model.addAttribute("me", session.getMember());
        model.addAttribute("store", session.getStore());
        model.addAttribute("insightsView", session.getInsightsView());
    }
}
