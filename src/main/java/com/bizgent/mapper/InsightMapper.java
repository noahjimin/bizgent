package com.bizgent.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.bizgent.mapper.row.InsightCampaignRow;
import com.bizgent.mapper.row.InsightDiffRow;
import com.bizgent.mapper.row.InsightItemRow;
import com.bizgent.mapper.row.InsightRow;

/** AI 캠페인 개선 방향 기록: INSIGHT, INSIGHT_ITEM, INSIGHT_DIFF, INSIGHT_CAMPAIGN */
@Mapper
public interface InsightMapper {

    /** 최근 분석부터 */
    List<InsightRow> findInsights(Long storeId);

    List<InsightItemRow> findItems(Long storeId);

    List<InsightDiffRow> findDiffs(Long storeId);

    List<InsightCampaignRow> findCampaigns(Long storeId);

    Long findLatestId(Long storeId);

    void insertInsight(InsightRow row);

    void insertItem(InsightItemRow row);

    void insertDiff(InsightDiffRow row);

    void insertCampaign(InsightCampaignRow row);

    /** 하위 기록(항목·차이·비교 캠페인)은 FK ON DELETE CASCADE로 함께 지워진다 */
    void deleteByStore(Long storeId);
}
