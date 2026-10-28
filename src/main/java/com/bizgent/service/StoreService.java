package com.bizgent.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.bizgent.demo.DemoSession;
import com.bizgent.domain.Product;
import com.bizgent.domain.Store;
import com.bizgent.web.form.StoreForm;

import lombok.RequiredArgsConstructor;

/** 매장·상품 등록 (F02) */
@Service
@RequiredArgsConstructor
public class StoreService {

    private final DemoSession session;
    private final FileStorage fileStorage;

    public Store get() {
        return session.getStore();
    }

    public void save(StoreForm form) {
        Store store = session.getStore();
        store.setStoreName(form.getStoreName().trim());
        store.setCategory(form.getCategory());
        store.setPhone(blankToNull(form.getPhone()));
        store.setTargetCustomer(form.getTargetCustomer());
        store.setToneManner(form.getToneManner());
        store.setTargetDescription(blankToNull(form.getTargetDescription()));
        store.setToneDescription(blankToNull(form.getToneDescription()));

        String uploadedHero = fileStorage.saveImage(form.getHeroImageFile(), "store");
        store.setHeroImageUrl(uploadedHero != null ? uploadedHero : form.getHeroImageUrl());

        // 이름이 빈 상품은 저장하지 않는다.
        List<Product> products = new ArrayList<>();
        for (StoreForm.ProductForm p : form.getProducts()) {
            if (p == null || p.getProductName() == null || p.getProductName().isBlank()) {
                continue;
            }
            String uploaded = fileStorage.saveImage(p.getImageFile(), "product");
            products.add(new Product(
                    p.getProductId() != null ? p.getProductId() : session.nextId(),
                    p.getProductName().trim(),
                    (int) digits(p.getPrice(), 0),
                    p.getStockQty() == null || p.getStockQty().replaceAll("[^0-9]", "").isEmpty() ? null
                            : (int) digits(p.getStockQty(), 0),
                    blankToNull(p.getDescription()),
                    uploaded != null ? uploaded : blankToNull(p.getImageUrl())));
        }
        store.setProducts(products);
    }

    /** AI로 만든 대표 이미지는 다른 입력값과 별개로 바로 저장한다. */
    public void setHeroImage(String imageUrl) {
        session.getStore().setHeroImageUrl(imageUrl);
    }

    private static long digits(String value, long fallback) {
        String d = value == null ? "" : value.replaceAll("[^0-9]", "");
        return d.isEmpty() ? fallback : Long.parseLong(d);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
