package com.bizgent.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.bizgent.demo.DemoSession;

class MemberServiceSignupTest {

    @Test
    void signupSavesMemberAndStoreWithTermsAgreement() {
        DemoSession session = new DemoSession();
        MemberService service = new MemberService(session);
        service.agreeTerms("start");

        service.signup(" 홍길동 ", "gd@ex.com", "길동 분식", "분식", "010-1234-5678");

        assertEquals("홍길동", session.getMember().getMemberName());
        assertEquals("010-1234-5678", session.getMember().getPhone());
        assertTrue(session.getMember().isTermsAgreed());
        assertNotNull(session.getMember().getTermsAgreedAt());
        assertEquals("길동 분식", session.getStore().getStoreName());
        assertEquals("분식", session.getStore().getCategory());

        service.signup("홍길동", "gd@ex.com", "길동 분식", "분식", "");
        assertNull(session.getMember().getPhone());
    }

    @Test
    void phoneFormat() {
        assertTrue(MemberService.isValidPhone("010-1234-5678"));
        assertFalse(MemberService.isValidPhone("01012345678"));
        assertFalse(MemberService.isValidPhone("011-1234-5678"));
        assertFalse(MemberService.isValidPhone("010-123-5678"));
    }
}
