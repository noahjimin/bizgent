package com.bizgent.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 상품·메뉴 (PRODUCT) */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    private Long productId;
    private String productName;
    private int price;
    /** 재고 수량. 사장님이 입력하지 않으면 null */
    private Integer stockQty;
    private String description;
    private String imageUrl;
}
