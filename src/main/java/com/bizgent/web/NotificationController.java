package com.bizgent.web;

import java.util.Map;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bizgent.service.NotificationService;

import lombok.RequiredArgsConstructor;

/** 알림 목록 (SCR-U-COM ⑧): 항목을 누르면 읽음 처리 */
@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/api/notifications/{id}/read")
    public Map<String, Boolean> read(@PathVariable Long id) {
        notificationService.markRead(id);
        return Map.of("read", true);
    }
}
