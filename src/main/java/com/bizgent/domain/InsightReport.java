package com.bizgent.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.bizgent.service.ai.AiService.Insights;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AI 개선 방향 분석 기록 (INSIGHT, INSIGHT_CAMPAIGN).
 * asOf: 이 날짜까지의 캠페인·매출로 분석 (null이면 전체), signature: 분석에 쓴 데이터 서명 (같으면 다시 분석하지 않음),
 * result: 분석 결과 (시연용 지난 기록은 처음 볼 때 채운다)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InsightReport {

    private Long reportId;
    private LocalDateTime createdAt;
    private LocalDate asOf;
    private String signature;
    private Insights result;

    public String getTypeLabel() {
        return result != null && "enough".equals(result.view()) ? "Good·Bad 비교" : "초기 분석";
    }

    public String getTargetLabel() {
        if (result == null || result.targets().isEmpty()) {
            return "분석한 캠페인 없음";
        }
        return String.join(" vs ", result.targets().stream().map(t -> t.title()).toList());
    }
}
