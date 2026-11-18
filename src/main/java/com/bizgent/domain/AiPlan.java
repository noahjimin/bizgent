package com.bizgent.domain;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

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

    /** 저장된 버전 (AI_PLAN.VERSION_NO). 새로 만든 기획은 null이고 저장할 때 다음 버전으로 들어간다. */
    @JsonIgnore
    private Integer versionNo;
    /** AI / REANALYZE / USER_EDIT (AI_PLAN.PLAN_SOURCE) */
    @JsonIgnore
    private String source = "AI";
    /** 불러온 시점의 내용. 저장할 때 내용이 달라졌으면 USER_EDIT 새 버전으로 남긴다. */
    @JsonIgnore
    private String savedContent;

    /** 버전 비교용 내용 문자열 */
    public String snapshot() {
        return String.join("\u0001", String.valueOf(direction), String.valueOf(targetCustomer), String.valueOf(strategy),
                String.valueOf(expectedEffect), String.valueOf(ageGroup), String.valueOf(channels), String.valueOf(tones),
                String.valueOf(reach), String.valueOf(frequency), String.valueOf(duration), String.valueOf(userRequest));
    }

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
