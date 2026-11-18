package com.bizgent.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.bizgent.domain.Product;
import com.bizgent.domain.Sale;
import com.bizgent.domain.SalesUpload;
import com.bizgent.mapper.SalesMapper;
import com.bizgent.mapper.row.UploadRow;

import lombok.RequiredArgsConstructor;

/** 매출 데이터 업로드 (F17): 엑셀/CSV, 영수증 사진(모의 OCR), 직접 입력 */
@Service
@RequiredArgsConstructor
public class SalesService {

    private final RequestData data;
    private final SalesMapper salesMapper;

    /** 업로드에서 읽은 한 줄. productId는 매장 상품과 이름이 일치할 때만 채워진다. */
    public record RawRow(String date, String menuName, Long productId, Integer quantity, Long amount) {
    }

    /** 화면에서 확인을 마친 한 줄 */
    public record ConfirmedRow(String date, Long productId, int quantity, long amount) {
    }

    /** CSV(date,menu,quantity,amount)를 읽는다. 엑셀은 아직 모의 데이터로 대신한다. */
    public List<RawRow> parseFile(MultipartFile file) {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (name.endsWith(".csv")) {
            List<RawRow> rows = parseCsv(file);
            if (!rows.isEmpty()) {
                return rows;
            }
        }
        return sampleRows(20, List.of("말차라떼", "레몬에이드"));
    }

    /** 모의 영수증 인식 */
    public List<RawRow> recognizeReceipt(MultipartFile image) {
        return sampleRows(6, List.of("딸기케잌"));
    }

    /** 매출 업로드 기록, 최근 순 (SCR-U-008 ③ 자세히 보기) */
    public List<SalesUpload> uploadHistory() {
        return data.uploads();
    }

    /**
     * 확인을 마친 매출을 저장한다. 직접 입력도 업로드 한 건(SALES_UPLOAD)으로 남기고 데이터 기간(DATA_FROM~DATA_TO)을 기록한다.
     *
     * @return 반영한 건수
     */
    @Transactional
    public int commit(String source, List<ConfirmedRow> rows) {
        String inputType = switch (source == null ? "" : source) {
            case "excel" -> "EXCEL";
            case "receipt" -> "RECEIPT";
            default -> "MANUAL";
        };
        List<Sale> sales = new ArrayList<>();
        for (ConfirmedRow r : rows) {
            Product product = data.store().findProduct(r.productId());
            if (product == null || r.quantity() <= 0) {
                continue;
            }
            sales.add(new Sale(null, LocalDate.parse(r.date()), product.getProductId(), r.quantity(), r.amount(),
                    inputType.toLowerCase(Locale.ROOT)));
        }
        if (sales.isEmpty()) {
            return 0;
        }
        UploadRow upload = UploadRow.builder().storeId(data.store().getStoreId()).inputType(inputType)
                .recordCount(sales.size())
                .dataFrom(sales.stream().map(Sale::getSaleDate).min(LocalDate::compareTo).orElseThrow())
                .dataTo(sales.stream().map(Sale::getSaleDate).max(LocalDate::compareTo).orElseThrow())
                .uploadedAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)).build();
        salesMapper.insertUpload(upload);
        sales.forEach(sale -> salesMapper.insertSale(upload.getUploadId(), sale));
        data.clear();
        return sales.size();
    }

    private List<RawRow> parseCsv(MultipartFile file) {
        String text;
        try {
            text = new String(file.getBytes(), StandardCharsets.UTF_8).replace("﻿", "");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        List<String> lines = text.lines().map(String::trim).filter(l -> !l.isEmpty()).toList();
        if (!lines.isEmpty() && lines.get(0).matches("(?i).*(date|날짜).*")) {
            lines = lines.subList(1, lines.size());
        }
        List<RawRow> rows = new ArrayList<>();
        for (String line : lines) {
            String[] c = line.split(",", -1);
            String date = cell(c, 0);
            String menu = cell(c, 1);
            Long qty = number(cell(c, 2));
            rows.add(new RawRow(date, menu, match(menu), qty == null ? null : qty.intValue(), number(cell(c, 3))));
        }
        return rows;
    }

    /** 시연용 인식 결과. 확인이 필요한 줄(모르는 메뉴, 금액 누락)을 일부러 섞는다. */
    private List<RawRow> sampleRows(int count, List<String> unknown) {
        List<Product> menus = data.store().getProducts();
        int n = Math.max(1, menus.size());
        LocalDate base = LocalDate.of(2026, 10, 13);
        List<RawRow> rows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Product menu = menus.isEmpty() ? null : menus.get(i % n);
            int qty = 1 + (i * 3) % 5;
            rows.add(new RawRow(base.plusDays(i / n).toString(), menu == null ? "메뉴" : menu.getProductName(),
                    menu == null ? null : menu.getProductId(), qty, (long) (menu == null ? 5000 : menu.getPrice()) * qty));
        }
        for (int i = 0; i < unknown.size(); i++) {
            String date = rows.get(Math.min(rows.size() - 1, 4 + i)).date();
            rows.add(Math.min(rows.size(), 4 + i * 2), new RawRow(date, unknown.get(i), match(unknown.get(i)), 1 + i, 5500L * (1 + i)));
        }
        if (rows.size() > 1) {
            RawRow r = rows.get(1);
            rows.set(1, new RawRow(r.date(), r.menuName(), r.productId(), r.quantity(), null));
        }
        return rows;
    }

    /** 공백·대소문자를 무시하고 매장 상품명과 맞춘다. */
    private Long match(String menuName) {
        String key = normalize(menuName);
        return data.store().getProducts().stream()
                .filter(p -> normalize(p.getProductName()).equals(key))
                .map(Product::getProductId).findFirst().orElse(null);
    }

    private static String normalize(String s) {
        return s == null ? "" : s.replaceAll("\\s", "").toLowerCase(Locale.ROOT);
    }

    private static String cell(String[] cells, int i) {
        return i < cells.length ? cells[i].trim().replaceAll("^\"|\"$", "") : "";
    }

    private static Long number(String value) {
        String d = value.replaceAll("[^0-9]", "");
        return d.isEmpty() ? null : Long.parseLong(d);
    }
}
