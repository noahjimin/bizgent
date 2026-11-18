package com.bizgent.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.bizgent.demo.DemoSession;
import com.bizgent.domain.Member;
import com.bizgent.domain.Plan;
import com.bizgent.domain.Product;
import com.bizgent.domain.Store;
import com.bizgent.service.ai.AiService;
import com.bizgent.service.ai.AiService.GeneratedImage;
import com.bizgent.service.ai.AiService.StoreImageRequest;
import com.bizgent.web.form.StoreForm;

import lombok.RequiredArgsConstructor;

/** 매장·상품 등록 (F02) */
@Service
@RequiredArgsConstructor
public class StoreService {

    private final DemoSession session;
    private final FileStorage fileStorage;
    private final AiService aiService;

    public Store get() {
        return session.getStore();
    }

    public void save(StoreForm form) {
        Store store = session.getStore();
        store.setStoreName(form.getStoreName().trim());
        store.setCategory(form.getCategory());
        store.setPhone(blankToNull(form.getPhone()));
        String color = form.getBrandColor() == null ? "" : form.getBrandColor().trim();
        store.setBrandColor(color.matches("#[0-9a-fA-F]{6}") ? color.toLowerCase() : null);
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

    /**
     * AI 매장 대표 이미지 3장 생성. 한 번 만들 때마다 이번 달 횟수 1회를 차감한다 (3장 = 1회).
     * DB 단계에서는 사용 횟수를 STORE_IMAGE의 생성 시각 묶음 수로 센다.
     */
    public List<GeneratedImage> generateHeroImages(StoreImageRequest request) {
        Member member = session.getMember();
        if (member.getRemainingStoreImages() <= 0) {
            throw new ForbiddenException(member.isFree()
                    ? "이번 달 무료 생성 횟수를 모두 사용했어요. 구독하면 월 " + Plan.PRO.getMonthlyStoreImages() + "회까지 만들 수 있어요"
                    : "이번 달 생성 횟수를 모두 사용했어요. 다음 달 1일에 다시 만들 수 있어요");
        }
        String style = "일러스트".equals(request.style()) ? "일러스트" : "실사 사진";
        String extra = request.extraRequest() == null ? "" : request.extraRequest().trim();
        request = new StoreImageRequest(style, request.mood(), request.scene(), request.productId(),
                extra.length() > 300 ? extra.substring(0, 300) : extra);
        List<GeneratedImage> images = aiService.generateStoreImages(session.getStore(), request);
        member.setStoreImagesUsedThisMonth(member.getStoreImagesUsedThisMonth() + 1);
        return images;
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
