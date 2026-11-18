package com.bizgent.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bizgent.service.MemberService;

import lombok.RequiredArgsConstructor;

/** 랜딩(SCR-U-000) · 로그인 · 회원가입(약관 동의 포함) */
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;

    @GetMapping("/landing")
    public String landing(Model model) {
        return landingPage(model, false);
    }

    /** 랜딩 화면. 로그인(SCR-U-001)도 별도 화면이 아니라 랜딩 위의 팝업으로 띄운다. */
    private String landingPage(Model model, boolean showLogin) {
        model.addAttribute("showcaseRows", LandingShowcase.ROWS);
        model.addAttribute("season", LandingSeason.of(java.time.LocalDate.now()));
        model.addAttribute("showLogin", showLogin);
        return "auth/landing";
    }

    /** 랜딩의 시작 버튼은 모두 여기로 온다. intent=heroImage면 로그인(또는 가입) 후 대표 이미지 카드를 연다. */
    @GetMapping("/login")
    public String loginForm(@RequestParam(required = false) String intent, Model model) {
        memberService.rememberStartIntent(intent);
        return landingPage(model, true);
    }

    @PostMapping("/login")
    public String login(@RequestParam(defaultValue = "") String email, @RequestParam(defaultValue = "") String password,
            Model model) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (email.isBlank()) errors.put("email", "이메일을 입력해주세요");
        else if (!MemberService.isValidEmail(email)) errors.put("email", "이메일 형식을 확인해주세요");
        if (password.isBlank()) errors.put("password", "비밀번호를 입력해주세요");
        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            model.addAttribute("email", email);
            return landingPage(model, true);
        }
        return "redirect:" + memberService.login(email);
    }

    /** intent는 로그인 화면에서 기억한 값이 세션에 남아 가입 후에도 쓰인다. */
    @GetMapping("/signup")
    public String signupForm(@RequestParam(required = false) String intent, Model model) {
        memberService.rememberStartIntent(intent);
        model.addAttribute("categories", StoreController.CATEGORIES);
        return "auth/signup";
    }

    /** 가입 화면 이메일 칸 blur 시 확인: invalid(형식 오류) / taken(이미 가입) / ok */
    @GetMapping("/api/auth/email-check")
    @ResponseBody
    public Map<String, String> checkEmail(@RequestParam(defaultValue = "") String email) {
        String status = !MemberService.isValidEmail(email) ? "invalid" : memberService.isRegistered(email) ? "taken" : "ok";
        return Map.of("status", status);
    }

    @PostMapping("/signup")
    public String signup(@RequestParam(defaultValue = "") String memberName, @RequestParam(defaultValue = "") String email,
            @RequestParam(defaultValue = "") String password, @RequestParam(defaultValue = "") String passwordCheck,
            @RequestParam(defaultValue = "") String storeName, @RequestParam(defaultValue = "") String category,
            @RequestParam(defaultValue = "") String phone, @RequestParam(defaultValue = "false") boolean agreeTerms,
            Model model) {
        Map<String, String> errors = new LinkedHashMap<>();
        int nameLength = memberName.trim().length();
        if (nameLength == 0) errors.put("memberName", "이름을 입력해주세요");
        else if (nameLength < 2 || nameLength > 20) errors.put("memberName", "이름은 2~20자로 입력해주세요");
        if (email.isBlank()) errors.put("email", "이메일을 입력해주세요");
        else if (!MemberService.isValidEmail(email)) errors.put("email", "이메일 형식을 확인해주세요");
        else if (memberService.isRegistered(email)) errors.put("email", "이미 가입된 이메일입니다");
        String passwordProblem = MemberService.passwordProblem(password);
        if (password.isEmpty()) errors.put("password", "비밀번호를 입력해주세요");
        else if (passwordProblem != null) errors.put("password", passwordProblem);
        if (passwordCheck.isEmpty()) errors.put("passwordCheck", "비밀번호 확인을 입력해주세요");
        else if (!password.equals(passwordCheck)) errors.put("passwordCheck", "비밀번호가 일치하지 않습니다");
        if (storeName.isBlank()) errors.put("storeName", "매장명을 입력해주세요");
        if (category.isBlank()) errors.put("category", "업종을 선택해주세요");
        else if (!StoreController.CATEGORIES.contains(category)) errors.put("category", "목록에서 업종을 선택해주세요");
        if (!phone.isBlank() && !MemberService.isValidPhone(phone)) errors.put("phone", "연락처는 010-0000-0000 형식으로 입력해주세요");
        if (!agreeTerms) errors.put("terms", "필수 약관에 동의해주세요");
        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            model.addAttribute("memberName", memberName);
            model.addAttribute("email", email);
            model.addAttribute("storeName", storeName);
            model.addAttribute("category", category);
            model.addAttribute("phone", phone);
            model.addAttribute("categories", StoreController.CATEGORIES);
            model.addAttribute("agreeTerms", agreeTerms);
            return "auth/signup";
        }
        return "redirect:" + memberService.signup(memberName, email, storeName, category, phone);
    }

    @PostMapping("/logout")
    public String logout() {
        memberService.logout();
        return "redirect:/landing";
    }

    /** 회원 탈퇴 (SCR-U-COM 비고: 확인 팝업 후 처리) */
    @PostMapping("/account/withdraw")
    public String withdraw(RedirectAttributes redirect) {
        memberService.withdraw();
        redirect.addFlashAttribute("toast", "탈퇴가 완료되었어요");
        return "redirect:/landing";
    }
}
