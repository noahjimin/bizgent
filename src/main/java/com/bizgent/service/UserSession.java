package com.bizgent.service;

import java.io.Serializable;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import lombok.Getter;
import lombok.Setter;

/**
 * 로그인 세션 상태 (HttpSession에 보관). 회원·매장·캠페인 등 데이터는 모두 DB(MyBatis)에 있다.
 * (목업 단계의 UserSession에서 세션 상태만 남겨 이름을 바꿨다.)
 */
@Getter
@Setter
@Component
@SessionScope
public class UserSession implements Serializable {

    /** 로그인한 회원 ID. 로그아웃 상태면 null */
    private Long memberId;
    /** 랜딩에서 누른 버튼: start / heroImage */
    private String startIntent;
    /** 개발 바로가기: AI 캠페인 개선 방향 데이터 양 early(적음) / enough(충분) */
    private String insightsView = "early";
    /** 현재 위자드에서 작성 중인 캠페인 */
    private Long activeDraftId;

    public boolean isLoggedIn() {
        return memberId != null;
    }

    public void logout() {
        memberId = null;
        startIntent = null;
        activeDraftId = null;
        insightsView = "early";
    }
}
