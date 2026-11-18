package com.bizgent.config;

import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.bizgent.service.UserSession;
import com.bizgent.service.MemberService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 로그인 전 화면(랜딩·로그인·회원가입)과 로그인 후 화면을 나눈다.
 * 로그인하지 않으면 랜딩으로, 로그인한 상태에서 로그인 전 화면에 오면 홈으로 보낸다.
 * 개발 바로가기(/dev/*)는 시연 계정만 쓸 수 있다 (그 밖에는 403).
 */
@Component
@RequiredArgsConstructor
public class LoginCheckInterceptor implements HandlerInterceptor {

    private static final Set<String> PUBLIC_ONLY = Set.of("/landing", "/login", "/signup");

    private final UserSession session;
    private final MemberService memberService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean publicOnly = PUBLIC_ONLY.contains(path);
        // 세션의 회원이 탈퇴했거나 없으면 로그아웃 상태로 본다.
        boolean loggedIn = session.isLoggedIn() && memberService.current() != null;
        if (session.isLoggedIn() && !loggedIn) {
            session.logout();
        }

        if (publicOnly && loggedIn) {
            response.sendRedirect(request.getContextPath() + "/");
            return false;
        }
        if (!publicOnly && !loggedIn) {
            response.sendRedirect(request.getContextPath() + "/landing");
            return false;
        }
        if (path.startsWith("/dev/") && !memberService.isDemo()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "시연 계정에서만 쓸 수 있어요");
            return false;
        }
        return true;
    }
}
