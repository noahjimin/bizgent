package com.bizgent.service.ai;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;

import com.bizgent.domain.AiPlan;
import com.bizgent.domain.Campaign;
import com.bizgent.domain.Draft;
import com.bizgent.domain.Post;
import com.bizgent.domain.Product;
import com.bizgent.domain.Sale;
import com.bizgent.domain.Store;
import com.bizgent.service.SalesAnalysisService.CampaignRow;

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
    public Insights generateInsights(Store store, List<CampaignRow> candidates, int moreNeeded, List<Sale> sales) {
        LocalDate from = sales.stream().map(Sale::getSaleDate).min(Comparator.naturalOrder()).orElse(null);
        LocalDate to = sales.stream().map(Sale::getSaleDate).max(Comparator.naturalOrder()).orElse(null);
        List<Suggestion> slowMenus = slowMenus(store, sales);
        List<Suggestion> stockPromotions = stockPromotions(store, sales, from, to);

        if (moreNeeded > 0) {
            String more = "캠페인을 " + moreNeeded + "개 더 진행하면 잘된 캠페인과 아쉬운 캠페인을 비교해 드려요.";
            if (candidates.isEmpty()) {
                return new Insights("early", "아직 분석할 캠페인이 없어요",
                        "캠페인을 게시하고 게시 후 7일 매출이 쌓이면 분석해 드려요. " + more, List.of(), from, to,
                        List.of(), List.of(), slowMenus, stockPromotions, null, null, List.of(), List.of(), null);
            }
            CampaignRow t = candidates.get(0);
            return new Insights("early", t.campaign().getTitle() + " 캠페인을 분석했어요",
                    "게시 전후 7일 매출과 게시물 반응을 살펴봤어요. " + more, List.of(brief(t, "대상")), from, to,
                    earlyStrengths(t), earlyImprovements(t), slowMenus, stockPromotions, null, null, List.of(),
                    List.of(), null);
        }

        Comparator<CampaignRow> byChange = Comparator.comparingInt(r -> r.change() == null ? Integer.MIN_VALUE : r.change());
        CampaignRow g = candidates.stream().max(byChange).orElseThrow();
        CampaignRow b = candidates.stream().filter(r -> r != g).min(byChange).orElseThrow();
        Copy gc = Copy.of(g.campaign());
        Copy bc = Copy.of(b.campaign());

        // Good이 앞서고 Bad가 부족한 항목: {Good이 잘한 점, Bad에 부족한 점, 잘된 점 제목, 개선 제목, 개선 근거}
        List<String[]> wins = new ArrayList<>();
        if (gc.benefitFirst() && !bc.benefitFirst()) {
            wins.add(new String[] {"첫 문장의 혜택 표시", "첫 문장 혜택 표시", "첫 문장에 혜택을 먼저 보여줬어요",
                    "첫 문장에 혜택을 적어보세요", "Bad 첫 문장 혜택 없음"});
        }
        if (gc.cta() && !bc.cta()) {
            wins.add(new String[] {"행동 유도 문구", "행동 유도 문구", "행동 유도 문구로 방문을 이끌었어요",
                    "행동 유도 문구를 추가해 보세요", "Bad 행동 유도 문구 없음"});
        }
        if (!gc.ratio().equals(bc.ratio())) {
            wins.add(new String[] {gc.ratio() + " 이미지", "채널에 맞는 이미지 비율", "채널에 맞는 " + gc.ratio() + " 이미지를 썼어요",
                    gc.ratio() + " 이미지를 써보세요", "Good " + gc.ratio() + " · Bad " + bc.ratio()});
        }
        if (!gc.slot().equals(bc.slot())) {
            wins.add(new String[] {gc.slot() + " 게시", "고객이 많이 보는 시간대 게시", gc.slot() + "에 게시해 반응이 좋았어요",
                    gc.slot() + " 게시를 시도해 보세요", "Good " + gc.slot() + " · Bad " + bc.slot()});
        }
        if (wins.isEmpty()) {
            wins.add(new String[] {"분명한 대상 메뉴", "대상 메뉴 강조", "대상 메뉴를 분명하게 보여줬어요",
                    "대상 메뉴를 첫 장에 크게 보여주세요", "Bad 매출 " + signed(b.change())});
        }
        List<String[]> top = wins.subList(0, Math.min(2, wins.size()));
        Long goodProduct = g.campaign().getProductIds().isEmpty() ? null : g.campaign().getProductIds().get(0);
        String goal = gc.benefitFirst() ? "할인/이벤트 홍보" : "재방문 유도";

        String goodPoints = String.join(", ", top.stream().map(w -> w[0]).toList());
        Conclusion conclusion = new Conclusion("Good 캠페인은 " + goodPoints + " 덕분에 잘됐고, Bad 캠페인은 "
                + String.join(", ", top.stream().map(w -> w[1]).toList()) + " 부분이 부족했어요. 다음 캠페인에는 "
                + goodPoints + " 방식을 적용해 보세요.", goal, goodProduct);

        List<Suggestion> strengths = new ArrayList<>();
        List<Suggestion> improvements = new ArrayList<>();
        for (int i = 0; i < top.size(); i++) {
            String[] w = top.get(i);
            strengths.add(new Suggestion("g-" + (i + 1), w[2], g.campaign().getTitle() + "에서 효과를 본 방식이에요. 다음 "
                    + g.menuNames() + " 캠페인에도 적용해 보세요.", "Good 매출 " + signed(g.change()), false, goal, goodProduct));
            improvements.add(new Suggestion("b-" + (i + 1), w[3], b.campaign().getTitle() + "에는 이 부분이 없었어요. "
                    + "다음 캠페인에서 바꿔 보세요.", w[4], false, goal, null));
        }

        Reactions gr = Reactions.of(g.campaign());
        Reactions br = Reactions.of(b.campaign());
        List<MetricBar> metrics = List.of(
                bar("대상 메뉴 매출 증가율", signed(g.change()), signed(b.change()), Math.max(0, nz(g.change())),
                        Math.max(0, nz(b.change())), false),
                bar("게시 후 7일 매출", won(g.after()), won(b.after()), g.after(), b.after(), false),
                bar("좋아요", number(gr.likes()), number(br.likes()), gr.likes(), br.likes(), true),
                bar("저장", number(gr.saves()), number(br.saves()), gr.saves(), br.saves(), true),
                bar("도달", number(gr.reach()), number(br.reach()), gr.reach(), br.reach(), true));
        List<Difference> differences = List.of(
                new Difference("첫 문장 혜택 표시", gc.benefitLabel(), bc.benefitLabel()),
                new Difference("이미지 비율", gc.ratio(), bc.ratio()),
                new Difference("행동 유도 문구", gc.cta() ? "있음" : "없음", bc.cta() ? "있음" : "없음"),
                new Difference("게시 채널", g.channels(), b.channels()),
                new Difference("게시 시간대", gc.slot() + " (" + gc.hour() + "시)", bc.slot() + " (" + bc.hour() + "시)"));

        return new Insights("enough", store.getStoreName() + "의 잘된 캠페인과 아쉬운 캠페인을 비교했어요",
                "최근 게시한 캠페인 " + candidates.size() + "개 중 대상 메뉴 매출 증가율이 가장 높은 캠페인과 가장 낮은 캠페인을 비교했어요.",
                List.of(brief(g, "Good"), brief(b, "Bad")), from, to, strengths, improvements, slowMenus,
                stockPromotions, comparison(g, gr, "게시 후 7일 대상 메뉴 매출이 게시 전보다 " + signed(g.change()) + " 변했어요."),
                comparison(b, br, "게시 후 7일 대상 메뉴 매출 변화가 " + signed(b.change()) + "에 그쳤어요."), metrics,
                differences, conclusion);
    }

    private List<Suggestion> earlyStrengths(CampaignRow t) {
        Long product = t.campaign().getProductIds().isEmpty() ? null : t.campaign().getProductIds().get(0);
        List<Suggestion> out = new ArrayList<>();
        out.add(t.change() != null && t.change() > 0
                ? new Suggestion("s-1", "게시 후 " + t.menuNames() + " 매출이 올랐어요",
                        "게시 전 7일보다 게시 후 7일에 더 많이 팔렸어요. 같은 메뉴로 한 번 더 알려보세요.",
                        "대상 메뉴 매출 " + signed(t.change()), false, "신메뉴/신상품 홍보", product)
                : new Suggestion("s-1", "게시 후에도 " + t.menuNames() + " 매출을 지켰어요",
                        "큰 변화는 없었지만 판매가 꾸준했어요. 혜택을 더해 다시 알려보세요.",
                        "게시 후 7일 매출 " + won(t.after()), false, "할인/이벤트 홍보", product));

        // 게시 후 7일을 평일·주말로 나눠 일평균 비교
        List<LocalDate> dates = t.chart().afterDates().stream().map(LocalDate::parse).toList();
        double weekend = avg(t.chart().afterDaily(), dates, true);
        double weekday = avg(t.chart().afterDaily(), dates, false);
        if (weekend > 0 && weekday > 0) {
            boolean weekendUp = weekend >= weekday;
            int diff = (int) Math.round(((weekendUp ? weekend : weekday) / (weekendUp ? weekday : weekend) - 1) * 100);
            out.add(new Suggestion("s-2", weekendUp ? "주말에 반응이 더 좋았어요" : "평일에도 꾸준히 팔렸어요",
                    weekendUp ? "주말 매출이 평일보다 높았어요. 금요일 저녁에 주말 혜택을 미리 알려보세요."
                            : "평일 매출이 주말보다 높았어요. 출근·점심 시간대 고객을 겨냥해 보세요.",
                    (weekendUp ? "주말" : "평일") + " 일평균 매출 +" + diff + "%", false, "재방문 유도", product));
        }
        int saves = Reactions.of(t.campaign()).saves();
        if (saves > 0) {
            out.add(new Suggestion("s-3", "저장이 많아 다시 찾을 고객이 늘었어요",
                    "게시물을 저장한 고객은 다시 방문할 가능성이 높아요. 저장한 고객을 위한 재방문 쿠폰을 안내해 보세요.",
                    "저장 " + number(saves) + "회", true, "재방문 유도", product));
        }
        return out;
    }

    private List<Suggestion> earlyImprovements(CampaignRow t) {
        Long product = t.campaign().getProductIds().isEmpty() ? null : t.campaign().getProductIds().get(0);
        List<Long> after = t.chart().afterDaily();
        double first = after.subList(0, 3).stream().mapToLong(Long::longValue).average().orElse(0);
        double late = after.subList(3, after.size()).stream().mapToLong(Long::longValue).average().orElse(0);
        int lateChange = first == 0 ? 0 : (int) Math.round((late / first - 1) * 100);
        List<Suggestion> out = new ArrayList<>();
        out.add(lateChange < 0
                ? new Suggestion("i-1", "3일차 이후 매출이 줄었어요",
                        "한 번 게시한 뒤 추가 게시가 없어 반응이 금방 줄었어요. 주 2회 정도 꾸준히 올려보세요.",
                        "4~7일차 일평균 " + lateChange + "%", false, "매장 인지도 확대", product)
                : new Suggestion("i-1", "추가 게시로 상승세를 이어가 보세요",
                        "후반에도 매출이 유지됐어요. 이때 한 번 더 게시하면 효과를 더 키울 수 있어요.",
                        "4~7일차 일평균 +" + lateChange + "%", false, "매장 인지도 확대", product));
        Copy c = Copy.of(t.campaign());
        if (!c.benefitFirst()) {
            out.add(new Suggestion("i-2", "첫 문장에 혜택이 없었어요",
                    "할인이나 한정 혜택이 본문 뒤쪽에 있으면 놓치기 쉬워요. 첫 문장에 가격이나 혜택을 먼저 적어보세요.",
                    "첫 문장 혜택 표시 없음", false, "할인/이벤트 홍보", product));
        } else if (!c.cta()) {
            out.add(new Suggestion("i-2", "행동을 이끄는 문구가 없었어요",
                    "'지금 방문하기', '예약하기' 같은 문구로 다음 행동을 안내해 보세요.",
                    "행동 유도 문구 없음", false, "재방문 유도", product));
        } else {
            out.add(new Suggestion("i-2", "참여형 문구로 댓글을 늘려보세요",
                    "질문이나 투표를 넣으면 댓글과 공유가 늘어 더 많은 고객에게 보여요.",
                    "댓글 " + number(Reactions.of(t.campaign()).comments()) + "개", true, "매장 인지도 확대", product));
        }
        return out;
    }

    private static List<Suggestion> slowMenus(Store store, List<Sale> sales) {
        List<Product> byQty = store.getProducts().stream()
                .sorted(Comparator.comparingLong((Product p) -> qty(sales, p))).toList();
        if (byQty.size() < 2) {
            return List.of();
        }
        Product slow = byQty.get(0);
        Product top = byQty.get(byQty.size() - 1);
        return List.of(new Suggestion("slow-1", slow.getProductName() + ", 함께 먹기 좋은 조합으로 소개해 보세요",
                top.getProductName() + "와 세트로 묶거나 시식 이벤트로 첫 구매 장벽을 낮춰보세요.",
                "판매 " + number(qty(sales, slow)) + "개 · 메뉴 중 가장 적음", false, "비인기 메뉴 살리기",
                slow.getProductId()));
    }

    private static List<Suggestion> stockPromotions(Store store, List<Sale> sales, LocalDate from, LocalDate to) {
        Product stock = store.getProducts().stream()
                .filter(p -> p.getStockQty() != null && p.getStockQty() > 0)
                .max(Comparator.comparingInt(Product::getStockQty)).orElse(null);
        if (stock == null) {
            return List.of();
        }
        long days = from == null ? 1 : ChronoUnit.DAYS.between(from, to) + 1;
        long daily = Math.round((double) qty(sales, stock) / days);
        return List.of(new Suggestion("stock-1", stock.getProductName() + " 재고 소진 타임세일을 추천해요",
                "오후 4시 이후 한정 할인이나 1+1 쿠폰으로 남은 재고를 빠르게 소진해 보세요.",
                "재고 " + stock.getStockQty() + "개 · 하루 평균 " + daily + "개 판매", false, "재고 소진 프로모션",
                stock.getProductId()));
    }

    private static CampaignBrief brief(CampaignRow r, String role) {
        return new CampaignBrief(r.campaign().getCampaignId(), r.campaign().getTitle(), r.campaign().getPublishedAt(),
                r.channels(), r.campaign().getThumbnailUrl(), role);
    }

    private static CampaignComparison comparison(CampaignRow r, Reactions x, String reason) {
        return new CampaignComparison(r.campaign().getCampaignId(), r.campaign().getTitle(), r.channels(),
                r.menuNames(), r.change(), r.before(), r.after(), x.likes(), x.comments(), x.shares(), x.saves(),
                x.reach(), reason);
    }

    private static MetricBar bar(String label, String goodText, String badText, long good, long bad, boolean sample) {
        long max = Math.max(good, bad);
        return new MetricBar(label, goodText, badText, max == 0 ? 0 : (int) Math.round(good * 100.0 / max),
                max == 0 ? 0 : (int) Math.round(bad * 100.0 / max), sample);
    }

    /** 게시물 반응 합계 (예시 데이터) */
    private record Reactions(int likes, int comments, int shares, int saves, int reach) {

        static Reactions of(Campaign c) {
            return new Reactions(c.getPosts().stream().mapToInt(Post::getLikes).sum(),
                    c.getPosts().stream().mapToInt(Post::getComments).sum(),
                    c.getPosts().stream().mapToInt(Post::getShares).sum(),
                    c.getPosts().stream().mapToInt(Post::getSaves).sum(),
                    c.getPosts().stream().mapToInt(Post::getReach).sum());
        }
    }

    /** 게시한 시안 문구·이미지·시각을 규칙으로 판정한 결과 (실제 AI는 문구를 읽고 판단) */
    private record Copy(String firstSentence, boolean benefitFirst, boolean cta, String ratio, String slot, int hour) {

        private static final Pattern BENEFIT = Pattern.compile("할인|%|원|한정|무료|1\\+1|증정|쿠폰");
        private static final Pattern CTA = Pattern.compile("방문|예약|주문|만나보세요|들러|지금");

        static Copy of(Campaign c) {
            String text = c.getDrafts().stream().filter(d -> c.getSelectedDraftKeys().contains(d.getKey()))
                    .map(Draft::getText).filter(Objects::nonNull).findFirst()
                    .orElse(c.getDrafts().isEmpty() ? "" : Objects.requireNonNullElse(c.getDrafts().get(0).getText(), ""));
            String first = text.split("(?<=[.!?])\\s+", 2)[0].trim();
            List<String> ratios = c.getImage().getRatios().values().stream().distinct().toList();
            String ratio = !c.getContent().isImageOn() ? "이미지 없음" : ratios.isEmpty() ? "기본" : String.join(", ", ratios);
            int hour = c.getPublishedAt() == null ? 0 : c.getPublishedAt().getHour();
            return new Copy(first, BENEFIT.matcher(first).find(), CTA.matcher(text).find(), ratio,
                    hour < 11 ? "오전" : hour < 17 ? "오후" : "저녁", hour);
        }

        String benefitLabel() {
            if (!benefitFirst) {
                return "없음";
            }
            return "있음 · “" + (firstSentence.length() > 18 ? firstSentence.substring(0, 18) + "…" : firstSentence) + "”";
        }
    }

    private static double avg(List<Long> amounts, List<LocalDate> dates, boolean weekend) {
        return IntStream.range(0, dates.size())
                .filter(i -> (dates.get(i).getDayOfWeek().getValue() >= 6) == weekend)
                .mapToLong(amounts::get).average().orElse(0);
    }

    private static long qty(List<Sale> sales, Product p) {
        return sales.stream().filter(s -> s.getProductId().equals(p.getProductId())).mapToLong(Sale::getQuantity).sum();
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    private static String signed(Integer v) {
        return v == null ? "-" : (v >= 0 ? "+" : "") + v + "%";
    }

    private static String number(long v) {
        return String.format("%,d", v);
    }

    private static String won(long v) {
        return number(v) + "원";
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
