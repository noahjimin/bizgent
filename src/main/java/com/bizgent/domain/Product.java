package com.bizgent.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 상품·메뉴 (PRODUCT). 재고 수량은 범위에서 제외되어 없다. 삭제는 SALE_STATUS='STOPPED'로 처리한다. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    private Long productId;
    private String productName;
    private int price;
    private String description;
    private String imageUrl;
}
