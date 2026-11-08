package com.bizgent.web;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.bizgent.service.SalesAnalysisService;
import com.bizgent.service.SalesAnalysisService.Range;
import com.bizgent.service.SalesService;
import com.bizgent.service.SalesService.ConfirmedRow;
import com.bizgent.service.SalesService.RawRow;
import com.bizgent.service.StoreService;

import lombok.RequiredArgsConstructor;

/** 성과분석 (SCR-U-010, F09) · 매출 데이터 올리기 (F17) */
@Controller
@RequiredArgsConstructor
public class PerformanceController {

    private final SalesAnalysisService analysisService;
    private final SalesService salesService;
    private final StoreService storeService;

    /**
     * @param period 7 / 30 / all / custom (custom이면 start~end, yyyy-MM-dd)
     * @param campaign 열자마자 띄울 캠페인 성과 상세 (보관함의 "성과 보기")
     */
    @GetMapping("/performance")
    public String performance(@RequestParam(defaultValue = "30") String period,
            @RequestParam(required = false) String start, @RequestParam(required = false) String end,
            @RequestParam(required = false) Long campaign, @RequestParam(required = false) String upload, Model model) {
        String p = SalesAnalysisService.PERIOD_LABELS.containsKey(period) ? period : "30";
        Range custom = null;
        if ("custom".equals(p)) {
            custom = parseRange(start, end);
            if (custom == null) {
                p = "30";
                model.addAttribute("toast", "기간을 다시 확인해 주세요. 최근 30일 기준으로 보여드릴게요.");
            }
        }
        model.addAttribute("period", p);
        model.addAttribute("view", analysisService.analyze(p, custom));
        model.addAttribute("openUpload", "open".equals(upload));
        model.addAttribute("openCampaign", campaign);
        model.addAttribute("uploads", salesService.uploadHistory());
        model.addAttribute("productOptions", storeService.get().getProducts().stream()
                .map(pr -> Map.of("id", pr.getProductId(), "name", pr.getProductName(), "price", pr.getPrice())).toList());
        return "performance";
    }

    /** 시작일이 종료일보다 늦거나, 형식이 틀리거나, 너무 긴 기간이면 null */
    private static Range parseRange(String start, String end) {
        try {
            Range r = new Range(LocalDate.parse(start), LocalDate.parse(end));
            return r.start().isAfter(r.end()) || r.days() > SalesAnalysisService.MAX_CUSTOM_DAYS ? null : r;
        } catch (DateTimeParseException | NullPointerException e) {
            return null;
        }
    }

    @PostMapping("/api/sales/parse")
    @ResponseBody
    public List<RawRow> parse(@RequestParam MultipartFile file) {
        return salesService.parseFile(file);
    }

    @PostMapping("/api/sales/receipt")
    @ResponseBody
    public List<RawRow> receipt(@RequestParam MultipartFile file) {
        return salesService.recognizeReceipt(file);
    }

    public record CommitRequest(String source, List<ConfirmedRow> rows) {
    }

    @PostMapping("/api/sales/commit")
    @ResponseBody
    public Map<String, Integer> commit(@RequestBody CommitRequest request) {
        return Map.of("count", salesService.commit(request.source(), request.rows()));
    }
}
