package com.bizgent.service;

import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.bizgent.demo.DemoSession;
import com.bizgent.domain.Member;
import com.bizgent.domain.Plan;

import lombok.RequiredArgsConstructor;

/** 회원가입·로그인·약관 동의·계정 관리 (F01) */
@Service
@RequiredArgsConstructor
public class MemberService {

    /** 랜딩의 "매장 대표 이미지 무료 생성하기"로 들어오면 로그인 후 매장 화면에서 AI 이미지 카드를 연다. */
    public static final String INTENT_HERO_IMAGE = "heroImage";

    private static final Pattern EMAIL = Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");

    private final DemoSession session;

    /** 이메일 형식 검사 (SCR-U-001 ①⑤) */
    public static boolean isValidEmail(String email) {
        return EMAIL.matcher(email.trim()).matches();
    }

    /** 이미 가입된 이메일인지 (SCR-U-001 ⑤) */
    public boolean isRegistered(String email) {
        // shortcut: 시연 계정 이메일만 가입된 것으로 본다, DB 연동 시 회원 테이블 조회로 교체
        return DemoSession.DEMO_EMAIL.equalsIgnoreCase(email.trim());
    }

    public Member current() {
        return session.getMember();
    }

    public boolean isTermsAgreed() {
        return session.isTermsAgreed();
    }

    public void agreeTerms(String intent) {
        session.setTermsAgreed(true);
        if (intent != null && !intent.isBlank()) {
            session.setStartIntent(intent);
        } else if (session.getStartIntent() == null) {
            session.setStartIntent("start");
        }
    }

    /** 데모 단계에서는 비밀번호를 검사하지 않는다. 로그인 후 이동할 경로를 돌려준다. */
    public String login(String email) {
        session.setLoggedIn(true);
        session.getMember().setEmail(email.trim());
        return consumeStartPath();
    }

    public String signup(String email, String storeName) {
        session.setLoggedIn(true);
        session.getMember().setEmail(email.trim());
        session.getMember().setMemberName(storeName.trim());
        session.getStore().setStoreName(storeName.trim());
        return consumeStartPath();
    }

    public void logout() {
        session.setLoggedIn(false);
    }

    /** 구독 플랜 전환. shortcut: 결제 없이 상태값만 바꾼다(PROJECT.md 모의 결제), 실제 PG 연동은 범위 밖 */
    public void subscribe() {
        session.getMember().setPlan(Plan.PRO);
    }

    /** 회원 탈퇴: 회원·매장·캠페인·매출·알림을 지우고 로그아웃한다. */
    public void withdraw() {
        // shortcut: DB가 없어 세션을 시연 초기 상태(로그아웃 포함)로 되돌린다, DB 연동 시 회원 데이터 삭제로 교체
        session.reset();
    }

    public void updateAccount(String memberName, String phone) {
        Member member = session.getMember();
        member.setMemberName(memberName.trim());
        member.setPhone(phone == null || phone.isBlank() ? null : phone.trim());
    }

    private String consumeStartPath() {
        if (INTENT_HERO_IMAGE.equals(session.getStartIntent())) {
            session.setStartIntent(null);
            return "/store?imageCard=open";
        }
        return "/";
    }
}
