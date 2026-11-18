package com.bizgent.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.bizgent.demo.DemoSession;
import com.bizgent.domain.Plan;
import com.bizgent.service.ai.AiService.StoreImageRequest;
import com.bizgent.service.ai.MockAiService;

/** 매장 대표 이미지 월 생성 한도: 무료 1회, 구독 2회 (한 번 만들기 = 3장 = 1회) */
class StoreServiceHeroImageTest {

    private static final StoreImageRequest REQUEST = new StoreImageRequest("일러스트", "따뜻한 감성", "매장 내부", null, "");

    @Test
    void 무료는_월_1회까지_만들_수_있다() {
        DemoSession session = new DemoSession();
        session.getMember().setPlan(Plan.FREE);
        session.getMember().setStoreImagesUsedThisMonth(0);
        StoreService service = new StoreService(session, new FileStorage("uploads"), new MockAiService());

        assertEquals(3, service.generateHeroImages(REQUEST).size());
        assertEquals(1, session.getMember().getStoreImagesUsedThisMonth());
        assertEquals(0, session.getMember().getRemainingStoreImages());
        assertThrows(ForbiddenException.class, () -> service.generateHeroImages(REQUEST));
        assertEquals(1, session.getMember().getStoreImagesUsedThisMonth());
    }

    @Test
    void 구독은_월_2회까지_만들_수_있다() {
        DemoSession session = new DemoSession();
        session.getMember().setPlan(Plan.PRO);
        session.getMember().setStoreImagesUsedThisMonth(0);
        StoreService service = new StoreService(session, new FileStorage("uploads"), new MockAiService());

        service.generateHeroImages(REQUEST);
        service.generateHeroImages(REQUEST);
        assertEquals(0, session.getMember().getRemainingStoreImages());
        assertThrows(ForbiddenException.class, () -> service.generateHeroImages(REQUEST));
    }
}
