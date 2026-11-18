package com.bizgent.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bizgent.domain.Notification;
import com.bizgent.mapper.row.CampaignChannelRow;
import com.bizgent.mapper.row.CampaignProductRow;
import com.bizgent.mapper.row.CampaignRow;
import com.bizgent.mapper.row.DraftRow;
import com.bizgent.mapper.row.HashtagRow;
import com.bizgent.mapper.row.ImageSettingRow;
import com.bizgent.mapper.row.PlanRow;
import com.bizgent.mapper.row.PostRow;

/**
 * 캠페인과 하위 기록: CAMPAIGN, CAMPAIGN_PRODUCT, CAMPAIGN_CHANNEL, AI_PLAN, IMAGE_SETTING, DRAFT, DRAFT_HASHTAG,
 * POST, POST_METRIC. 조회는 매장 단위(campaignId가 null이면 매장의 모든 캠페인)로 한 번에 읽는다.
 */
@Mapper
public interface CampaignMapper {

    List<CampaignRow> findCampaigns(@Param("storeId") Long storeId, @Param("campaignId") Long campaignId);

    List<CampaignProductRow> findProducts(@Param("storeId") Long storeId, @Param("campaignId") Long campaignId);

    List<CampaignChannelRow> findChannels(@Param("storeId") Long storeId, @Param("campaignId") Long campaignId);

    /** 캠페인마다 가장 최근 버전의 AI 기획 */
    List<PlanRow> findLatestPlans(@Param("storeId") Long storeId, @Param("campaignId") Long campaignId);

    List<ImageSettingRow> findImageSettings(@Param("storeId") Long storeId, @Param("campaignId") Long campaignId);

    List<DraftRow> findDrafts(@Param("storeId") Long storeId, @Param("campaignId") Long campaignId);

    List<HashtagRow> findHashtags(@Param("storeId") Long storeId, @Param("campaignId") Long campaignId);

    /** 게시 결과와 가장 최근 게시물 반응 */
    List<PostRow> findPosts(@Param("storeId") Long storeId, @Param("campaignId") Long campaignId);

    /** 성과 입력 알림 일시 (campaignId, dueAt) */
    List<Notification> findReminders(@Param("storeId") Long storeId, @Param("campaignId") Long campaignId,
            @Param("type") String type);

    void insertCampaign(CampaignRow row);

    void updateCampaign(CampaignRow row);

    /** 마지막으로 보던 단계·확정 단계만 바꾼다 */
    void updateSteps(@Param("campaignId") Long campaignId, @Param("currentStep") String currentStep,
            @Param("lockedStep") String lockedStep);

    void deleteProducts(Long campaignId);

    void insertProduct(CampaignProductRow row);

    void deleteChannels(Long campaignId);

    void insertChannel(CampaignChannelRow row);

    /** 새 버전 번호(최근 버전 + 1)로 저장한다 */
    void insertPlan(PlanRow row);

    void deletePlans(Long campaignId);

    void confirmLatestPlan(Long campaignId);

    void deleteImageSetting(Long campaignId);

    void insertImageSetting(ImageSettingRow row);

    void deleteDraftsExcept(@Param("campaignId") Long campaignId, @Param("keepIds") List<Long> keepIds);

    void insertDraft(DraftRow row);

    void updateDraft(DraftRow row);

    void deleteHashtags(Long draftId);

    void insertHashtag(HashtagRow row);

    void insertPost(PostRow row);

    void insertPostMetric(PostRow row);

    /** 이 캠페인을 분석에 쓴 AI 개선 방향 기록 수 (INSIGHT_CAMPAIGN은 삭제 시 함께 지워지지 않는다) */
    int countInsightLinks(Long campaignId);

    /** 하위 기록은 FK ON DELETE CASCADE로 함께 지워진다 */
    int deleteCampaign(@Param("storeId") Long storeId, @Param("campaignId") Long campaignId);

    void clearInsightIds(Long storeId);

    void deleteByStore(Long storeId);
}
