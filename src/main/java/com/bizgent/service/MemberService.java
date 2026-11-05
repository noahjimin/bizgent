package com.bizgent.service;

import java.time.LocalDateTime;
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
    private static final Pattern PHONE = Pattern.compile("010-\\d{4}-\\d{4}");

    private final DemoSession session;

    /** 이메일 형식 검사 (SCR-U-001 ①⑤) */
    public static boolean isValidEmail(String email) {
        return EMAIL.matcher(email.trim()).matches();
    }

    /** 연락처 형식 검사: 010-0000-0000 */
    public static boolean isValidPhone(String phone) {
        return PHONE.matcher(phone.trim()).matches();
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
        session.setTermsAgreedAt(LocalDateTime.now());
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

    /** 이름·연락처·약관 동의는 회원 정보에, 매장명·업종은 매장 정보에 저장한다. 약관 동의 여부는 컨트롤러가 먼저 확인한다. */
    public String signup(String memberName, String email, String storeName, String category, String phone) {
        Member member = session.getMember();
        member.setMemberName(memberName.trim());
        member.setEmail(email.trim());
        member.setPhone(phone.isBlank() ? null : phone.trim());
        member.setTermsAgreed(true);
        member.setTermsAgreedAt(session.getTermsAgreedAt());
        session.getStore().setStoreName(storeName.trim());
        session.getStore().setCategory(category);
        session.setLoggedIn(true);
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
