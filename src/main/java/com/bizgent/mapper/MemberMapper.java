package com.bizgent.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bizgent.domain.Member;

/** 회원·구독·월 이용 횟수: MEMBERS, SUBSCRIPTION, MONTHLY_USAGE */
@Mapper
public interface MemberMapper {

    /** 현재 요금제(SUBSCRIPTION ACTIVE, 없으면 FREE)와 이번 달 캠페인·매장 이미지 사용 횟수를 함께 읽는다. */
    Member findById(Long memberId);

    /** 이메일은 대소문자를 구분하지 않는다. */
    Member findByEmail(String email);

    int countByEmail(String email);

    void insert(Member member);

    void updateAccount(@Param("memberId") Long memberId, @Param("memberName") String memberName,
            @Param("phone") String phone);

    void updatePassword(@Param("memberId") Long memberId, @Param("password") String password);

    /** 탈퇴: 상태와 일시만 기록한다 (15일 후 삭제는 별도 작업) */
    void withdraw(Long memberId);

    /** 이번 달(YYYYMM) 캠페인 이용 횟수 +1. 그달 첫 사용이면 새 행 */
    void addUsage(@Param("memberId") Long memberId, @Param("month") String month);

    /** 개발 바로가기: 이번 달 이용 횟수를 지정한 값으로 */
    void setUsage(@Param("memberId") Long memberId, @Param("month") String month, @Param("count") int count);

    /** 구독 시작 (모의 결제: 결제 상태 PAID로 바로 기록) */
    void insertSubscription(@Param("memberId") Long memberId, @Param("planCode") String planCode);

    /** 구독 해지: ACTIVE 행을 CANCELED로 */
    void cancelSubscriptions(Long memberId);

    void deleteUsage(Long memberId);

    void deleteSubscriptions(Long memberId);
}
