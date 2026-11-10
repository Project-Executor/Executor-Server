package com.seyoung5309.executorserver.domain.friend.repository;

import com.seyoung5309.executorserver.domain.friend.entity.Friend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FriendRepository extends JpaRepository<Friend, Long> {

    /** 친구 관계는 user.id < friend.id 로 저장되므로 작은 ID를 userId에 넣어서 호출한다 */
    boolean existsByUserIdAndFriendId(Long smallerUserId, Long largerUserId);

    /** 내 친구들의 ID (친구 관계는 한 행만 저장되므로 양쪽 컬럼을 모두 본다) */
    @Query("""
            select case when f.user.id = :userId then f.friend.id else f.user.id end
            from Friend f
            where f.user.id = :userId or f.friend.id = :userId
            """)
    List<Long> findFriendIds(@Param("userId") Long userId);

    default boolean areFriends(Long a, Long b) {
        if (a.equals(b)) {
            return false;
        }
        return existsByUserIdAndFriendId(Math.min(a, b), Math.max(a, b));
    }
}
