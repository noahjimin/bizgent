package com.bizgent.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/** 가입 비밀번호 규칙: 8~20자, 영문·숫자 포함 (특수문자는 선택) */
class PasswordRuleTest {

    @Test
    void 규칙에_맞지_않으면_부족한_점을_알려준다() {
        assertEquals("8~20자로 입력해주세요", MemberService.passwordProblem("ab12"));
        assertEquals("8~20자로 입력해주세요", MemberService.passwordProblem("abcd1234abcd1234abcd1"));
        assertEquals("영문을 포함해주세요", MemberService.passwordProblem("12345678"));
        assertEquals("숫자를 포함해주세요", MemberService.passwordProblem("abcdefgh"));
    }

    @Test
    void 영문과_숫자를_포함한_8에서_20자는_통과한다() {
        assertNull(MemberService.passwordProblem("abcd1234"));
        assertNull(MemberService.passwordProblem("abcd1234!@#$abcd1234"));
    }
}
