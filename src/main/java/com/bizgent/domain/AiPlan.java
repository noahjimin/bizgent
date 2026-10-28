package com.bizgent.domain;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/** AI 캠페인 기획 결과 (AI_PLAN) */
@Getter
@Setter
public class AiPlan {

    private String direction = "";
    private String targetCustomer = "";
    private String strategy = "";
    private String expectedEffect = "";
    private String ageGroup;
    private List<String> channels = new ArrayList<>();
    private List<String> tones = new ArrayList<>();
    private String reach;
    private String frequency;
    private String duration;
    /** 구독 사용자가 직접 입력한 재분석 요청 (AI_PLAN.USER_REQUEST) */
    private String userRequest;

    public AiPlan() {
    }

    public AiPlan(String direction, String targetCustomer, String strategy, String expectedEffect) {
        this.direction = direction;
        this.targetCustomer = targetCustomer;
        this.strategy = strategy;
        this.expectedEffect = expectedEffect;
    }

    public boolean isEmpty() {
        return direction == null || direction.isBlank();
    }
}
