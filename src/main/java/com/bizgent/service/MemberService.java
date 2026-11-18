package com.bizgent.service;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bizgent.domain.Member;
import com.bizgent.domain.Plan;
import com.bizgent.domain.Store;
import com.bizgent.mapper.MemberMapper;
import com.bizgent.mapper.StoreMapper;

import lombok.RequiredArgsConstructor;

/** 회원가입·로그인·계정 관리 (F01): MEMBERS, STORE, SUBSCRIPTION */
@Service
@RequiredArgsConstructor
public class MemberService {

    /** 랜딩의 "매장 대표 이미지 무료 생성하기"로 들어오면 로그인 후 매장 화면에서 AI 이미지 카드를 연다. */
    public static final String INTENT_HERO_IMAGE = "heroImage";

    public static final PasswordEncoder PASSWORDS = new BCryptPasswordEncoder();

    private static final Pattern EMAIL = Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");
    private static final Pattern PHONE = Pattern.compile("010-\\d{4}-\\d{4}");

    private final UserSession session;
    private final MemberMapper memberMapper;
    private final StoreMapper storeMapper;
    private final RequestData data;

    @Value("${bizgent.demo.email}")
    private String demoEmail;

    /** 이메일 형식 검사 (SCR-U-001 ①⑤) */
    public static boolean isValidEmail(String email) {
        return EMAIL.matcher(email.trim()).matches();
    }

    /** 연락처 형식 검사: 010-0000-0000 */
    public static boolean isValidPhone(String phone) {
        return PHONE.matcher(phone.trim()).matches();
    }

    /** 이미 가입된 이메일인지 (탈퇴한 회원의 이메일도 포함: UK_MEMBERS_EMAIL) */
    public boolean isRegistered(String email) {
        return memberMapper.countByEmail(email.trim()) > 0;
    }

    /** @return 로그인하지 않았으면 null */
    public Member current() {
        return data.member();
    }

    /** 시연 계정으로 로그인했는지 (개발 바로가기 표시·허용) */
    public boolean isDemo() {
        Member m = current();
        return m != null && demoEmail.equalsIgnoreCase(m.getEmail());
    }

    /** 비밀번호 규칙(8~20자, 영문·숫자 포함) 검사. 맞으면 null, 아니면 부족한 점 안내 문구 */
    public static String passwordProblem(String password) {
        if (password.length() < 8 || password.length() > 20) return "8~20자로 입력해주세요";
        if (!password.matches(".*[A-Za-z].*")) return "영문을 포함해주세요";
        if (!password.matches(".*\\d.*")) return "숫자를 포함해주세요";
        return null;
    }

    /** 랜딩에서 누른 시작 버튼을 기억한다. 알 수 없는 값은 무시한다. */
    public void rememberStartIntent(String intent) {
        if (INTENT_HERO_IMAGE.equals(intent)) session.setStartIntent(intent);
    }

    /**
     * 비밀번호를 BCrypt 해시와 비교한다. 탈퇴한 회원은 로그인할 수 없다.
     *
     * @return 로그인 후 이동할 경로. 이메일·비밀번호가 맞지 않으면 null
     */
    public String login(String email, String password) {
        Member m = memberMapper.findByEmail(email.trim());
        if (m == null || !"ACTIVE".equals(m.getMemberStatus()) || !PASSWORDS.matches(password, m.getPassword())) {
            return null;
        }
        session.setMemberId(m.getMemberId());
        data.clear();
        return consumeStartPath();
    }

    /** 회원과 매장(회원당 1개)을 함께 만든다. 약관 동의 여부는 컨트롤러가 먼저 확인한다. */
    @Transactional
    public String signup(String memberName, String email, String password, String storeName, String category,
            String phone) {
        Member member = new Member();
        member.setMemberName(memberName.trim());
        member.setEmail(email.trim());
        member.setPassword(PASSWORDS.encode(password));
        member.setPhone(phone.isBlank() ? null : phone.trim());
        member.setTermsAgreedAt(LocalDateTime.now());
        memberMapper.insert(member);

        Store store = new Store();
        store.setStoreName(storeName.trim());
        store.setCategory(category);
        storeMapper.insertStore(member.getMemberId(), store);

        session.setMemberId(member.getMemberId());
        data.clear();
        return consumeStartPath();
    }

    public void logout() {
        session.logout();
        data.clear();
    }

    /** 구독 시작. shortcut: 결제 없이 SUBSCRIPTION 행만 추가한다(PROJECT.md 모의 결제), 실제 PG 연동은 범위 밖 */
    @Transactional
    public void subscribe() {
        Member m = current();
        if (m != null && m.isFree()) {
            memberMapper.insertSubscription(m.getMemberId(), Plan.PRO.name());
            data.clear();
        }
    }

    /** 구독 해지: ACTIVE 구독을 CANCELED로 바꾼다 (무료 플랜으로 돌아감) */
    public void cancelSubscription() {
        Member m = current();
        if (m != null) {
            memberMapper.cancelSubscriptions(m.getMemberId());
            data.clear();
        }
    }

    /** 회원 탈퇴: 상태를 WITHDRAWN으로 바꾸고 일시를 기록한 뒤 로그아웃한다. 15일 후 삭제는 별도 작업으로 한다. */
    public void withdraw() {
        Member m = current();
        if (m != null) {
            memberMapper.withdraw(m.getMemberId());
        }
        logout();
    }

    public void updateAccount(String memberName, String phone) {
        Member m = current();
        memberMapper.updateAccount(m.getMemberId(), memberName.trim(),
                phone == null || phone.isBlank() ? null : phone.trim());
        data.clear();
    }

    private String consumeStartPath() {
        if (INTENT_HERO_IMAGE.equals(session.getStartIntent())) {
            session.setStartIntent(null);
            return "/store?imageCard=open";
        }
        return "/";
    }
}
