package com.bizgent.domain;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/** 매장 (STORE) */
@Getter
@Setter
public class Store {

    private Long storeId;
    private String storeName;
    private String category;
    private String description;
    private String phone;
    private String address;
    /** 쉼표로 구분된 타겟 고객 태그 (예: "20대, 30대, 직장인") */
    private String targetCustomer;
    /** 쉼표로 구분된 톤앤매너 태그 */
    private String toneManner;
    private String targetDescription;
    private String toneDescription;
    private String heroImageUrl;
    /** 브랜드 색 #rrggbb (3-2단계 브랜드 컬러 반영). 화면설계서 SCR-U-003·ERD에 없는 팀 결정 추가 항목 */
    private String brandColor;
    /** 판매 중(ON_SALE) 상품. 목록·선택지에는 이것만 보여 준다. */
    private List<Product> products = new ArrayList<>();
    /** 판매 중지(STOPPED) 상품. 지난 캠페인·매출의 메뉴 이름 표시에만 쓴다. */
    private List<Product> stoppedProducts = new ArrayList<>();

    public Product findProduct(Long productId) {
        return products.stream().filter(p -> p.getProductId().equals(productId)).findFirst().orElse(null);
    }

    public String productName(Long productId) {
        return java.util.stream.Stream.concat(products.stream(), stoppedProducts.stream())
                .filter(p -> p.getProductId().equals(productId)).map(Product::getProductName).findFirst().orElse("");
    }
}
