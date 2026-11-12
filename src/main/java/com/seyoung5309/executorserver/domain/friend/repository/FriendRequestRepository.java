package com.seyoung5309.executorserver.domain.friend.repository;

import com.seyoung5309.executorserver.domain.friend.entity.FriendRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    @Query("""
            select new com.seyoung5309.executorserver.domain.friend.repository.RequestPair(r.sender.id, r.receiver.id)
            from FriendRequest r where r.id = :requestId
            """)
    Optional<RequestPair> findPair(@Param("requestId") Long requestId);

    Optional<FriendRequest> findBySenderIdAndReceiverId(Long senderId, Long receiverId);

    /**
     * 요청을 잠금 읽기(SELECT ... FOR UPDATE)로 읽는다.
     * <p>
     * MySQL 기본 격리 수준(REPEATABLE READ)에서 일반 SELECT는 트랜잭션의 첫 조회 시점 스냅샷을 보므로,
     * 잠금을 기다리는 사이에 다른 트랜잭션이 수락·취소한 결과를 못 볼 수 있다. 잠금 읽기는 항상 최신 커밋을 읽는다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from FriendRequest r where r.id = :requestId")
    Optional<FriendRequest> findByIdForUpdate(@Param("requestId") Long requestId);

    /** 두 사람 사이의 요청을 잠금 읽기로 (요청 보내기에서 최신 상태를 보기 위함) */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from FriendRequest r where r.sender.id = :senderId and r.receiver.id = :receiverId")
    Optional<FriendRequest> findForUpdate(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);

    /** 나에게 온 대기 중 요청 (최근 순) */
    @Query("""
            select r from FriendRequest r
            where r.receiver.id = :userId
              and r.status = com.seyoung5309.executorserver.domain.friend.entity.FriendRequestStatus.PENDING
            order by r.createdAt desc, r.id desc
            """)
    List<FriendRequest> findReceivedPending(@Param("userId") Long userId);

    /** 내가 보낸 대기 중 요청 (최근 순) */
    @Query("""
            select r from FriendRequest r
            where r.sender.id = :userId
              and r.status = com.seyoung5309.executorserver.domain.friend.entity.FriendRequestStatus.PENDING
            order by r.createdAt desc, r.id desc
            """)
    List<FriendRequest> findSentPending(@Param("userId") Long userId);

    /** 두 사용자 사이의 요청을 방향과 상관없이 모두 지운다 (친구를 끊을 때 정리용) */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from FriendRequest r
            where (r.sender.id = :a and r.receiver.id = :b) or (r.sender.id = :b and r.receiver.id = :a)
            """)
    void deleteBetween(@Param("a") Long a, @Param("b") Long b);
}
