package com.bizgent.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 콘텐츠 시안 (DRAFT, DRAFT_HASHTAG). 4단계에서는 항상 A/B 두 개를 비교한다. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Draft {

    /** "A" 또는 "B" (DRAFT.DRAFT_TYPE) */
    private String key;
    private String channel;
    private String text;
    private List<String> hashtags = new ArrayList<>();
    private String imageUrl;

    public String getHashtagText() {
        return hashtags.stream().map(tag -> "#" + tag).reduce((a, b) -> a + " " + b).orElse("");
    }

    /** 미리보기 제목: 문구의 첫 문장 */
    public String getHeadline() {
        Matcher m = HEADLINE.matcher(text == null ? "" : text);
        return m.matches() ? m.group(1).trim() : text;
    }

    /** 미리보기 본문: 첫 문장 뒤의 내용 */
    public String getBody() {
        Matcher m = HEADLINE.matcher(text == null ? "" : text);
        if (!m.matches()) {
            return "";
        }
        String rest = m.group(2).trim();
        return rest.isEmpty() ? text : rest;
    }

    private static final Pattern HEADLINE = Pattern.compile("^(.+?[.!?,](?:\\s|$))(.*)$",
            Pattern.DOTALL);

    /** "문구 복사" 버튼으로 복사할 본문 + 해시태그 */
    public String getCopyText() {
        return hashtags.isEmpty() ? text : text + "\n\n" + getHashtagText();
    }
}
