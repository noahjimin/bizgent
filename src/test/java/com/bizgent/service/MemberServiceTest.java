package com.bizgent.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.bizgent.demo.DemoSession;

class MemberServiceTest {

    @Test
    void emailFormat() {
        assertTrue(MemberService.isValidEmail("owner@cafe.kr"));
        assertTrue(MemberService.isValidEmail(" owner@cafe.co.kr "));
        assertFalse(MemberService.isValidEmail("abc"));
        assertFalse(MemberService.isValidEmail("abc@cafe"));
        assertFalse(MemberService.isValidEmail("a b@cafe.kr"));
    }

    @Test
    void registeredEmail() {
        MemberService service = new MemberService(new DemoSession());
        assertTrue(service.isRegistered(" JIEUN@example.com"));
        assertFalse(service.isRegistered("new@cafe.kr"));
    }
}
