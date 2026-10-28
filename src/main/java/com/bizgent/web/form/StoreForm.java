package com.bizgent.web.form;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import lombok.Getter;
import lombok.Setter;

/** 매장 정보 관리 화면 입력값. 상품은 products[0].productName 형식으로 전달된다. */
@Getter
@Setter
public class StoreForm {

    private String storeName = "";
    private String category = "";
    private String phone = "";
    private String targetCustomer = "";
    private String toneManner = "";
    private String targetDescription = "";
    private String toneDescription = "";
    private String heroImageUrl = "";
    private MultipartFile heroImageFile;
    private List<ProductForm> products = new ArrayList<>();

    @Getter
    @Setter
    public static class ProductForm {

        /** 새로 추가한 상품은 비어 있다 */
        private Long productId;
        private String productName = "";
        /** "6,500원"처럼 입력되므로 숫자만 골라 쓴다 */
        private String price = "";
        private String description = "";
        private String stockQty = "";
        private String imageUrl = "";
        private MultipartFile imageFile;
    }
}
