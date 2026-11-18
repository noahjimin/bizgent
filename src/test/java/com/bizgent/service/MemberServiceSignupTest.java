package com.bizgent.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.bizgent.domain.Member;
import com.bizgent.domain.Store;
import com.bizgent.mapper.MemberMapper;
import com.bizgent.mapper.StoreMapper;

class MemberServiceSignupTest {

    private final UserSession session = new UserSession();
    private final MemberMapper members = mock(MemberMapper.class);
    private final StoreMapper stores = mock(StoreMapper.class);
    private final MemberService service = new MemberService(session, members, stores, mock(RequestData.class));

    @Test
    void signupSavesHashedPasswordAndStoreTogether() {
        doAnswer(inv -> {
            inv.<Member>getArgument(0).setMemberId(7L);
            return null;
        }).when(members).insert(any());
        service.rememberStartIntent("unknown"); // 알 수 없는 값은 무시
        service.rememberStartIntent(MemberService.INTENT_HERO_IMAGE);

        assertEquals("/store?imageCard=open",
                service.signup(" 홍길동 ", "gd@ex.com", "abcd1234", "길동 분식", "분식", "010-1234-5678"));

        ArgumentCaptor<Member> member = ArgumentCaptor.forClass(Member.class);
        verify(members).insert(member.capture());
        assertEquals("홍길동", member.getValue().getMemberName());
        assertNotEquals("abcd1234", member.getValue().getPassword(), "비밀번호는 해시로 저장");
        assertTrue(MemberService.PASSWORDS.matches("abcd1234", member.getValue().getPassword()));
        assertNotNull(member.getValue().getTermsAgreedAt());

        ArgumentCaptor<Store> store = ArgumentCaptor.forClass(Store.class);
        verify(stores).insertStore(eq(7L), store.capture());
        assertEquals("길동 분식", store.getValue().getStoreName());
        assertEquals(7L, session.getMemberId());
    }

    @Test
    void loginChecksPasswordAndRejectsWithdrawnMember() {
        Member m = new Member();
        m.setMemberId(3L);
        m.setPassword(MemberService.PASSWORDS.encode("abcd1234"));
        m.setMemberStatus("ACTIVE");
        when(members.findByEmail("a@b.kr")).thenReturn(m);

        assertNull(service.login("a@b.kr", "wrong999"));
        assertFalse(session.isLoggedIn());
        assertEquals("/", service.login(" a@b.kr ", "abcd1234"));
        assertEquals(3L, session.getMemberId());

        session.logout();
        m.setMemberStatus("WITHDRAWN");
        assertNull(service.login("a@b.kr", "abcd1234"), "탈퇴한 회원은 로그인할 수 없다");
    }

    @Test
    void phoneFormat() {
        assertTrue(MemberService.isValidPhone("010-1234-5678"));
        assertFalse(MemberService.isValidPhone("01012345678"));
        assertFalse(MemberService.isValidPhone("011-1234-5678"));
        assertFalse(MemberService.isValidPhone("010-123-5678"));
    }
}
