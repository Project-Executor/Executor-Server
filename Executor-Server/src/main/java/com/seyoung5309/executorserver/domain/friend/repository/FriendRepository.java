package com.seyoung5309.executorserver.domain.friend.repository;

import com.seyoung5309.executorserver.domain.friend.entity.Friend;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FriendRepository extends JpaRepository<Friend, Long> {

    /** 친구 관계는 user.id < friend.id 로 저장되므로 작은 ID를 userId에 넣어서 호출한다 */
    boolean existsByUserIdAndFriendId(Long smallerUserId, Long largerUserId);

    default boolean areFriends(Long a, Long b) {
        if (a.equals(b)) {
            return false;
        }
        return existsByUserIdAndFriendId(Math.min(a, b), Math.max(a, b));
    }
}
