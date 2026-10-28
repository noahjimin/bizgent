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
    private List<Product> products = new ArrayList<>();

    public Product findProduct(Long productId) {
        return products.stream().filter(p -> p.getProductId().equals(productId)).findFirst().orElse(null);
    }

    public String productName(Long productId) {
        Product product = findProduct(productId);
        return product == null ? "" : product.getProductName();
    }
}
