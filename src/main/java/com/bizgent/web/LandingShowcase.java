package com.bizgent.web;

import java.util.List;

/** 랜딩 "AI 제작 예시" 갤러리 카드. photo 카드는 image가, poster 카드는 bg/text/accent가 있다. */
public record LandingShowcase(String kind, String image, String alt, String eyebrow, String headline, String sub,
        String channel, String width, String bg, String text, String accent) {

    static LandingShowcase photo(String image, String alt, String eyebrow, String headline, String channel,
            String width) {
        return new LandingShowcase("photo", image, alt, eyebrow, headline, null, channel, width, null, null, null);
    }

    static LandingShowcase poster(String eyebrow, String headline, String sub, String channel, String width,
            String bg, String text, String accent) {
        return new LandingShowcase("poster", null, null, eyebrow, headline, sub, channel, width, bg, text, accent);
    }

    public boolean isPhoto() {
        return "photo".equals(kind);
    }

    public static final List<List<LandingShowcase>> ROWS = List.of(
            List.of(
                    photo("2cbe7.png", "밤 라떼와 밤 페이스트리", "가을 한정", "고소한 밤 라떼,\n이번 주만 만나요", "인스타그램", "w-[360px]"),
                    poster("NEW MENU", "가을 신메뉴\n고구마 라떼 출시", "달콤한 군고구마 향 가득", "인스타그램", "w-[240px]", "bg-[#fff1e3]", "text-[#7a3e0e]", "bg-[#f97316]"),
                    poster("LUNCH SPECIAL", "점심 특가\n떡볶이 4,900원", "평일 11:30 – 14:00", "당근마켓", "w-[280px]", "bg-[#e11d48]", "text-white", "bg-[#ffd60a]"),
                    photo("afdbb.png", "크루아상과 샐러드, 아이스 라떼", "브런치 세트", "창가 자리에서\n여유로운 브런치", "블로그", "w-[320px]"),
                    poster("공지", "추석 연휴\n휴무 안내", "9.16(월) – 9.18(수) 휴무", "카카오톡 채널", "w-[230px]", "bg-[#eaf2ff]", "text-[#1d3a8a]", "bg-[#2563eb]"),
                    photo("eece9.png", "보자기로 포장한 배 선물세트", "명절 선물", "정성 담은\n추석 선물세트 예약", "광고 이미지", "w-[340px]"),
                    poster("COUPON", "재방문 고객\n10% 할인 쿠폰", "다음 방문 시 바로 사용", "카카오톡 채널", "w-[260px]", "bg-[#14b8a6]", "text-white", "bg-white")),
            List.of(
                    poster("WEEKEND ONLY", "주말 한정\n디저트 세트", "음료 + 케이크 9,900원", "인스타그램", "w-[260px]", "bg-[#fdecf3]", "text-[#9d174d]", "bg-[#db2777]"),
                    photo("2d925.png", "라떼와 크루아상", "오늘의 추천", "갓 구운 크루아상과\n따뜻한 라떼 한 잔", "광고 이미지", "w-[340px]"),
                    poster("BLACK FRIDAY", "11월\n블랙프라이데이 세일", "전 품목 최대 30%", "광고 이미지", "w-[300px]", "bg-[#0c1b3d]", "text-white", "bg-[#ffd60a]"),
                    photo("a299e.png", "베이커리에서 포장 상자를 건네는 사장님", "단골 이벤트", "포장 주문하면\n쿠키 하나 더", "당근마켓", "w-[330px]"),
                    poster("FLOWER", "꽃다발\n예약 주문 받아요", "기념일 3일 전까지 예약", "블로그", "w-[240px]", "bg-[#ddf8f3]", "text-[#0f5e55]", "bg-[#14b8a6]"),
                    photo("d26d2.png", "창가 자리의 커피와 디저트", "힐링 타임", "햇살 좋은 오후,\n디저트 한 접시", "인스타그램", "w-[300px]"),
                    poster("GRAND OPEN", "신규 오픈\n기념 이벤트", "첫 방문 아메리카노 무료", "당근마켓", "w-[270px]", "bg-[#2563eb]", "text-white", "bg-[#14b8a6]")));
}
