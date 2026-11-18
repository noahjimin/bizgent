package com.bizgent.service;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import com.bizgent.domain.Campaign;
import com.bizgent.domain.Member;
import com.bizgent.domain.Notification;
import com.bizgent.domain.Sale;
import com.bizgent.domain.SalesUpload;
import com.bizgent.domain.Store;
import com.bizgent.mapper.MemberMapper;
import com.bizgent.mapper.NotificationMapper;
import com.bizgent.mapper.SalesMapper;
import com.bizgent.mapper.StoreMapper;

import lombok.RequiredArgsConstructor;

/**
 * 로그인한 회원의 데이터를 요청 하나 동안만 기억한다 (같은 요청에서 여러 서비스·화면이 같은 값을 다시 조회하지 않게).
 * 저장한 뒤 다시 읽어야 하면 {@link #clear()}를 부른다.
 */
@Component
@RequestScope
@RequiredArgsConstructor
public class RequestData {

    private final UserSession session;
    private final MemberMapper memberMapper;
    private final StoreMapper storeMapper;
    private final SalesMapper salesMapper;
    private final NotificationMapper notificationMapper;
    private final CampaignRepository campaignRepository;
    private final CodeService codes;

    private Member member;
    private Store store;
    private List<Campaign> campaigns;
    private List<Sale> sales;
    private List<SalesUpload> uploads;
    private List<Notification> notifications;

    /** @return 로그인하지 않았거나 탈퇴한 회원이면 null */
    public Member member() {
        if (member == null && session.getMemberId() != null) {
            Member m = memberMapper.findById(session.getMemberId());
            if (m != null && "ACTIVE".equals(m.getMemberStatus())) {
                m.setPricePlan(codes.pricePlan(m.getPlan()));
                member = m;
            }
        }
        return member;
    }

    public Store store() {
        if (store == null && member() != null) {
            Store s = storeMapper.findByMember(member.getMemberId());
            if (s != null) {
                s.setProducts(storeMapper.findProducts(s.getStoreId(), "ON_SALE"));
                s.setStoppedProducts(storeMapper.findProducts(s.getStoreId(), "STOPPED"));
            }
            store = s;
        }
        return store;
    }

    public List<Campaign> campaigns() {
        if (campaigns == null) {
            campaigns = store() == null ? List.of() : campaignRepository.findAll(store.getStoreId());
        }
        return campaigns;
    }

    public List<Sale> sales() {
        if (sales == null) {
            sales = store() == null ? List.of() : salesMapper.findSales(store.getStoreId());
        }
        return sales;
    }

    public List<SalesUpload> uploads() {
        if (uploads == null) {
            uploads = store() == null ? List.of() : salesMapper.findUploads(store.getStoreId());
        }
        return uploads;
    }

    public List<Notification> notifications() {
        if (notifications == null) {
            notifications = member() == null ? List.of() : notificationMapper.findByMember(member.getMemberId());
        }
        return notifications;
    }

    public void clear() {
        member = null;
        store = null;
        campaigns = null;
        sales = null;
        uploads = null;
        notifications = null;
    }
}
