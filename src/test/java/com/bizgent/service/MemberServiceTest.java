package com.bizgent.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.bizgent.mapper.MemberMapper;
import com.bizgent.mapper.StoreMapper;

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
    void registeredEmailIsLookedUpTrimmed() {
        MemberMapper members = mock(MemberMapper.class);
        when(members.countByEmail("JIEUN@example.com")).thenReturn(1);
        MemberService service = new MemberService(new UserSession(), members, mock(StoreMapper.class), mock(RequestData.class));
        assertTrue(service.isRegistered(" JIEUN@example.com"));
        assertFalse(service.isRegistered("new@cafe.kr"));
    }
}
