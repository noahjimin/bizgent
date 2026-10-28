package com.bizgent.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 채널별 게시 결과 (POST) */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Post {

    private String channel;
    /** 시안별로 기록된 결과일 때만 값이 있다 */
    private String draftKey;
    /** "success" / "failed" */
    private String status;
    private int views;
    private int clicks;
    private int conversions;

    public static Post result(String channel, int views, int clicks, int conversions) {
        return new Post(channel, null, null, views, clicks, conversions);
    }

    public boolean isSuccess() {
        return "success".equals(status);
    }
}
