package com.bizgent.mapper.row;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** STORE_IMAGE 한 줄 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreImageRow {

    private Long storeImageId;
    private Long storeId;
    private Long productId;
    private String imageStyle;
    private String mood;
    private String scene;
    private String extraRequest;
    private String imageUrl;
    private LocalDateTime createdAt;
}
