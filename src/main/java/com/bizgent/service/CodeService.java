package com.bizgent.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.bizgent.domain.Plan;
import com.bizgent.domain.PricePlan;
import com.bizgent.mapper.CodeMapper;
import com.bizgent.mapper.row.CodeRow;

import lombok.RequiredArgsConstructor;

/**
 * 코드 값 변환: 화면·도메인은 한글 이름(목표·채널), DB는 GOAL_CODE·CHANNEL_CODE를 쓴다.
 * 코드 테이블은 초기 데이터로만 바뀌므로 처음 읽은 값을 계속 쓴다.
 */
@Service
@RequiredArgsConstructor
public class CodeService {

    public static final String GOAL_ETC = "ETC";
    public static final String CHANNEL_ETC = "ETC";

    private final CodeMapper codeMapper;

    private volatile Map<Plan, PricePlan> plans;
    private volatile List<CodeRow> goals;
    private volatile List<CodeRow> channels;

    public PricePlan pricePlan(Plan plan) {
        if (plans == null) {
            Map<Plan, PricePlan> map = new LinkedHashMap<>();
            codeMapper.findPricePlans().forEach(p -> map.put(p.getPlanCode(), p));
            plans = map;
        }
        return plans.get(plan);
    }

    /** @return 목록에 없는 이름이면 null */
    public String goalCode(String name) {
        return find(goals(), name, true);
    }

    public String goalName(String code) {
        return find(goals(), code, false);
    }

    /** @return 목록에 없는 이름이면 null */
    public String channelCode(String name) {
        return find(channels(), name, true);
    }

    public String channelName(String code) {
        return find(channels(), code, false);
    }

    private List<CodeRow> goals() {
        if (goals == null) {
            goals = codeMapper.findGoals();
        }
        return goals;
    }

    private List<CodeRow> channels() {
        if (channels == null) {
            channels = codeMapper.findChannels();
        }
        return channels;
    }

    private static String find(List<CodeRow> rows, String key, boolean byName) {
        if (key == null) {
            return null;
        }
        return rows.stream().filter(r -> key.equals(byName ? r.getName() : r.getCode()))
                .map(r -> byName ? r.getCode() : r.getName()).findFirst().orElse(null);
    }
}
