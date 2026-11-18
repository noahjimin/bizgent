package com.bizgent.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bizgent.domain.Notification;

/** 알림: NOTIFICATION */
@Mapper
public interface NotificationMapper {

    List<Notification> findByMember(Long memberId);

    void insert(@Param("memberId") Long memberId, @Param("campaignId") Long campaignId, @Param("type") String type,
            @Param("message") String message, @Param("dueAt") LocalDateTime dueAt);

    int markRead(@Param("memberId") Long memberId, @Param("notificationId") Long notificationId);

    void deleteByCampaign(@Param("campaignId") Long campaignId, @Param("type") String type);

    void deleteByMember(Long memberId);
}
