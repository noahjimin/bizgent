package com.bizgent.demo;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.bizgent.domain.AiPlan;
import com.bizgent.domain.Campaign;
import com.bizgent.domain.CampaignStatus;
import com.bizgent.domain.Draft;
import com.bizgent.domain.Member;
import com.bizgent.domain.Notification;
import com.bizgent.domain.Plan;
import com.bizgent.domain.Post;
import com.bizgent.domain.Product;
import com.bizgent.domain.Sale;
import com.bizgent.domain.Store;

/** 시연용 가상 매장·캠페인·매출 데이터 (프로토타입 seed.ts 이식) */
final class DemoSeed {

    private static final String UNSPLASH = "https://images.unsplash.com/";

    private DemoSeed() {
    }

    static void fill(DemoSession s) {
        s.setLoggedIn(false);
        s.setTermsAgreed(false);
        s.setStartIntent(null);
        s.setInsightsView("early");
        s.setActiveDraftId(null);

        Member member = new Member();
        member.setMemberId(1L);
        member.setMemberName("김사장");
        member.setEmail("jieun@example.com");
        member.setPlan(Plan.FREE);
        member.setCredits(12);
        member.setCampaignsUsedThisMonth(1);
        member.setUsageResetAt(LocalDate.now().withDayOfMonth(1).plusMonths(1));
        s.setMember(member);

        Store store = new Store();
        store.setStoreId(1L);
        store.setStoreName("모먼트 카페");
        store.setCategory("카페·디저트");
        store.setDescription("성수동 골목에서 제철 재료로 만든 음료와 디저트를 선보이는 작은 카페입니다.");
        store.setTargetCustomer("20대, 30대, 직장인");
        store.setToneManner("친근한, 감성적인");
        store.setHeroImageUrl(UNSPLASH + "photo-1554118811-1e0d58224f24?auto=format&fit=crop&w=1600&q=80");
        store.setProducts(new ArrayList<>(List.of(
                new Product(1L, "아메리카노", 4500, 48, null, null),
                new Product(2L, "크루아상", 4200, 40, null, null),
                new Product(3L, "시그니처 케이크", 7800, 80, null, null),
                new Product(4L, "브런치 세트", 15800, 12, null, null))));
        s.setStore(store);

        s.setCampaigns(new ArrayList<>(campaigns()));
        s.setSales(sales(store.getProducts()));

        LocalDateTime tomorrow = LocalDate.now().plusDays(1).atTime(10, 0);
        s.setNotifications(new ArrayList<>(List.of(new Notification(1L, 3L,
                "주말 디저트 캠페인의 성과를 확인하고 다음 개선 방향을 살펴보세요.", tomorrow, false))));
    }

    private static List<Campaign> campaigns() {
        Campaign c1 = published(1L, "10월 신메뉴 프로모션", "/assets/2cbe7.png", LocalDateTime.of(2025, 10, 5, 9, 0),
                "봄 시즌 신메뉴 알리기", 1L,
                new AiPlan("부드러운 봄날의 한 잔", "감성적인 카페를 찾는 20~30대 직장인",
                        "시그니처 크림의 질감과 여유로운 오후를 연결해 신메뉴 호기심을 높입니다.", "신메뉴 인지도와 평일 오후 방문 증가"),
                List.of("인스타그램", "네이버 플레이스"), true, "따뜻하고 화사한 봄", "음료 중심의 테이블 컷");
        String latte = UNSPLASH + "photo-1570968915860-54d5c301fa9f?auto=format&fit=crop&w=1200&q=80";
        c1.setDrafts(new ArrayList<>(List.of(
                new Draft("A", "인스타그램", "봄처럼 부드러운 오늘의 한 잔, 모먼트 카페 시그니처 크림 라떼를 만나보세요.",
                        new ArrayList<>(List.of("모먼트카페", "성수카페", "크림라떼")), latte),
                new Draft("B", "네이버 플레이스", "고소한 에스프레소와 부드러운 수제 크림이 어우러진 봄 시즌 라떼입니다.",
                        new ArrayList<>(List.of("성수동카페", "신메뉴")), latte))));
        c1.setPosts(new ArrayList<>(List.of(Post.result("인스타그램", 4820, 386, 74),
                Post.result("네이버 플레이스", 2110, 241, 51))));

        Campaign c2 = published(2L, "9월 신메뉴 프로모션", "/assets/eece9.png", LocalDateTime.of(2025, 9, 18, 9, 0),
                "평일 오후 방문 늘리기", 3L,
                new AiPlan("오후 세 시, 달콤한 쉼표", "성수동 인근 직장인과 프리랜서",
                        "커피와 스콘 세트의 합리적인 가격을 강조합니다.", "평일 14~17시 매출 증가"),
                List.of("인스타그램", "카카오톡 채널"), false, "포근하고 차분한 분위기", "커피와 스콘 세트");
        c2.setDrafts(new ArrayList<>(List.of(new Draft("A", "카카오톡 채널",
                "오후 2시부터 5시까지, 커피와 바닐라빈 스콘 세트로 잠깐의 여유를 즐겨보세요.", new ArrayList<>(),
                UNSPLASH + "photo-1555507036-ab1f4038808a?auto=format&fit=crop&w=1200&q=80"))));
        c2.setPosts(new ArrayList<>(List.of(Post.result("카카오톡 채널", 1890, 304, 92))));

        Campaign c3 = published(3L, "추석 할인 이벤트", "/assets/afdbb.png", LocalDateTime.of(2025, 9, 12, 9, 0),
                "주말 디저트 판매 늘리기", 4L,
                new AiPlan("주말을 위한 작은 축하", "주말 데이트를 즐기는 20~30대 고객",
                        "딸기와 생크림의 신선함을 시각적으로 강조합니다.", "주말 디저트 주문과 동반 방문 증가"),
                List.of("인스타그램"), true, "생기 있고 사랑스러운 분위기", "케이크 클로즈업");
        c3.setDrafts(new ArrayList<>(List.of(new Draft("A", "인스타그램",
                "이번 주말에는 제철 딸기를 가득 올린 생크림 케이크로 달콤하게 쉬어가세요.",
                new ArrayList<>(List.of("딸기케이크", "성수데이트", "모먼트카페")),
                UNSPLASH + "photo-1565958011703-44f9829ba187?auto=format&fit=crop&w=1200&q=80"))));
        c3.setPosts(new ArrayList<>(List.of(Post.result("인스타그램", 3240, 278, 63))));
        c3.setReminderAt(LocalDate.now().plusDays(1).atTime(10, 0));

        Campaign c4 = new Campaign();
        c4.setCampaignId(4L);
        c4.setTitle("가을 시즌 메뉴 소개");
        c4.setThumbnailUrl("/assets/a299e.png");
        c4.setCreatedAt(LocalDateTime.of(2025, 10, 3, 9, 0));
        c4.setUpdatedAt(c4.getCreatedAt());
        c4.setGoal("가을 시즌 메뉴 알리기");
        c4.setProductIds(new ArrayList<>(List.of(1L)));
        c4.getContent().setChannels(new ArrayList<>(List.of("인스타그램")));
        c4.getContent().setHashtagOn(true);
        c4.getContent().setImageOn(true);

        return List.of(c1, c2, c3, c4);
    }

    private static Campaign published(Long id, String title, String thumbnail, LocalDateTime at, String goal,
            Long productId, AiPlan plan, List<String> channels, boolean hashtagOn, String mood, String composition) {
        Campaign c = new Campaign();
        c.setCampaignId(id);
        c.setTitle(title);
        c.setThumbnailUrl(thumbnail);
        c.setCreatedAt(at);
        c.setUpdatedAt(at);
        c.setPublishedAt(at);
        c.setStatus(CampaignStatus.PUBLISHED);
        c.setGoal(goal);
        c.setProductIds(new ArrayList<>(List.of(productId)));
        c.setPlan(plan);
        c.getContent().setChannels(new ArrayList<>(channels));
        c.getContent().setHashtagOn(hashtagOn);
        c.getContent().setImageOn(true);
        c.getImage().setMood(mood);
        c.getImage().setComposition(composition);
        c.setSelectedDraftKeys(new ArrayList<>(List.of("A")));
        return c;
    }

    private record Lift(long productId, int from, int days, double factor) {
    }

    /**
     * 2025-09-01 ~ 2025-10-12 (42일) 메뉴별 일 매출. 게시된 캠페인과 맞춰
     * 추석 할인 이벤트(9/12, 브런치 세트)와 9월 신메뉴 프로모션(9/18, 시그니처 케이크)은 뚜렷하게 오르고,
     * 10월 신메뉴 프로모션(10/5, 아메리카노)은 효과가 거의 없게 만든다.
     */
    private static List<Sale> sales(List<Product> products) {
        LocalDate start = LocalDate.of(2025, 9, 1);
        Map<Long, Integer> baseQty = Map.of(1L, 14, 2L, 6, 3L, 4, 4L, 2);
        List<Lift> lifts = List.of(new Lift(4, 11, 8, 2.1), new Lift(3, 17, 10, 1.8), new Lift(1, 34, 8, 1.02));
        String[] sources = {"excel", "receipt", "manual"};

        List<Sale> sales = new ArrayList<>();
        long id = 1;
        for (int day = 0; day < 42; day++) {
            LocalDate date = start.plusDays(day);
            boolean weekend = date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
            for (int m = 0; m < products.size(); m++) {
                Product product = products.get(m);
                int noise = ((day * 7 + m * 13) % 5) - 2;
                int d = day;
                double factor = lifts.stream()
                        .filter(l -> l.productId() == product.getProductId() && d >= l.from() && d < l.from() + l.days())
                        .mapToDouble(Lift::factor).findFirst().orElse(1);
                int qty = (int) Math.max(1, Math.round(
                        (baseQty.get(product.getProductId()) * (weekend ? 1.3 : 1) + noise * 0.6) * factor));
                sales.add(new Sale(id++, date, product.getProductId(), qty, (long) product.getPrice() * qty,
                        sources[(day + m) % sources.length]));
            }
        }
        return sales;
    }
}
