package com.bizgent.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 알림 (NOTIFICATION) */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    private Long notificationId;
    private Long campaignId;
    private String message;
    private LocalDateTime dueAt;
    private boolean read;
}
