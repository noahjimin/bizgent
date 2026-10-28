package com.bizgent.web;

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

    @GetMapping("/performance")
    public String performance(@RequestParam(defaultValue = "30") String period,
            @RequestParam(required = false) String upload, Model model) {
        String p = SalesAnalysisService.PERIOD_LABELS.containsKey(period) ? period : "30";
        model.addAttribute("period", p);
        model.addAttribute("view", analysisService.analyze(p));
        model.addAttribute("openUpload", "open".equals(upload));
        model.addAttribute("productOptions", storeService.get().getProducts().stream()
                .map(pr -> Map.of("id", pr.getProductId(), "name", pr.getProductName(), "price", pr.getPrice())).toList());
        return "performance";
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
