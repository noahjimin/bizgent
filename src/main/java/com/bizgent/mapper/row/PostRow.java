package com.bizgent.mapper.row;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** POST 한 줄 + 최근 POST_METRIC */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostRow {

    private Long postId;
    private Long draftId;
    private Long campaignId;
    private String channelCode;
    private String draftType;
    private String postStatus;
    private LocalDateTime postedAt;
    private int likeCount;
    private int commentCount;
    private int shareCount;
    private int saveCount;
    private int reachCount;
}
