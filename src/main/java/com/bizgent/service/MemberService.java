package com.bizgent.service;

import org.springframework.stereotype.Service;

import com.bizgent.demo.DemoSession;
import com.bizgent.domain.Member;

import lombok.RequiredArgsConstructor;

/** 회원가입·로그인·약관 동의·계정 관리 (F01) */
@Service
@RequiredArgsConstructor
public class MemberService {

    /** 랜딩의 "매장 대표 이미지 무료 생성하기"로 들어오면 로그인 후 매장 화면에서 AI 이미지 카드를 연다. */
    public static final String INTENT_HERO_IMAGE = "heroImage";

    private final DemoSession session;

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
