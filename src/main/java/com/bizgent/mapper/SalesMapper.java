package com.bizgent.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bizgent.domain.Sale;
import com.bizgent.domain.SalesUpload;
import com.bizgent.mapper.row.UploadRow;

/** 매출: SALES_UPLOAD, SALES */
@Mapper
public interface SalesMapper {

    /** 매장의 모든 매출 (source: 업로드 입력 방식 소문자) */
    List<Sale> findSales(Long storeId);

    /** 최근 업로드부터 */
    List<SalesUpload> findUploads(Long storeId);

    Long findLatestUploadId(Long storeId);

    void insertUpload(UploadRow row);

    void insertSale(@Param("uploadId") Long uploadId, @Param("s") Sale sale);

    void deleteSalesByStore(Long storeId);

    void deleteUploadsByStore(Long storeId);
}
