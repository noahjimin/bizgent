package com.bizgent.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 채널별 게시 결과 (POST) */
@Getter
@Setter
@NoArgsConstructor
public class Post {

    private String channel;
    /** 시안별로 기록된 결과일 때만 값이 있다 */
    private String draftKey;
    /** "success" / "failed" */
    private String status;
    private int views;
    private int clicks;
    private int conversions;
    /** 게시물 반응. 인스타그램 연동 전이라 예시 데이터다 */
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

    public static Post result(String channel, int views, int clicks, int conversions) {
        Post post = new Post(channel, null, null);
        post.views = views;
        post.clicks = clicks;
        post.conversions = conversions;
        return post;
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
        return "success".equals(status);
    }
}
