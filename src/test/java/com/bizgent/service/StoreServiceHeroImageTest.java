package com.bizgent.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.bizgent.domain.Member;
import com.bizgent.domain.Plan;
import com.bizgent.domain.PricePlan;
import com.bizgent.domain.Store;
import com.bizgent.mapper.StoreMapper;
import com.bizgent.mapper.row.StoreImageRow;
import com.bizgent.service.ai.AiService.StoreImageRequest;
import com.bizgent.service.ai.MockAiService;

/** 매장 대표 이미지 월 생성 한도 (PRICE_PLAN.MONTHLY_IMAGE_LIMIT): 한 번 만들기 = 3장 = 1회 */
class StoreServiceHeroImageTest {

    private static final StoreImageRequest REQUEST = new StoreImageRequest("일러스트", "따뜻한 감성", "매장 내부", null, "");

    private final RequestData data = mock(RequestData.class);
    private final StoreMapper stores = mock(StoreMapper.class);
    private final CodeService codes = mock(CodeService.class);
    private final StoreService service = new StoreService(data, stores, new FileStorage("uploads"), new MockAiService(), codes);

    @Test
    void 남은_횟수가_있으면_3장을_같은_생성시각으로_저장한다() {
        when(data.member()).thenReturn(member(Plan.FREE, 1, 0));
        when(data.store()).thenReturn(store());

        assertEquals(3, service.generateHeroImages(REQUEST).size());
        ArgumentCaptor<StoreImageRow> rows = ArgumentCaptor.forClass(StoreImageRow.class);
        verify(stores, times(3)).insertStoreImage(rows.capture());
        List<StoreImageRow> saved = rows.getAllValues();
        assertEquals(1, saved.stream().map(StoreImageRow::getCreatedAt).distinct().count(), "3장 = 생성 시각 하나 = 1회");
        assertEquals("일러스트", saved.get(0).getImageStyle());
    }

    @Test
    void 횟수를_다_쓰면_403_예외() {
        PricePlan pro = new PricePlan();
        pro.setMonthlyImageLimit(2);
        when(codes.pricePlan(Plan.PRO)).thenReturn(pro);
        when(data.member()).thenReturn(member(Plan.FREE, 1, 1));
        when(data.store()).thenReturn(store());

        ForbiddenException e = assertThrows(ForbiddenException.class, () -> service.generateHeroImages(REQUEST));
        assertEquals("이번 달 무료 생성 횟수를 모두 사용했어요. 구독하면 월 2회까지 만들 수 있어요", e.getMessage());
        verify(stores, never()).insertStoreImage(org.mockito.ArgumentMatchers.any());
    }

    private static Member member(Plan plan, int limit, int used) {
        PricePlan p = new PricePlan();
        p.setMonthlyImageLimit(limit);
        Member m = new Member();
        m.setPlan(plan);
        m.setPricePlan(p);
        m.setStoreImagesUsedThisMonth(used);
        return m;
    }

    private static Store store() {
        Store s = new Store();
        s.setStoreId(10L);
        s.setStoreName("모먼트 카페");
        s.setCategory("카페·디저트");
        return s;
    }
}
