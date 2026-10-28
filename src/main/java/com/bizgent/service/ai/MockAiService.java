package com.bizgent.service.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;

import com.bizgent.domain.AiPlan;
import com.bizgent.domain.Campaign;
import com.bizgent.domain.Draft;
import com.bizgent.domain.Post;
import com.bizgent.domain.Product;
import com.bizgent.domain.Sale;
import com.bizgent.domain.Store;

/** 실제 AI API 대신 정해진 규칙으로 응답하는 모의 구현 (프로토타입 services/ai.ts 이식) */
@Service
public class MockAiService implements AiService {

    public static final List<String> DRAFT_IMAGES = List.of("/assets/2d925.png", "/assets/d26d2.png");

    private static final Map<String, List<String>> STORE_IMAGE_POOLS = Map.of(
            "매장 외관", List.of("1616141037618-d671075fd1a1", "1785508145318-a42f8fe67eb7", "1596517447156-4408f27791ae", "1735917567827-8fabd79877a9", "1596104514460-3f3aa35a5776"),
            "매장 내부", List.of("1521017432531-fbd92d768814", "1511081692775-05d0f180a065", "1648462908676-8305f0eff8e0", "1579341560277-4dfaddaf6e98", "1613274554329-70f997f5789f", "1583354608715-177553a4035e"),
            "대표 메뉴:cafe", List.of("1529942458412-eda69f76291d", "1611916656173-875e4277bea6", "1562931976-76211df16534", "1769775529747-107551aec610", "1562931964-fb8897dbb0f8"),
            "대표 메뉴:food", List.of("1663530761401-15eefb544889", "1564759296729-771e78c26df7", "1625862692743-3f57873bb37c", "1579631542761-d7c4e913f8d4", "1625861910621-e9385ba1d993"),
            "메뉴와 공간 함께", List.of("1769775529747-107551aec610", "1759912256095-72174d9e88d7", "1565895405140-6b9830a88c19", "1529942458412-eda69f76291d", "1521017432531-fbd92d768814"));

    private final AtomicInteger storeImageRound = new AtomicInteger();

    @Override
    public AiPlan generatePlan(Store store, Campaign campaign) {
        String menu = menuNames(store, campaign);
        String audience = !campaign.getTargetCustomers().isEmpty() ? String.join(", ", campaign.getTargetCustomers())
                : store.getCategory() + "에 관심 있는 지역 고객";
        AiPlan plan = new AiPlan(
                menu + "의 매력이 바로 보이는 " + campaign.getGoal() + " 캠페인",
                audience,
                store.getStoreName() + "만의 분위기와 " + menu + "의 특징을 연결해, 고객이 방문할 구체적인 이유를 전달합니다.",
                menu + " 인지도 향상과 매장 방문 전환 증가");
        plan.setAgeGroup(campaign.getTargetCustomers().isEmpty() ? "20-30대" : audience);
        plan.setChannels(new ArrayList<>(List.of("인스타그램", "블로그")));
        plan.setTones(campaign.getToneManners().isEmpty() ? new ArrayList<>(List.of("감성적인", "트렌디한"))
                : new ArrayList<>(campaign.getToneManners()));
        plan.setReach("2,000~3,000명");
        plan.setFrequency("주 2-3회");
        plan.setDuration("2주");
        return plan;
    }

    @Override
    public AiPlan reanalyzePlan(Store store, Campaign campaign, String feedback) {
        String menu = menuNames(store, campaign);
        AiPlan current = campaign.getPlan();
        String focus = feedback == null || feedback.isBlank() ? "방문 동기를 더 선명하게 만드는 방향" : feedback.trim();
        AiPlan plan = new AiPlan(
                campaign.getGoal() + "에 집중한 " + menu + " 제안",
                !campaign.getTargetCustomers().isEmpty() ? String.join(", ", campaign.getTargetCustomers())
                        : current.getTargetCustomer(),
                focus + "으로 재구성했습니다. " + store.getStoreName() + "의 강점을 첫 문장에 배치하고 혜택과 방문 시점을 분명하게 안내합니다.",
                "콘텐츠 반응률 개선과 " + menu + " 구매 전환 증가");
        plan.setAgeGroup(current.getAgeGroup());
        plan.setChannels(current.getChannels());
        plan.setTones(current.getTones());
        plan.setReach("2,500~3,500명");
        plan.setFrequency(current.getFrequency());
        plan.setDuration(current.getDuration());
        plan.setUserRequest(feedback);
        return plan;
    }

    @Override
    public List<Draft> generateDrafts(Store store, Campaign campaign) {
        String menu = menuNames(store, campaign);
        List<String> channels = campaign.getContent().getChannels();
        String[] openings = {"오늘, 놓치기 아까운", "한 번 맛보면 다시 생각나는"};
        String[] keys = {"A", "B"};
        List<Draft> drafts = new ArrayList<>();
        for (int i = 0; i < keys.length; i++) {
            String channel = channels.isEmpty() ? "인스타그램" : channels.get(i % channels.size());
            List<String> hashtags = new ArrayList<>();
            if (campaign.getContent().isHashtagOn()) {
                // 매장명, 대상 메뉴(메뉴마다 하나씩), 목표
                hashtags.add(noSpace(store.getStoreName()));
                campaign.getProductIds().stream().map(store::productName).filter(n -> !n.isEmpty())
                        .map(MockAiService::noSpace).forEach(hashtags::add);
                hashtags.add(noSpace(campaign.getGoal()).replace("/", ""));
            }
            String image = campaign.getContent().isImageOn() ? DRAFT_IMAGES.get(i % DRAFT_IMAGES.size()) : "";
            drafts.add(new Draft(keys[i], channel,
                    openings[i] + " " + menu + ". " + store.getStoreName() + "에서 " + campaign.getGoal()
                            + "을 위한 특별한 순간을 만나보세요.",
                    hashtags, image));
        }
        return drafts;
    }

    @Override
    public List<String> suggestCopyAlternatives(Store store, Draft draft) {
        String tone = store.getToneManner() == null || store.getToneManner().isBlank() ? "친근한" : store.getToneManner();
        String name = store.getStoreName();
        return List.of(
                name + "이 준비한 오늘의 특별함을 만나보세요. " + draft.getText(),
                tone + " 분위기로 전해드려요. 지금 " + name + "에서 직접 경험해 보세요.",
                draft.getChannel() + "에서 먼저 만나는 소식, " + draft.getText());
    }

    @Override
    public EditedImage editImage(String imageUrl, String instruction, String mood) {
        String next = DRAFT_IMAGES.stream().filter(url -> !url.equals(imageUrl)).findFirst().orElse(imageUrl);
        String moodText = mood == null || mood.isBlank() ? "브랜드에 어울리는 분위기" : mood;
        return new EditedImage(next, moodText + "를 살려 “" + instruction + "” 요청을 반영한 이미지입니다.");
    }

    @Override
    public List<GeneratedImage> generateStoreImages(Store store, StoreImageRequest request) {
        boolean cafe = store.getCategory() != null && store.getCategory().matches(".*(카페|커피|디저트|베이커리).*");
        String key = "대표 메뉴".equals(request.scene()) ? "대표 메뉴:" + (cafe ? "cafe" : "food") : request.scene();
        List<String> pool = STORE_IMAGE_POOLS.getOrDefault(key, STORE_IMAGE_POOLS.get("매장 내부"));
        int offset = (storeImageRound.getAndIncrement() * 3) % pool.size();
        String subject = request.productId() != null ? store.productName(request.productId()) : request.scene();
        long now = System.currentTimeMillis();
        return IntStream.range(0, 3)
                .mapToObj(i -> new GeneratedImage(now + "-" + i,
                        "https://images.unsplash.com/photo-" + pool.get((offset + i) % pool.size())
                                + "?auto=format&fit=crop&w=1200&q=80",
                        store.getStoreName() + " " + request.mood() + " " + subject + " 이미지 " + (i + 1)))
                .toList();
    }

    @Override
    public Insights generateInsights(Store store, String view, List<Campaign> campaigns, List<Sale> sales) {
        List<Product> byQty = store.getProducts().stream()
                .sorted(Comparator.comparingInt((Product p) -> sales.stream()
                        .filter(s -> s.getProductId().equals(p.getProductId())).mapToInt(Sale::getQuantity).sum())
                        .reversed())
                .toList();
        Product top = byQty.isEmpty() ? null : byQty.get(0);
        Product slow = byQty.isEmpty() ? null : byQty.get(byQty.size() - 1);
        Product stock = store.getProducts().stream()
                .filter(p -> p.getStockQty() != null && p.getStockQty() > 0)
                .max(Comparator.comparingInt(Product::getStockQty)).orElse(null);
        String topName = top == null ? null : top.getProductName();
        Long topId = top == null ? null : top.getProductId();

        List<Suggestion> slowMenus = slow == null ? List.of() : List.of(new Suggestion("slow-1",
                slow.getProductName() + ", 함께 먹기 좋은 조합으로 소개해 보세요",
                "최근 판매가 가장 적은 메뉴예요. " + Objects.requireNonNullElse(topName, "인기 메뉴")
                        + "와 세트로 묶거나 시식 이벤트로 첫 구매 장벽을 낮춰보세요.",
                "비인기 메뉴 살리기", slow.getProductId()));
        List<Suggestion> stockPromotions = stock == null ? List.of() : List.of(new Suggestion("stock-1",
                stock.getProductName() + " 재고 " + stock.getStockQty() + "개, 마감 전 타임세일 추천",
                "재고가 많이 남아 있어요. 오후 4시 이후 한정 할인이나 1+1 쿠폰으로 빠르게 소진해 보세요.",
                "재고 소진 프로모션", stock.getProductId()));

        if (!"enough".equals(view)) {
            return new Insights("early",
                    store.getStoreName() + ", 첫 캠페인 결과를 살펴봤어요",
                    "아직 캠페인과 매출 기록이 많지 않아 간단한 분석만 보여드려요. 캠페인을 2~3개 더 진행하면 잘된 캠페인과 아쉬운 캠페인을 비교해드려요.",
                    List.of(
                            new Suggestion("s-1", "사진이 있는 게시물의 반응이 좋았어요",
                                    Objects.requireNonNullElse(topName, "대표 메뉴")
                                            + " 사진을 크게 보여준 게시물에서 저장과 문의가 많았어요. 다음 캠페인에도 메뉴 사진을 첫 장에 배치해 보세요.",
                                    "신메뉴/신상품 홍보", topId),
                            new Suggestion("s-2", "평일 오후 방문이 늘었어요",
                                    "게시 직후 평일 오후 매출이 조금 올랐어요. 같은 시간대에 재방문 쿠폰을 함께 안내하면 효과가 더 커질 수 있어요.",
                                    "재방문 유도", topId)),
                    List.of(
                            new Suggestion("i-1", "게시 횟수가 적어 노출이 금방 줄었어요",
                                    "한 번 게시한 뒤 추가 게시가 없어 3일 차부터 조회수가 줄었어요. 주 2회 정도 꾸준히 올려보세요.",
                                    "매장 인지도 확대", null),
                            new Suggestion("i-2", "혜택 문구가 눈에 잘 띄지 않았어요",
                                    "할인 정보가 본문 끝에 있어 놓치기 쉬웠어요. 첫 문장에 가격이나 혜택을 먼저 적어보세요.",
                                    "할인/이벤트 홍보", topId)),
                    slowMenus, stockPromotions, null, null);
        }

        List<CampaignComparison> compared = campaigns.stream()
                .filter(Campaign::isPublished)
                .flatMap(c -> c.getPosts().stream().map(p -> comparison(c, p)))
                .sorted(Comparator.comparingDouble(CampaignComparison::conversionRate).reversed())
                .toList();
        CampaignComparison good = compared.isEmpty() ? null : withReason(compared.get(0),
                "메뉴 사진과 기간 한정 혜택을 첫 문장에 배치해 클릭 후 방문 전환이 높았어요.");
        CampaignComparison bad = compared.size() < 2 ? null : withReason(compared.get(compared.size() - 1),
                "정보는 충분했지만 행동을 유도하는 문구가 없어 클릭 대비 방문이 적었어요.");

        return new Insights("enough",
                store.getStoreName() + "의 잘된 캠페인과 아쉬운 캠페인을 비교했어요",
                "게시한 캠페인의 조회·클릭·방문 전환을 비교해 다음 캠페인에 적용할 점을 정리했어요.",
                List.of(new Suggestion("g-1", "기간 한정 문구가 방문을 이끌었어요",
                        "잘된 캠페인은 '이번 주만' 같은 기간 한정 표현을 사용했어요. 다음 "
                                + Objects.requireNonNullElse(topName, "메뉴") + " 캠페인에도 적용해 보세요.",
                        "할인/이벤트 홍보", topId)),
                List.of(
                        new Suggestion("b-1", "행동 유도 문구를 추가해 보세요",
                                "아쉬운 캠페인은 마지막에 '지금 방문하기' 같은 안내가 없었어요. 쿠폰이나 예약 링크를 함께 넣어보세요.",
                                "재방문 유도", null),
                        new Suggestion("b-2", "채널에 맞는 이미지 비율을 사용하세요",
                                "가로 이미지는 SNS 피드에서 작게 보여 조회 대비 클릭이 낮았어요. 4:5 세로 이미지를 추천해요.",
                                "매장 인지도 확대", null)),
                slowMenus, stockPromotions, good, bad);
    }

    private static CampaignComparison comparison(Campaign c, Post p) {
        double rate = p.getClicks() == 0 ? 0 : Math.round(p.getConversions() * 1000.0 / p.getClicks()) / 10.0;
        return new CampaignComparison(c.getCampaignId(), c.getTitle(), p.getChannel(), p.getViews(), p.getClicks(),
                p.getConversions(), rate, null);
    }

    private static CampaignComparison withReason(CampaignComparison c, String reason) {
        return new CampaignComparison(c.campaignId(), c.title(), c.channel(), c.views(), c.clicks(), c.conversions(),
                c.conversionRate(), reason);
    }

    private static String menuNames(Store store, Campaign campaign) {
        String names = String.join(", ", campaign.getProductIds().stream().map(store::productName)
                .filter(n -> !n.isEmpty()).toList());
        return names.isEmpty() ? "대표 메뉴" : names;
    }

    private static String noSpace(String value) {
        return value == null ? "" : value.replace(" ", "");
    }
}
