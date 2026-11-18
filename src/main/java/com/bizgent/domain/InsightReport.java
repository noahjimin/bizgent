package com.bizgent.domain;

import java.time.LocalDateTime;

import com.bizgent.service.ai.AiService.Insights;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AI 개선 방향 분석 기록 (INSIGHT + INSIGHT_ITEM, INSIGHT_DIFF, INSIGHT_CAMPAIGN).
 * baseUploadId: 분석할 때 마지막 매출 업로드. 그 뒤로 새 업로드가 없으면 저장된 분석을 그대로 보여 준다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InsightReport {

    private Long reportId;
    private LocalDateTime createdAt;
    private Long baseUploadId;
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
