package com.bizgent.demo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import com.bizgent.domain.Campaign;
import com.bizgent.domain.Member;
import com.bizgent.domain.Notification;
import com.bizgent.domain.Sale;
import com.bizgent.domain.Store;

import lombok.Getter;
import lombok.Setter;

/**
 * 화면 구현 단계용 세션별 데모 데이터 (프로토타입의 localStorage 역할).
 * DB 연동 단계에서는 서비스 계층이 이 클래스 대신 MyBatis Mapper를 사용한다.
 */
@Getter
@Setter
@Component
@SessionScope
public class DemoSession implements Serializable {

    private boolean loggedIn;
    private boolean termsAgreed;
    /** 랜딩에서 누른 버튼: start / heroImage */
    private String startIntent;
    /** AI 캠페인 개선 방향 화면의 데이터 양: early(적음) / enough(충분) */
    private String insightsView = "early";
    /** 현재 위자드에서 작성 중인 캠페인 */
    private Long activeDraftId;

    private Member member;
    private Store store;
    private List<Campaign> campaigns = new ArrayList<>();
    private List<Sale> sales = new ArrayList<>();
    private List<Notification> notifications = new ArrayList<>();

    private final AtomicLong sequence = new AtomicLong(1000);

    public DemoSession() {
        reset();
    }

    public void reset() {
        DemoSeed.fill(this);
    }

    /** DB 시퀀스 대신 사용하는 ID 발급 */
    public long nextId() {
        return sequence.incrementAndGet();
    }
}
