package com.bizgent.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bizgent.domain.Product;
import com.bizgent.domain.Store;
import com.bizgent.mapper.row.StoreImageRow;

/** 매장·상품·매장 이미지: STORE, PRODUCT, STORE_IMAGE */
@Mapper
public interface StoreMapper {

    Store findByMember(Long memberId);

    /** @param status ON_SALE / STOPPED */
    List<Product> findProducts(@Param("storeId") Long storeId, @Param("status") String status);

    void insertStore(@Param("memberId") Long memberId, @Param("s") Store store);

    void updateStore(Store store);

    void updateHeroImage(@Param("storeId") Long storeId, @Param("url") String url);

    void insertProduct(@Param("storeId") Long storeId, @Param("p") Product product);

    void updateProduct(@Param("storeId") Long storeId, @Param("p") Product product);

    /** 목록에서 빠진 상품은 지우지 않고 판매 중지로 바꾼다 (매출·캠페인 기록이 연결될 수 있어서). */
    void stopProductsExcept(@Param("storeId") Long storeId, @Param("keepIds") List<Long> keepIds);

    void insertStoreImage(StoreImageRow row);

    /** 이 매장의 가장 최근 이미지 생성 시각 (없으면 null) */
    java.time.LocalDateTime findLatestImageTime(Long storeId);

    /** 개발 바로가기 "횟수 초기화": 이번 달 생성 기록을 지운다 (시연 계정 전용) */
    void deleteStoreImagesThisMonth(Long storeId);

    void deleteStoreImages(Long storeId);

    void deleteProducts(Long storeId);

    void deleteStore(Long storeId);
}
