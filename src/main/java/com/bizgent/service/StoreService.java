package com.bizgent.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bizgent.domain.Member;
import com.bizgent.domain.Plan;
import com.bizgent.domain.Product;
import com.bizgent.domain.Store;
import com.bizgent.mapper.StoreMapper;
import com.bizgent.mapper.row.StoreImageRow;
import com.bizgent.service.ai.AiService;
import com.bizgent.service.ai.AiService.GeneratedImage;
import com.bizgent.service.ai.AiService.StoreImageRequest;
import com.bizgent.web.form.StoreForm;

import lombok.RequiredArgsConstructor;

/** 매장·상품 등록 (F02): STORE, PRODUCT, STORE_IMAGE */
@Service
@RequiredArgsConstructor
public class StoreService {

    private final RequestData data;
    private final StoreMapper storeMapper;
    private final FileStorage fileStorage;
    private final AiService aiService;
    private final CodeService codes;

    public Store get() {
        return data.store();
    }

    /** 매장 정보와 상품 목록 저장. 목록에서 빠진 상품은 지우지 않고 판매 중지(STOPPED)로 바꾼다. */
    @Transactional
    public void save(StoreForm form) {
        Store store = data.store();
        store.setStoreName(form.getStoreName().trim());
        String category = form.getCategory() == null ? "" : form.getCategory().trim();
        if (!category.isEmpty()) {
            store.setCategory(category(category, form.getCategoryEtc()));
        }
        store.setPhone(cut(blankToNull(form.getPhone()), 13));
        String color = form.getBrandColor() == null ? "" : form.getBrandColor().trim();
        store.setBrandColor(color.matches("#[0-9a-fA-F]{6}") ? color.toLowerCase() : null);
        store.setTargetCustomer(cut(blankToNull(form.getTargetCustomer()), 500));
        store.setToneManner(cut(blankToNull(form.getToneManner()), 500));
        store.setTargetDescription(cut(blankToNull(form.getTargetDescription()), 500));
        store.setToneDescription(cut(blankToNull(form.getToneDescription()), 500));

        String uploadedHero = fileStorage.saveImage(form.getHeroImageFile(), "store");
        store.setHeroImageUrl(uploadedHero != null ? uploadedHero : blankToNull(form.getHeroImageUrl()));
        storeMapper.updateStore(store);

        // 이름이 빈 상품은 저장하지 않는다.
        List<Long> keep = new ArrayList<>();
        for (StoreForm.ProductForm p : form.getProducts()) {
            if (p == null || p.getProductName() == null || p.getProductName().isBlank()) {
                continue;
            }
            String uploaded = fileStorage.saveImage(p.getImageFile(), "product");
            Product product = new Product(p.getProductId(), cut(p.getProductName().trim(), 100), (int) digits(p.getPrice(), 0),
                    cut(blankToNull(p.getDescription()), 1000), uploaded != null ? uploaded : blankToNull(p.getImageUrl()));
            if (product.getProductId() != null && store.findProduct(product.getProductId()) != null) {
                storeMapper.updateProduct(store.getStoreId(), product);
            } else {
                storeMapper.insertProduct(store.getStoreId(), product);
            }
            keep.add(product.getProductId());
        }
        storeMapper.stopProductsExcept(store.getStoreId(), keep);
        data.clear();
    }

    /** 위자드 1단계에서 새로 입력한 메뉴를 매장 상품으로 추가한다. */
    public Product addProduct(String name) {
        Product product = new Product(null, cut(name, 100), 0, null, null);
        storeMapper.insertProduct(data.store().getStoreId(), product);
        data.store().getProducts().add(product);
        return product;
    }

    /**
     * AI 매장 대표 이미지 3장 생성. 한 번 만들 때마다 이번 달 횟수 1회를 쓴다 (3장 = 1회).
     * 3장을 같은 생성 시각(CREATED_AT)으로 STORE_IMAGE에 남기고, 월 사용 횟수는 생성 시각 묶음 수로 센다.
     */
    @Transactional
    public List<GeneratedImage> generateHeroImages(StoreImageRequest request) {
        Member member = data.member();
        if (member.getRemainingStoreImages() <= 0) {
            throw new ForbiddenException(member.isFree()
                    ? "이번 달 무료 생성 횟수를 모두 사용했어요. 구독하면 월 " + codes.pricePlan(Plan.PRO).getMonthlyImageLimit() + "회까지 만들 수 있어요"
                    : "이번 달 생성 횟수를 모두 사용했어요. 다음 달 1일에 다시 만들 수 있어요");
        }
        String style = "일러스트".equals(request.style()) ? "일러스트" : "실사 사진";
        String extra = request.extraRequest() == null ? "" : request.extraRequest().trim();
        Store store = data.store();
        Long productId = request.productId() != null && store.findProduct(request.productId()) != null ? request.productId() : null;
        request = new StoreImageRequest(style, request.mood(), request.scene(), productId,
                extra.length() > 300 ? extra.substring(0, 300) : extra);
        List<GeneratedImage> images = aiService.generateStoreImages(store, request);
        // CREATED_AT(DATE)은 초 단위라, 같은 초에 다시 만들어도 다른 묶음으로 세도록 직전 생성 시각보다 뒤로 잡는다.
        LocalDateTime createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime last = storeMapper.findLatestImageTime(store.getStoreId());
        if (last != null && !createdAt.isAfter(last)) {
            createdAt = last.plusSeconds(1);
        }
        for (GeneratedImage image : images) {
            storeMapper.insertStoreImage(StoreImageRow.builder().storeId(store.getStoreId()).productId(productId)
                    .imageStyle(style).mood(cut(orDash(request.mood()), 30)).scene(cut(orDash(request.scene()), 30))
                    .extraRequest(blankToNull(request.extraRequest())).imageUrl(image.imageUrl()).createdAt(createdAt).build());
        }
        return images;
    }

    /** 고른 1장을 매장 대표 이미지로 바로 저장한다 (다른 입력값과 별개). */
    public void setHeroImage(String imageUrl) {
        storeMapper.updateHeroImage(data.store().getStoreId(), imageUrl);
    }

    /** 업종 '기타'는 직접 입력한 글자를 그대로 저장한다 (비어 있으면 '기타') */
    public static String category(String category, String etc) {
        String typed = etc == null ? "" : etc.trim();
        return "기타".equals(category) && !typed.isEmpty() ? typed.substring(0, Math.min(50, typed.length())) : category;
    }

    private static long digits(String value, long fallback) {
        String d = value == null ? "" : value.replaceAll("[^0-9]", "");
        return d.isEmpty() ? fallback : Long.parseLong(d);
    }

    /** MOOD·SCENE은 필수 컬럼이라 화면을 거치지 않은 빈 값은 '-'로 남긴다 */
    private static String orDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String cut(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
