package com.bizgent.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.bizgent.demo.DemoSession;
import com.bizgent.domain.Campaign;
import com.bizgent.domain.Product;
import com.bizgent.domain.Sale;

import lombok.RequiredArgsConstructor;

/** 매출 성과 분석 (F09). 프로토타입 store/sales.ts 이식 */
@Service
@RequiredArgsConstructor
public class SalesAnalysisService {

    public static final Map<String, String> PERIOD_LABELS = Map.of("7", "최근 7일", "30", "최근 30일", "all", "전체 기간",
            "custom", "직접 선택");
    /** 직접 선택 기간의 최대 길이 */
    public static final int MAX_CUSTOM_DAYS = 366;

    private final DemoSession session;

    public record Range(LocalDate start, LocalDate end) {

        public boolean contains(LocalDate d) {
            return !d.isBefore(start) && !d.isAfter(end);
        }

        public long days() {
            return ChronoUnit.DAYS.between(start, end) + 1;
        }
    }

    /** compareLabel: 증감 배지가 무엇과 비교한 값인지 (예: "직전 30일 대비") */
    public record Summary(long revenue, Integer revenueChange, String compareLabel, long count, long avg, long linked,
            int linkedShare) {
    }

    public record DailyPoint(String key, String label, long amount) {
    }

    public record Band(Long campaignId, String title, String x1, String x2) {
    }

    /** status: 인기 / 보통 / 비인기 / 재고 많음 */
    public record MenuRow(Product product, long quantity, long amount, int share, Integer change, String status) {
    }

    /** 캠페인 성과 상세의 막대 그래프 데이터 (게시 전 7일 / 게시 후 7일 일별 대상 메뉴 매출) */
    public record CompareChart(Long campaignId, long before, long after, List<Long> beforeDaily, List<Long> afterDaily,
            List<String> beforeDates, List<String> afterDates) {
    }

    /**
     * 캠페인별 성과. before·after는 게시 전 7일 / 게시 후 7일(게시일 포함) 대상 메뉴 매출, result: 좋음 / 보통 / 아쉬움.
     * 캠페인 기간은 게시 후 7일이고, quantity·storeRevenue·share는 그 기간 기준이다.
     */
    public record CampaignRow(Campaign campaign, String menuNames, String channels, LocalDate periodEnd, long before,
            long after, Integer change, String result, long quantity, long storeRevenue, int share,
            CompareChart chart) {
    }

    public record WeekdayRow(String label, long quantity, long amount, int share, long dailyAvg, boolean top) {
    }

    /** campaignRows: 선택 기간에 게시한 캠페인, campaignDetails: 게시한 모든 캠페인 (성과 상세 팝업용) */
    public record View(LocalDate lastUpload, Range range, Summary summary, List<DailyPoint> series, List<Band> bands,
            List<MenuRow> menuRows, List<CampaignRow> campaignRows, List<CampaignRow> campaignDetails,
            List<WeekdayRow> weekdayRows) {
    }

    /**
     * @param period 7 / 30 / all / custom
     * @param custom period가 custom일 때의 기간 (검증된 값)
     * @return 매출 데이터가 없으면 null
     */
    public View analyze(String period, Range custom) {
        List<Sale> sales = session.getSales();
        if (sales.isEmpty()) {
            return null;
        }
        LocalDate first = sales.stream().map(Sale::getSaleDate).min(Comparator.naturalOrder()).orElseThrow();
        LocalDate last = sales.stream().map(Sale::getSaleDate).max(Comparator.naturalOrder()).orElseThrow();

        // 기간은 마지막 매출일 기준으로 잡아 시연 데이터가 항상 꽉 찬 화면이 되게 한다.
        Range current;
        Range previous = null;
        String compareLabel = null;
        if ("custom".equals(period)) {
            current = custom;
            LocalDate prevEnd = custom.start().minusDays(1);
            if (!prevEnd.isBefore(first)) {
                previous = new Range(custom.start().minusDays(custom.days()), prevEnd);
            }
            compareLabel = "직전 같은 기간(" + custom.days() + "일) 대비";
        } else if ("all".equals(period)) {
            current = new Range(first, last);
        } else {
            int days = "7".equals(period) ? 7 : 30;
            LocalDate start = last.minusDays(days - 1);
            current = new Range(start.isBefore(first) ? first : start, last);
            LocalDate prevEnd = start.minusDays(1);
            if (!prevEnd.isBefore(first)) {
                LocalDate prevStart = start.minusDays(days);
                previous = new Range(prevStart.isBefore(first) ? first : prevStart, prevEnd);
            }
            compareLabel = "직전 " + days + "일 대비";
        }

        List<Campaign> published = session.getCampaigns().stream()
                .filter(c -> c.isPublished() && c.getPublishedAt() != null)
                .toList();
        Range cur = current;
        List<Campaign> untilEnd = published.stream()
                .filter(c -> !c.getPublishedAt().toLocalDate().isAfter(cur.end().plusDays(1))).toList();
        List<Campaign> inPeriod = published.stream().filter(c -> {
            LocalDate p = c.getPublishedAt().toLocalDate();
            return !p.isBefore(cur.start().minusDays(1)) && !p.isAfter(cur.end().plusDays(1));
        }).toList();

        List<CampaignRow> details = campaignRows(sales, published);
        List<CampaignRow> rows = details.stream().filter(r -> inPeriod.contains(r.campaign())).toList();
        return new View(last, current, summary(sales, untilEnd, current, previous, compareLabel),
                dailySeries(sales, current), bands(published, current), menuRows(sales, current, previous), rows,
                details, weekdayRows(sales, current));
    }

    private Summary summary(List<Sale> sales, List<Campaign> campaigns, Range current, Range previous,
            String compareLabel) {
        List<Sale> cur = in(sales, current);
        long revenue = sum(cur, Sale::getAmount);
        long count = sum(cur, Sale::getQuantity);
        long linked = 0;
        for (Campaign c : campaigns) {
            LocalDate p = c.getPublishedAt().toLocalDate();
            Range win = new Range(p, p.plusDays(6));
            linked += sum(cur.stream().filter(s -> c.getProductIds().contains(s.getProductId()) && win.contains(s.getSaleDate()))
                    .toList(), Sale::getAmount);
        }
        Integer change = previous == null ? null : pct(revenue, sum(in(sales, previous), Sale::getAmount));
        return new Summary(revenue, change, compareLabel, count, count == 0 ? 0 : Math.round((double) revenue / count), linked,
                revenue == 0 ? 0 : (int) Math.round(linked * 100.0 / revenue));
    }

    private List<DailyPoint> dailySeries(List<Sale> sales, Range r) {
        Map<LocalDate, Long> byDay = in(sales, r).stream()
                .collect(Collectors.groupingBy(Sale::getSaleDate, Collectors.summingLong(Sale::getAmount)));
        List<DailyPoint> out = new ArrayList<>();
        for (LocalDate d = r.start(); !d.isAfter(r.end()); d = d.plusDays(1)) {
            out.add(new DailyPoint(d.toString(), d.getMonthValue() + "월 " + d.getDayOfMonth() + "일", byDay.getOrDefault(d, 0L)));
        }
        return out;
    }

    /** 차트에 표시할 캠페인 진행 기간 (게시일부터 7일) */
    private List<Band> bands(List<Campaign> campaigns, Range r) {
        return campaigns.stream().map(c -> {
            LocalDate start = c.getPublishedAt().toLocalDate();
            LocalDate end = start.plusDays(6);
            if (end.isBefore(r.start()) || start.isAfter(r.end())) {
                return null;
            }
            return new Band(c.getCampaignId(), c.getTitle(),
                    (start.isBefore(r.start()) ? r.start() : start).toString(),
                    (end.isAfter(r.end()) ? r.end() : end).toString());
        }).filter(b -> b != null).sorted(Comparator.comparing(Band::x1)).toList();
    }

    private List<MenuRow> menuRows(List<Sale> sales, Range current, Range previous) {
        List<Product> products = session.getStore().getProducts();
        List<Sale> cur = in(sales, current);
        List<Sale> prev = previous == null ? List.of() : in(sales, previous);
        long total = sum(cur, Sale::getAmount);

        record Base(Product p, long amount, long quantity, Integer change) {
        }
        List<Base> rows = products.stream().map(p -> {
            List<Sale> mine = cur.stream().filter(s -> s.getProductId().equals(p.getProductId())).toList();
            long amount = sum(mine, Sale::getAmount);
            Integer change = previous == null ? null
                    : pct(amount, sum(prev.stream().filter(s -> s.getProductId().equals(p.getProductId())).toList(), Sale::getAmount));
            return new Base(p, amount, sum(mine, Sale::getQuantity), change);
        }).sorted(Comparator.comparingLong(Base::amount).reversed()).toList();

        Set<Long> popular = new HashSet<>();
        rows.stream().limit(rows.size() >= 5 ? 2 : 1).filter(b -> b.amount() > 0).forEach(b -> popular.add(b.p().getProductId()));
        Long lowest = rows.size() > 2 ? rows.get(rows.size() - 1).p().getProductId() : null;
        long days = current.days();

        return rows.stream().map(b -> {
            double dailyQty = (double) b.quantity() / days;
            Integer stock = b.p().getStockQty();
            // 재고 많음: 지금 판매 속도로 10일 넘게 팔아야 하는 재고
            boolean highStock = stock != null && stock > 0 && stock > Math.max(dailyQty * 10, 10);
            String status = popular.contains(b.p().getProductId()) ? "인기"
                    : b.p().getProductId().equals(lowest) ? "비인기" : highStock ? "재고 많음" : "보통";
            return new MenuRow(b.p(), b.quantity(), b.amount(), total == 0 ? 0 : (int) Math.round(b.amount() * 100.0 / total),
                    b.change(), status);
        }).toList();
    }

    private List<CampaignRow> campaignRows(List<Sale> sales, List<Campaign> campaigns) {
        return campaigns.stream()
                .sorted(Comparator.comparing(Campaign::getPublishedAt).reversed())
                .map(c -> {
                    LocalDate p = c.getPublishedAt().toLocalDate();
                    Range beforeRange = new Range(p.minusDays(7), p.minusDays(1));
                    Range afterRange = new Range(p, p.plusDays(6));
                    List<Sale> mine = sales.stream().filter(s -> c.getProductIds().contains(s.getProductId())).toList();
                    List<Sale> after = in(mine, afterRange);
                    long beforeAmount = sum(in(mine, beforeRange), Sale::getAmount);
                    long afterAmount = sum(after, Sale::getAmount);
                    long storeRevenue = sum(in(sales, afterRange), Sale::getAmount);
                    Integer change = pct(afterAmount, beforeAmount);
                    String result = change == null ? "보통" : change >= 20 ? "좋음" : change >= 5 ? "보통" : "아쉬움";
                    String names = String.join(", ", c.getProductIds().stream().map(session.getStore()::productName)
                            .filter(n -> !n.isEmpty()).toList());
                    List<String> channels = c.getContent().getChannels();
                    CompareChart chart = new CompareChart(c.getCampaignId(), beforeAmount, afterAmount,
                            dailyAmounts(mine, beforeRange), dailyAmounts(mine, afterRange),
                            dates(beforeRange), dates(afterRange));
                    return new CampaignRow(c, names.isEmpty() ? "-" : names,
                            channels.isEmpty() ? "-" : String.join(", ", channels), afterRange.end(), beforeAmount,
                            afterAmount, change, result, sum(after, Sale::getQuantity), storeRevenue,
                            storeRevenue == 0 ? 0 : (int) Math.round(afterAmount * 100.0 / storeRevenue), chart);
                }).toList();
    }

    private static List<Long> dailyAmounts(List<Sale> sales, Range r) {
        return r.start().datesUntil(r.end().plusDays(1))
                .map(d -> sum(sales.stream().filter(s -> s.getSaleDate().equals(d)).toList(), Sale::getAmount))
                .toList();
    }

    private static List<String> dates(Range r) {
        return r.start().datesUntil(r.end().plusDays(1)).map(LocalDate::toString).toList();
    }

    private List<WeekdayRow> weekdayRows(List<Sale> sales, Range r) {
        List<Sale> cur = in(sales, r);
        long total = sum(cur, Sale::getAmount);
        String[] labels = {"월요일", "화요일", "수요일", "목요일", "금요일", "토요일", "일요일"};
        List<WeekdayRow> rows = new ArrayList<>();
        long max = 0;
        for (DayOfWeek dow : DayOfWeek.values()) {
            List<Sale> mine = cur.stream().filter(s -> s.getSaleDate().getDayOfWeek() == dow).toList();
            long amount = sum(mine, Sale::getAmount);
            long dayCount = r.start().datesUntil(r.end().plusDays(1)).filter(d -> d.getDayOfWeek() == dow).count();
            rows.add(new WeekdayRow(labels[dow.getValue() - 1], sum(mine, Sale::getQuantity), amount,
                    total == 0 ? 0 : (int) Math.round(amount * 100.0 / total), dayCount == 0 ? 0 : Math.round((double) amount / dayCount), false));
            max = Math.max(max, amount);
        }
        long top = max;
        return rows.stream().map(w -> new WeekdayRow(w.label(), w.quantity(), w.amount(), w.share(), w.dailyAvg(),
                top > 0 && w.amount() == top)).toList();
    }

    private static List<Sale> in(List<Sale> sales, Range r) {
        return sales.stream().filter(s -> r.contains(s.getSaleDate())).toList();
    }

    private static long sum(List<Sale> sales, ToLongFunction<Sale> f) {
        return sales.stream().mapToLong(f).sum();
    }

    private static Integer pct(long cur, long prev) {
        return prev > 0 ? (int) Math.round((cur - prev) * 100.0 / prev) : null;
    }
}
