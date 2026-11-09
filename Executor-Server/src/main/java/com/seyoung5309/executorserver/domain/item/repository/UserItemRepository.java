package com.seyoung5309.executorserver.domain.item.repository;

import com.seyoung5309.executorserver.domain.item.entity.UserItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserItemRepository extends JpaRepository<UserItem, Long> {

    /** 보유 아이템 한 건 (아이템 정보 포함) */
    @EntityGraph(attributePaths = "item")
    Optional<UserItem> findByUserIdAndItemId(Long userId, Long itemId);
}
