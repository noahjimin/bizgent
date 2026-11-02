package com.bizgent.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.bizgent.demo.DemoSession;
import com.bizgent.domain.Campaign;
import com.bizgent.domain.Notification;

import lombok.RequiredArgsConstructor;

/** 성과 입력 알림 (F18). 게시 7일 뒤 알림을 사이드바·상단바 알림 목록에 보여준다. */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final DateTimeFormatter DUE_DATE = DateTimeFormatter.ofPattern("MM.dd");

    private final DemoSession session;

    /** 알림 목록 한 줄. due: 알림 날짜가 지나 지금 입력할 수 있는지 */
    public record Item(Long notificationId, String campaignTitle, String message, boolean due, String dueLabel,
            boolean read) {
    }

    /** 때가 된 알림을 위에, 예정 알림은 날짜가 가까운 순으로 */
    public List<Item> findAll() {
        LocalDateTime now = LocalDateTime.now();
        return session.getNotifications().stream()
                .sorted(Comparator.comparing((Notification n) -> !isDue(n, now))
                        .thenComparing(Notification::getDueAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(n -> new Item(n.getNotificationId(), campaignTitle(n.getCampaignId()), n.getMessage(),
                        isDue(n, now), isDue(n, now) ? "지금 입력할 수 있어요" : n.getDueAt().format(DUE_DATE) + " 예정",
                        n.isRead()))
                .toList();
    }

    /** 종 아이콘의 빨간 점: 때가 됐는데 아직 읽지 않은 알림이 있을 때만 */
    public boolean hasUnreadDue() {
        LocalDateTime now = LocalDateTime.now();
        return session.getNotifications().stream().anyMatch(n -> !n.isRead() && isDue(n, now));
    }

    public void markRead(Long notificationId) {
        session.getNotifications().stream()
                .filter(n -> n.getNotificationId().equals(notificationId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("알림을 찾을 수 없습니다: " + notificationId))
                .setRead(true);
    }

    private static boolean isDue(Notification n, LocalDateTime now) {
        return n.getDueAt() == null || !n.getDueAt().isAfter(now);
    }

    private String campaignTitle(Long campaignId) {
        return session.getCampaigns().stream()
                .filter(c -> c.getCampaignId().equals(campaignId))
                .map(Campaign::getTitle)
                .findFirst()
                .orElse("");
    }
}
