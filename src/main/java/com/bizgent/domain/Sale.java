package com.bizgent.domain;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 매출 (SALES). 업로드 단위는 DB 단계에서 SALES_UPLOAD로 묶는다. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Sale {

    private Long salesId;
    private LocalDate saleDate;
    private Long productId;
    private int quantity;
    private long amount;
    /** 입력 방식: excel / receipt / manual (SALES_UPLOAD.INPUT_TYPE) */
    private String source;
}
