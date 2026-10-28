package com.bizgent.web;

import java.util.Map;

import org.springframework.stereotype.Component;

/** 사이드바·홈에서 쓰는 24px 라인 아이콘 경로. 템플릿에서 ${@icons.path('home')}로 사용 */
@Component("icons")
public class Icons {

    private static final Map<String, String> PATHS = Map.ofEntries(
            Map.entry("home", "M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z"),
            Map.entry("store", "M3 9l1.5-5h15L21 9M3 9v1a3 3 0 0 0 6 0 3 3 0 0 0 6 0 3 3 0 0 0 6 0V9M3 9h18M5 13v8h14v-8M10 21v-5h4v5"),
            Map.entry("archive", "M3 4h18v4H3zM5 8v12h14V8M10 12h4"),
            Map.entry("chart", "M4 20V10M10 20V4M16 20v-7M22 20H2"),
            Map.entry("bulb", "M9 18h6M10 22h4M12 2a7 7 0 0 0-4 12.7V17h8v-2.3A7 7 0 0 0 12 2z"),
            Map.entry("ticket", "M3 8a2 2 0 0 0 0 4v0a2 2 0 0 1 0 4v2h18v-2a2 2 0 0 1 0-4 2 2 0 0 1 0-4V6H3zM14 6v12"),
            Map.entry("bell", "M6 8a6 6 0 1 1 12 0c0 7 3 9 3 9H3s3-2 3-9M10.3 21a2 2 0 0 0 3.4 0"),
            Map.entry("plus", "M12 5v14M5 12h14"),
            Map.entry("sparkle", "M12 3l1.9 5.1L19 10l-5.1 1.9L12 17l-1.9-5.1L5 10l5.1-1.9zM19 15l.8 2.2L22 18l-2.2.8L19 21l-.8-2.2L16 18l2.2-.8z"),
            Map.entry("send", "M5 12h14M13 6l6 6-6 6"),
            Map.entry("crown", "M3 8l4.5 4L12 5l4.5 7L21 8l-2 11H5z"),
            Map.entry("pencil", "M4 20h4L19 9a2.8 2.8 0 0 0-4-4L4 16zM13.5 6.5l4 4"),
            Map.entry("image", "M3 5h18v14H3zM3 16l5-5 4 4 3-3 6 6M15.5 9.5h.01"),
            Map.entry("instagram", "M7 3h10a4 4 0 0 1 4 4v10a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4V7a4 4 0 0 1 4-4zM12 16a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM17.5 6.5h.01"),
            Map.entry("blog", "M4 4h16v16H4zM8 9h8M8 13h8M8 17h5"),
            Map.entry("carrot", "M14 4c1 1.5 3 2 4.5 1.5M16 3c-.5 1.5 0 3.5 1.5 4.5M15.5 8.5 4 20l5-9a5 5 0 0 1 6.5-2.5z"),
            Map.entry("upload", "M12 16V4M7 9l5-5 5 5M4 16v4h16v-4"),
            Map.entry("chevron", "M9 6l6 6-6 6"));

    public String path(String name) {
        return PATHS.getOrDefault(name, "");
    }
}
