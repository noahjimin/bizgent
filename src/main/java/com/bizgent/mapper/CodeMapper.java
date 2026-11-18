package com.bizgent.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.bizgent.domain.PricePlan;
import com.bizgent.mapper.row.CodeRow;

/** 코드 테이블: PRICE_PLAN, GOAL, CHANNEL (03_BizGent_seed.sql로 채워지는 고정 값) */
@Mapper
public interface CodeMapper {

    List<PricePlan> findPricePlans();

    List<CodeRow> findGoals();

    List<CodeRow> findChannels();
}
