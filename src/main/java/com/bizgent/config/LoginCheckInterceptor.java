package com.bizgent.config;

import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.bizgent.demo.DemoSession;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 로그인 전 화면(랜딩·로그인·회원가입)과 로그인 후 화면을 나눈다.
 * 로그인하지 않으면 랜딩으로, 로그인한 상태에서 로그인 전 화면에 오면 홈으로 보낸다.
 */
@Component
@RequiredArgsConstructor
public class LoginCheckInterceptor implements HandlerInterceptor {

    private static final Set<String> PUBLIC_ONLY = Set.of("/landing", "/login", "/signup", "/terms/agree");

    private final DemoSession session;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean publicOnly = PUBLIC_ONLY.contains(path);

        if (publicOnly && session.isLoggedIn()) {
            response.sendRedirect(request.getContextPath() + "/");
            return false;
        }
        if (!publicOnly && !session.isLoggedIn()) {
            response.sendRedirect(request.getContextPath() + "/landing");
            return false;
        }
        return true;
    }
}
