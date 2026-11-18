package com.bizgent.demo;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.bizgent.domain.AiPlan;
import com.bizgent.domain.Campaign;
import com.bizgent.domain.CampaignStatus;
import com.bizgent.domain.Draft;
import com.bizgent.domain.Member;
import com.bizgent.domain.Plan;
import com.bizgent.domain.Post;
import com.bizgent.domain.Product;
import com.bizgent.domain.Sale;
import com.bizgent.domain.Store;
import com.bizgent.mapper.CampaignMapper;
import com.bizgent.mapper.InsightMapper;
import com.bizgent.mapper.MemberMapper;
import com.bizgent.mapper.NotificationMapper;
import com.bizgent.mapper.SalesMapper;
import com.bizgent.mapper.StoreMapper;
import com.bizgent.mapper.row.StoreImageRow;
import com.bizgent.mapper.row.UploadRow;
import com.bizgent.service.CampaignRepository;
import com.bizgent.service.InsightRepository;
import com.bizgent.service.InsightService;
import com.bizgent.service.MemberService;
import com.bizgent.service.NotificationService;

/**
 * 시연 계정(bizgent.demo.email)과 시연 데이터(모먼트 카페: 매장, 상품, 캠페인, 매출, 알림, 분석 이력).
 * 서버를 켤 때 시연 계정이 없으면 만들고, 개발 바로가기 "시연 데이터 초기화"는 시연 계정의 데이터만 지우고 처음 상태로 되돌린다.
 * 다른 회원의 데이터는 건드리지 않는다 (모든 삭제를 시연 계정의 회원 ID·매장 ID로 한정).
 */
@Component
public class DemoSeed implements ApplicationRunner {

    private static final String UNSPLASH = "https://images.unsplash.com/";
    private static final DateTimeFormatter YYYYMM = DateTimeFormatter.ofPattern("yyyyMM");

    private final MemberMapper memberMapper;
    private final StoreMapper storeMapper;
    private final CampaignMapper campaignMapper;
    private final NotificationMapper notificationMapper;
    private final SalesMapper salesMapper;
    private final InsightMapper insightMapper;
    private final CampaignRepository campaignRepository;
    private final InsightRepository insightRepository;
    private final InsightService insightService;
    private final TransactionTemplate tx;
    private final String demoEmail;
    private final String demoPassword;

    public DemoSeed(MemberMapper memberMapper, StoreMapper storeMapper, CampaignMapper campaignMapper,
            NotificationMapper notificationMapper, SalesMapper salesMapper, InsightMapper insightMapper,
            CampaignRepository campaignRepository, InsightRepository insightRepository, InsightService insightService,
            PlatformTransactionManager transactionManager, @Value("${bizgent.demo.email}") String demoEmail,
            @Value("${bizgent.demo.password}") String demoPassword) {
        this.memberMapper = memberMapper;
        this.storeMapper = storeMapper;
        this.campaignMapper = campaignMapper;
        this.notificationMapper = notificationMapper;
        this.salesMapper = salesMapper;
        this.insightMapper = insightMapper;
        this.campaignRepository = campaignRepository;
        this.insightRepository = insightRepository;
        this.insightService = insightService;
        this.tx = new TransactionTemplate(transactionManager);
        this.demoEmail = demoEmail;
        this.demoPassword = demoPassword;
    }

    /** 서버 시작 시 시연 계정이 없으면 만든다 (이미 있으면 그대로 둔다). */
    @Override
    public void run(ApplicationArguments args) {
        if (memberMapper.findByEmail(demoEmail) == null) {
            reset();
        }
    }

    /** 시연 계정의 데이터를 모두 지우고 처음 상태로 채운다. */
    public void reset() {
        tx.executeWithoutResult(status -> {
            Member member = memberMapper.findByEmail(demoEmail);
            if (member == null) {
                member = new Member();
                member.setEmail(demoEmail);
                member.setMemberName("김사장");
                member.setPassword(MemberService.PASSWORDS.encode(demoPassword));
                member.setTermsAgreedAt(LocalDateTime.now());
                memberMapper.insert(member);
            } else {
                wipe(member.getMemberId());
                memberMapper.updateAccount(member.getMemberId(), "김사장", null);
                memberMapper.updatePassword(member.getMemberId(), MemberService.PASSWORDS.encode(demoPassword));
            }
            fill(member.getMemberId());
        });
    }

    /** 개발 바로가기 "횟수 소진·초기화": 이번 달 캠페인 이용 횟수와 매장 이미지 생성 횟수를 함께 바꾼다. */
    public void setUsage(Member member, Store store, int used) {
        tx.executeWithoutResult(status -> {
            memberMapper.setUsage(member.getMemberId(), LocalDate.now().format(YYYYMM), used);
            storeMapper.deleteStoreImagesThisMonth(store.getStoreId());
            if (used > 0) {
                // 생성 횟수는 생성 시각 묶음 수로 세므로, 한도만큼 시각이 다른 기록을 남긴다.
                LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
                for (int i = 0; i < member.getPricePlan().getMonthlyImageLimit(); i++) {
                    storeMapper.insertStoreImage(StoreImageRow.builder().storeId(store.getStoreId()).imageStyle("실사 사진")
                            .mood("-").scene("-").imageUrl(store.getHeroImageUrl() == null ? "-" : store.getHeroImageUrl())
                            .createdAt(now.minusSeconds(i)).build());
                }
            }
        });
    }

    /** 시연 계정의 기록만 지운다. 하위 기록은 FK ON DELETE CASCADE로 함께 지워진다. */
    private void wipe(Long memberId) {
        Store store = storeMapper.findByMember(memberId);
        if (store != null) {
            Long storeId = store.getStoreId();
            campaignMapper.clearInsightIds(storeId);
            insightMapper.deleteByStore(storeId);
            campaignMapper.deleteByStore(storeId);
            salesMapper.deleteSalesByStore(storeId);
            salesMapper.deleteUploadsByStore(storeId);
            storeMapper.deleteStoreImages(storeId);
            storeMapper.deleteProducts(storeId);
            storeMapper.deleteStore(storeId);
        }
        notificationMapper.deleteByMember(memberId);
        memberMapper.deleteUsage(memberId);
        memberMapper.deleteSubscriptions(memberId);
    }

    private void fill(Long memberId) {
        // 시연 계정은 구독(PRO) 상태로 시작한다.
        memberMapper.insertSubscription(memberId, Plan.PRO.name());

        Store store = new Store();
        store.setStoreName("모먼트 카페");
        store.setBrandColor("#8b5e3c");
        store.setCategory("카페·디저트");
        store.setDescription("성수동 골목에서 제철 재료로 만든 음료와 디저트를 선보이는 작은 카페입니다.");
        store.setTargetCustomer("20대, 30대, 직장인");
        store.setToneManner("친근한, 감성적인");
        store.setHeroImageUrl(UNSPLASH + "photo-1554118811-1e0d58224f24?auto=format&fit=crop&w=1600&q=80");
        storeMapper.insertStore(memberId, store);

        // 시드 안에서 쓰는 상품 번호(1~4) → 실제 PRODUCT_ID
        Map<Long, Long> ids = new LinkedHashMap<>();
        List<Product> products = new ArrayList<>();
        Object[][] menu = { {1L, "아메리카노", 4500}, {2L, "크루아상", 4200}, {3L, "시그니처 케이크", 7800}, {4L, "브런치 세트", 15800} };
        for (Object[] m : menu) {
            Product p = new Product(null, (String) m[1], (Integer) m[2], null, null);
            storeMapper.insertProduct(store.getStoreId(), p);
            ids.put((Long) m[0], p.getProductId());
            products.add(p);
        }
        store.setProducts(products);

        List<Campaign> campaigns = campaigns(ids);
        Map<String, Integer> usage = new TreeMap<>();
        for (Campaign c : campaigns) {
            campaignRepository.insert(c, store.getStoreId());
            campaignRepository.insertPosts(c);
            usage.merge(c.getUsageMonth(), 1, Integer::sum);
        }
        usage.forEach((month, count) -> memberMapper.setUsage(memberId, month, count));

        List<Sale> sales = sales(ids);
        List<UploadRow> uploads = saveSales(store.getStoreId(), sales);

        Campaign weekend = campaigns.stream().filter(c -> c.getTitle().equals("추석 할인 이벤트")).findFirst().orElseThrow();
        notificationMapper.insert(memberId, weekend.getCampaignId(), NotificationService.TYPE_CAMPAIGN_RESULT,
                "주말 디저트 캠페인의 성과를 확인하고 다음 개선 방향을 살펴보세요.", LocalDate.now().plusDays(1).atTime(10, 0));

        // 지난 AI 개선 방향 분석: 그 날짜까지의 캠페인·매출로 분석해 기록으로 남긴다.
        for (LocalDateTime at : List.of(LocalDateTime.of(2026, 9, 19, 10, 0), LocalDateTime.of(2026, 9, 25, 10, 0),
                LocalDateTime.of(2026, 10, 13, 10, 0))) {
            Long base = uploads.stream().filter(u -> !u.getUploadedAt().isAfter(at)).map(UploadRow::getUploadId)
                    .reduce((a, b) -> b).orElse(null);
            InsightService.Analysis a = insightService.analyze(store, campaigns, sales, at.toLocalDate(), false);
            insightRepository.save(store.getStoreId(), base, at, a.result(), a.candidates());
        }
    }

    /** 매출을 주 단위·입력 방식별로 묶어 업로드 기록으로 저장한다 (그 주 마지막 날 21시 업로드). */
    private List<UploadRow> saveSales(Long storeId, List<Sale> sales) {
        LocalDate start = LocalDate.of(2026, 9, 1);
        Map<String, List<Sale>> groups = new TreeMap<>();
        for (Sale sale : sales) {
            long week = ChronoUnit.DAYS.between(start, sale.getSaleDate()) / 7;
            groups.computeIfAbsent(week + ":" + sale.getSource(), k -> new ArrayList<>()).add(sale);
        }
        List<UploadRow> uploads = new ArrayList<>();
        for (Map.Entry<String, List<Sale>> e : groups.entrySet()) {
            String[] key = e.getKey().split(":");
            List<LocalDate> dates = e.getValue().stream().map(Sale::getSaleDate).sorted().toList();
            UploadRow upload = UploadRow.builder().storeId(storeId).inputType(key[1].toUpperCase())
                    .fileName("excel".equals(key[1]) ? "매출_" + key[0] + "주차.xlsx" : null).recordCount(dates.size())
                    .dataFrom(dates.get(0)).dataTo(dates.get(dates.size() - 1))
                    .uploadedAt(start.plusDays(Long.parseLong(key[0]) * 7 + 6).atTime(21, 0)).build();
            salesMapper.insertUpload(upload);
            e.getValue().forEach(s -> salesMapper.insertSale(upload.getUploadId(), s));
            uploads.add(upload);
        }
        uploads.sort((a, b) -> a.getUploadId().compareTo(b.getUploadId()));
        return uploads;
    }

    private static List<Campaign> campaigns(Map<Long, Long> ids) {
        String latte = UNSPLASH + "photo-1570968915860-54d5c301fa9f?auto=format&fit=crop&w=1200&q=80";
        String scone = UNSPLASH + "photo-1555507036-ab1f4038808a?auto=format&fit=crop&w=1200&q=80";
        String cake = UNSPLASH + "photo-1565958011703-44f9829ba187?auto=format&fit=crop&w=1200&q=80";

        // 목업의 "네이버 플레이스"는 CHANNEL 목록에 없어 "블로그"로 바꿨다.
        Campaign c1 = published("10월 신메뉴 프로모션", LocalDateTime.of(2026, 10, 5, 9, 0), "봄 시즌 신메뉴 알리기",
                ids.get(1L), new AiPlan("부드러운 봄날의 한 잔", "감성적인 카페를 찾는 20~30대 직장인",
                        "시그니처 크림의 질감과 여유로운 오후를 연결해 신메뉴 호기심을 높입니다.", "신메뉴 인지도와 평일 오후 방문 증가"),
                List.of("인스타그램", "블로그"), true, "따뜻하고 화사한 봄", "음료 중심의 테이블 컷");
        c1.setDrafts(new ArrayList<>(List.of(
                new Draft("A", "인스타그램", "봄처럼 부드러운 오늘의 한 잔, 모먼트 카페 시그니처 크림 라떼입니다.",
                        new ArrayList<>(List.of("모먼트카페", "성수카페", "크림라떼")), latte),
                new Draft("B", "블로그", "고소한 에스프레소와 부드러운 수제 크림이 어우러진 봄 시즌 라떼입니다.",
                        new ArrayList<>(List.of("성수동카페", "신메뉴")), latte))));
        c1.setSelectedDraftKeys(new ArrayList<>(List.of("A", "B")));
        c1.setPosts(new ArrayList<>(List.of(new Post("인스타그램", "A", Post.SUCCESS).reactions(412, 38, 21, 97, 4100),
                new Post("블로그", "B", Post.SUCCESS).reactions(186, 22, 9, 41, 1830))));
        c1.getImage().getRatios().putAll(Map.of("인스타그램", "1:1", "블로그", "1:1"));

        Campaign c2 = published("9월 신메뉴 프로모션", LocalDateTime.of(2026, 9, 18, 14, 0), "평일 오후 방문 늘리기",
                ids.get(3L), new AiPlan("오후 세 시, 달콤한 쉼표", "성수동 인근 직장인과 프리랜서",
                        "커피와 스콘 세트의 합리적인 가격을 강조합니다.", "평일 14~17시 매출 증가"),
                List.of("인스타그램", "카카오톡 채널"), false, "포근하고 차분한 분위기", "커피와 스콘 세트");
        c2.setDrafts(new ArrayList<>(List.of(new Draft("A", "카카오톡 채널",
                "오후 2시부터 5시까지, 커피와 바닐라빈 스콘 세트로 잠깐의 여유를 즐겨보세요.", new ArrayList<>(), scone))));
        c2.setPosts(new ArrayList<>(List.of(new Post("카카오톡 채널", "A", Post.SUCCESS).reactions(154, 12, 33, 28, 1650))));
        c2.getImage().getRatios().put("카카오톡 채널", "4:5");

        Campaign c3 = published("추석 할인 이벤트", LocalDateTime.of(2026, 9, 12, 18, 30), "주말 디저트 판매 늘리기",
                ids.get(4L), new AiPlan("주말을 위한 작은 축하", "주말 데이트를 즐기는 20~30대 고객",
                        "딸기와 생크림의 신선함을 시각적으로 강조합니다.", "주말 디저트 주문과 동반 방문 증가"),
                List.of("인스타그램"), true, "생기 있고 사랑스러운 분위기", "케이크 클로즈업");
        c3.setDrafts(new ArrayList<>(List.of(new Draft("A", "인스타그램",
                "이번 주말 한정 10% 할인! 제철 딸기를 가득 올린 생크림 케이크로 달콤하게 쉬어가세요. 매장에 방문해 만나보세요.",
                new ArrayList<>(List.of("딸기케이크", "성수데이트", "모먼트카페")), cake))));
        c3.setPosts(new ArrayList<>(List.of(new Post("인스타그램", "A", Post.SUCCESS).reactions(298, 31, 18, 76, 2870))));
        c3.getImage().getRatios().put("인스타그램", "4:5");

        Campaign c5 = published("크루아상 아침 세트 안내", LocalDateTime.of(2026, 9, 26, 8, 0), "아침 방문 늘리기",
                ids.get(2L), new AiPlan("하루를 여는 고소한 아침", "출근길 직장인과 아침 산책 고객",
                        "갓 구운 크루아상과 커피 세트로 아침 루틴을 제안합니다.", "오전 8~10시 방문 증가"),
                List.of("블로그"), false, "따뜻한 아침 햇살", "크루아상과 커피 세트");
        c5.setDrafts(new ArrayList<>(List.of(new Draft("A", "블로그",
                "갓 구운 크루아상과 아메리카노로 하루를 시작해 보세요. 오전 8시부터 매장에서 만나보세요.", new ArrayList<>(), scone))));
        c5.setPosts(new ArrayList<>(List.of(new Post("블로그", "A", Post.SUCCESS).reactions(121, 9, 6, 33, 1290))));
        c5.getImage().getRatios().put("블로그", "1:1");

        // 작성 중인 캠페인 (2단계 AI 기획부터 이어서 작성)
        Campaign c4 = new Campaign();
        c4.setTitle("가을 시즌 메뉴 소개");
        c4.setCreatedAt(LocalDateTime.of(2026, 10, 3, 9, 0));
        c4.setUpdatedAt(c4.getCreatedAt());
        c4.setUsageMonth("202610");
        c4.setGoal("가을 시즌 메뉴 알리기");
        c4.setProductIds(new ArrayList<>(List.of(ids.get(1L))));
        c4.getContent().setChannels(new ArrayList<>(List.of("인스타그램")));
        c4.getContent().setHashtagOn(true);
        c4.getContent().setImageOn(true);

        return List.of(c1, c5, c2, c3, c4);
    }

    private static Campaign published(String title, LocalDateTime at, String goal, Long productId, AiPlan plan,
            List<String> channels, boolean hashtagOn, String mood, String composition) {
        Campaign c = new Campaign();
        c.setTitle(title);
        c.setCreatedAt(at);
        c.setUpdatedAt(at);
        c.setPublishedAt(at);
        c.setUsageMonth(at.format(YYYYMM));
        c.setStatus(CampaignStatus.PUBLISHED);
        c.setLockedStep("5");
        c.setCurrentStep("5");
        c.setGoal(goal);
        c.setProductIds(new ArrayList<>(List.of(productId)));
        c.setPlan(plan);
        c.getContent().setChannels(new ArrayList<>(channels));
        c.getContent().setHashtagOn(hashtagOn);
        c.getContent().setImageOn(true);
        c.getImage().setPurpose("신메뉴 홍보");
        c.getImage().setMood(mood);
        c.getImage().setComposition(composition);
        c.setSelectedDraftKeys(new ArrayList<>(List.of("A")));
        return c;
    }

    private record Lift(long productId, int from, int days, double factor) {
    }

    /**
     * 2026-09-01 ~ 2026-10-12 (42일) 메뉴별 일 매출. 게시된 캠페인과 맞춰
     * 추석 할인 이벤트(9/12, 브런치 세트)와 9월 신메뉴 프로모션(9/18, 시그니처 케이크)은 뚜렷하게 오르고,
     * 크루아상 아침 세트 안내(9/26, 크루아상)는 조금 오르고, 10월 신메뉴 프로모션(10/5, 아메리카노)은 효과가 거의 없게 만든다.
     */
    private static List<Sale> sales(Map<Long, Long> ids) {
        LocalDate start = LocalDate.of(2026, 9, 1);
        Map<Long, Integer> baseQty = Map.of(1L, 14, 2L, 6, 3L, 4, 4L, 2);
        Map<Long, Integer> price = Map.of(1L, 4500, 2L, 4200, 3L, 7800, 4L, 15800);
        List<Lift> lifts = List.of(new Lift(4, 11, 8, 2.1), new Lift(3, 17, 10, 1.8), new Lift(2, 25, 7, 1.15),
                new Lift(1, 34, 8, 1.02));
        String[] sources = {"excel", "receipt", "manual"};

        List<Sale> sales = new ArrayList<>();
        for (int day = 0; day < 42; day++) {
            LocalDate date = start.plusDays(day);
            boolean weekend = date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
            for (int m = 0; m < 4; m++) {
                long seedId = m + 1;
                int noise = ((day * 7 + m * 13) % 5) - 2;
                int d = day;
                double factor = lifts.stream()
                        .filter(l -> l.productId() == seedId && d >= l.from() && d < l.from() + l.days())
                        .mapToDouble(Lift::factor).findFirst().orElse(1);
                int qty = (int) Math.max(1, Math.round((baseQty.get(seedId) * (weekend ? 1.3 : 1) + noise * 0.6) * factor));
                sales.add(new Sale(null, date, ids.get(seedId), qty, (long) price.get(seedId) * qty,
                        sources[(day + m) % sources.length]));
            }
        }
        return sales;
    }
}
