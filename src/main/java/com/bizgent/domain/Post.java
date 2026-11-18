package com.bizgent.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 시안별 게시 결과 (POST) + 최근 게시물 반응 (POST_METRIC) */
@Getter
@Setter
@NoArgsConstructor
public class Post {

    public static final String SUCCESS = "SUCCESS";

    private String channel;
    /** 게시한 시안의 A/B */
    private String draftKey;
    /** SUCCESS / FAILED */
    private String status;
    /** 게시물 반응. 인스타그램 연동 전이라 예시 데이터다 (POST_METRIC.COLLECT_METHOD='SAMPLE') */
    private int likes;
    private int comments;
    private int shares;
    private int saves;
    private int reach;

    public Post(String channel, String draftKey, String status) {
        this.channel = channel;
        this.draftKey = draftKey;
        this.status = status;
    }

    public Post reactions(int likes, int comments, int shares, int saves, int reach) {
        this.likes = likes;
        this.comments = comments;
        this.shares = shares;
        this.saves = saves;
        this.reach = reach;
        return this;
    }

    public boolean isSuccess() {
        return SUCCESS.equals(status);
    }
}
