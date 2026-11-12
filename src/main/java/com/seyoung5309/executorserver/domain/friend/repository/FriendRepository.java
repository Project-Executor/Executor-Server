package com.seyoung5309.executorserver.domain.friend.repository;

import com.seyoung5309.executorserver.domain.friend.entity.Friend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
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

    /** 내 친구들의 ID와 친구가 된 시각 */
    @Query("""
            select new com.seyoung5309.executorserver.domain.friend.repository.FriendRow(
                case when f.user.id = :userId then f.friend.id else f.user.id end, f.createdAt)
            from Friend f
            where f.user.id = :userId or f.friend.id = :userId
            """)
    List<FriendRow> findFriendRows(@Param("userId") Long userId);

    /**
     * 친구 관계를 만든다 (작은 ID, 큰 ID 순서). 이미 있으면 아무것도 하지 않는다 (UNIQUE(user_id, friend_id)).
     */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT IGNORE INTO friends (user_id, friend_id, created_at)
            VALUES (:smallerUserId, :largerUserId, :now)
            """, nativeQuery = true)
    int insertPair(@Param("smallerUserId") Long smallerUserId, @Param("largerUserId") Long largerUserId,
                   @Param("now") Instant now);

    /** @return 지웠으면 1, 친구가 아니었으면 0 */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Friend f where f.user.id = :smallerUserId and f.friend.id = :largerUserId")
    int deletePair(@Param("smallerUserId") Long smallerUserId, @Param("largerUserId") Long largerUserId);

    default boolean areFriends(Long a, Long b) {
        if (a.equals(b)) {
            return false;
        }
        return existsByUserIdAndFriendId(Math.min(a, b), Math.max(a, b));
    }
}
