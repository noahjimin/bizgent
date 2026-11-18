package com.bizgent.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.bizgent.domain.AiPlan;
import com.bizgent.domain.Campaign;
import com.bizgent.domain.CampaignStatus;
import com.bizgent.domain.Draft;
import com.bizgent.domain.Member;
import com.bizgent.domain.Post;
import com.bizgent.domain.Product;
import com.bizgent.domain.Store;
import com.bizgent.mapper.CampaignMapper;
import com.bizgent.mapper.MemberMapper;
import com.bizgent.mapper.NotificationMapper;
import com.bizgent.mapper.StoreMapper;

/**
 * 캠페인을 지우면 하위 기록이 FK ON DELETE CASCADE로 함께 지워지는지 실제 DB에서 확인한다.
 * 테스트 트랜잭션은 끝나면 롤백되므로 DB에 데이터가 남지 않는다 (시퀀스 번호만 소비).
 */
@SpringBootTest
@Transactional
class CampaignDeleteCascadeTest {

    @Autowired
    MemberMapper memberMapper;
    @Autowired
    StoreMapper storeMapper;
    @Autowired
    CampaignMapper campaignMapper;
    @Autowired
    NotificationMapper notificationMapper;
    @Autowired
    CampaignRepository repository;
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void deletingCampaignRemovesAllChildRows() {
        Member member = new Member();
        member.setEmail("cascade-test@bizgent.test");
        member.setMemberName("테스트");
        member.setPassword("x");
        member.setTermsAgreedAt(LocalDateTime.now());
        memberMapper.insert(member);
        Store store = new Store();
        store.setStoreName("테스트 매장");
        store.setCategory("기타");
        storeMapper.insertStore(member.getMemberId(), store);
        Product product = new Product(null, "테스트 메뉴", 1000, null, null);
        storeMapper.insertProduct(store.getStoreId(), product);

        Campaign c = new Campaign();
        c.setTitle("삭제 테스트");
        c.setCreatedAt(LocalDateTime.now());
        c.setPublishedAt(LocalDateTime.now());
        c.setStatus(CampaignStatus.PUBLISHED);
        c.setLockedStep("5");
        c.setGoal("재방문 유도");
        c.setProductIds(new ArrayList<>(List.of(product.getProductId())));
        c.setPlan(new AiPlan("방향", "타겟", "전략", "효과"));
        c.getContent().setChannels(new ArrayList<>(List.of("인스타그램")));
        c.getImage().setPurpose("신메뉴 홍보");
        c.getImage().getRatios().put("인스타그램", "4:5 세로 (추천)");
        c.setDrafts(new ArrayList<>(List.of(new Draft("A", "인스타그램", "문구", new ArrayList<>(List.of("태그")), ""))));
        c.setSelectedDraftKeys(new ArrayList<>(List.of("A")));
        c.setPosts(new ArrayList<>(List.of(new Post("인스타그램", "A", Post.SUCCESS).reactions(1, 2, 3, 4, 5))));
        repository.insert(c, store.getStoreId());
        repository.insertPosts(c);
        notificationMapper.insert(member.getMemberId(), c.getCampaignId(), NotificationService.TYPE_CAMPAIGN_RESULT, "알림",
                LocalDateTime.now());

        Long id = c.getCampaignId();
        Long draftId = c.getDrafts().get(0).getDraftId();
        Long postId = jdbc.queryForObject("SELECT POST_ID FROM POST WHERE DRAFT_ID = ?", Long.class, draftId);
        assertEquals(9, childRows(id, draftId, postId), "하위 기록 9종이 한 줄씩 저장됨");
        assertEquals("4:5", jdbc.queryForObject("SELECT IMAGE_RATIO FROM CAMPAIGN_CHANNEL WHERE CAMPAIGN_ID = ?", String.class, id));

        assertEquals(1, campaignMapper.deleteCampaign(store.getStoreId(), id));
        assertEquals(0, childRows(id, draftId, postId), "캠페인을 지우면 하위 기록도 함께 지워짐");
    }

    /** CAMPAIGN_PRODUCT, CAMPAIGN_CHANNEL, AI_PLAN, IMAGE_SETTING, DRAFT, DRAFT_HASHTAG, POST, POST_METRIC, NOTIFICATION */
    private int childRows(Long id, Long draftId, Long postId) {
        int total = 0;
        for (String sql : List.of("SELECT COUNT(*) FROM CAMPAIGN_PRODUCT WHERE CAMPAIGN_ID = " + id,
                "SELECT COUNT(*) FROM CAMPAIGN_CHANNEL WHERE CAMPAIGN_ID = " + id,
                "SELECT COUNT(*) FROM AI_PLAN WHERE CAMPAIGN_ID = " + id,
                "SELECT COUNT(*) FROM IMAGE_SETTING WHERE CAMPAIGN_ID = " + id,
                "SELECT COUNT(*) FROM DRAFT WHERE CAMPAIGN_ID = " + id,
                "SELECT COUNT(*) FROM DRAFT_HASHTAG WHERE DRAFT_ID = " + draftId,
                "SELECT COUNT(*) FROM POST WHERE POST_ID = " + postId,
                "SELECT COUNT(*) FROM POST_METRIC WHERE POST_ID = " + postId,
                "SELECT COUNT(*) FROM NOTIFICATION WHERE CAMPAIGN_ID = " + id)) {
            total += jdbc.queryForObject(sql, Integer.class);
        }
        return total;
    }
}
