package com.bizgent.mapper.row;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DRAFT_HASHTAG 한 줄 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HashtagRow {

    private Long draftId;
    private int hashtagSeq;
    private String hashtag;
}
