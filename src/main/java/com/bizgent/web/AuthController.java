package com.bizgent.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.bizgent.service.MemberService;

import lombok.RequiredArgsConstructor;

/** 랜딩(SCR-U-000) · 이용약관 동의 · 로그인 · 회원가입 */
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;

    @GetMapping("/landing")
    public String landing(Model model) {
        model.addAttribute("showcaseRows", LandingShowcase.ROWS);
        return "auth/landing";
    }

    /** 약관 동의 후 랜딩에서는 로그인으로, 회원가입 화면에서는 그대로 머문다. */
    @PostMapping("/terms/agree")
    public String agreeTerms(@RequestParam(required = false) String intent,
            @RequestParam(defaultValue = "/login") String next) {
        memberService.agreeTerms(intent);
        return "redirect:" + ("/signup".equals(next) ? "/signup" : "/login");
    }

    @GetMapping("/login")
    public String loginForm() {
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam(defaultValue = "") String email, @RequestParam(defaultValue = "") String password,
            Model model) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (email.isBlank()) errors.put("email", "이메일을 입력해주세요");
        if (password.isBlank()) errors.put("password", "비밀번호를 입력해주세요");
        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            model.addAttribute("email", email);
            return "auth/login";
        }
        return "redirect:" + memberService.login(email);
    }

    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("showTerms", !memberService.isTermsAgreed());
        return "auth/signup";
    }

    @PostMapping("/signup")
    public String signup(@RequestParam(defaultValue = "") String email, @RequestParam(defaultValue = "") String password,
            @RequestParam(defaultValue = "") String passwordCheck, @RequestParam(defaultValue = "") String storeName,
            Model model) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (email.isBlank()) errors.put("email", "이메일을 입력해주세요");
        if (password.isEmpty()) errors.put("password", "비밀번호를 입력해주세요");
        if (passwordCheck.isEmpty()) errors.put("passwordCheck", "비밀번호 확인을 입력해주세요");
        else if (!password.equals(passwordCheck)) errors.put("passwordCheck", "비밀번호가 일치하지 않습니다");
        if (storeName.isBlank()) errors.put("storeName", "매장명을 입력해주세요");
        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            model.addAttribute("email", email);
            model.addAttribute("storeName", storeName);
            model.addAttribute("showTerms", !memberService.isTermsAgreed());
            return "auth/signup";
        }
        return "redirect:" + memberService.signup(email, storeName);
    }

    @PostMapping("/logout")
    public String logout() {
        memberService.logout();
        return "redirect:/landing";
    }
}
