package com.bizgent.mapper.row;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** GOAL·CHANNEL 코드 한 줄 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeRow {

    private String code;
    private String name;
    private int sortOrder;
}
