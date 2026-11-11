package com.seyoung5309.executorserver.domain.item.repository;

import com.seyoung5309.executorserver.domain.item.entity.ItemType;
import com.seyoung5309.executorserver.domain.item.entity.UserItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserItemRepository extends JpaRepository<UserItem, Long> {

    /** 이 아이템을 가진 사용자가 한 명이라도 있는지 (다 쓴 힌트 포함) */
    boolean existsByItemId(Long itemId);

    /** 보유 아이템 한 건 (아이템 정보 포함) */
    @EntityGraph(attributePaths = "item")
    Optional<UserItem> findByUserIdAndItemId(Long userId, Long itemId);

    /** 상점 목록의 보유 표시용: 수량이 1 이상인 보유 아이템 */
    @Query("""
            select new com.seyoung5309.executorserver.domain.item.repository.OwnedItemRow(ui.item.id, ui.quantity)
            from UserItem ui
            where ui.user.id = :userId and ui.quantity > 0
            """)
    List<OwnedItemRow> findOwnedRows(@Param("userId") Long userId);

    /**
     * 내 보유 아이템 (아이템 정보 포함, 아이템 ID 순). 힌트를 다 써서 수량이 0인 행은 뺀다.
     *
     * @param itemType null이면 전체
     */
    @Query("""
            select ui from UserItem ui join fetch ui.item i
            where ui.user.id = :userId and ui.quantity > 0
              and (:itemType is null or i.itemType = :itemType)
            order by i.id asc
            """)
    List<UserItem> findMyItems(@Param("userId") Long userId, @Param("itemType") ItemType itemType);

    @Query("select ui.quantity from UserItem ui where ui.user.id = :userId and ui.item.id = :itemId")
    Optional<Integer> findQuantity(@Param("userId") Long userId, @Param("itemId") Long itemId);

    /**
     * 스킨·테마처럼 한 번만 가질 수 있는 아이템을 지급한다. 이미 있으면 아무것도 하지 않는다 (UNIQUE(user_id, item_id)).
     * itemId는 존재하는 값이어야 한다 (INSERT IGNORE는 FK 오류도 무시하므로 호출 전에 확인할 것).
     *
     * @return 새로 지급했으면 1, 이미 가지고 있었으면 0
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT IGNORE INTO user_items (user_id, item_id, quantity, acquired_at)
            VALUES (:userId, :itemId, 1, :now)
            """, nativeQuery = true)
    int insertIfAbsent(@Param("userId") Long userId, @Param("itemId") Long itemId, @Param("now") Instant now);

    /**
     * 힌트처럼 쌓이는 아이템을 지급한다. 행이 없으면 만들고, 있으면 수량을 더한다 (처음 획득일은 그대로).
     * DB에서 바로 더하므로 동시 요청에도 수량이 유실되지 않는다.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO user_items (user_id, item_id, quantity, acquired_at)
            VALUES (:userId, :itemId, :quantity, :now) AS new
            ON DUPLICATE KEY UPDATE quantity = user_items.quantity + new.quantity
            """, nativeQuery = true)
    void addQuantity(@Param("userId") Long userId, @Param("itemId") Long itemId,
                     @Param("quantity") int quantity, @Param("now") Instant now);

    /**
     * 힌트 아이템 1개를 쓴다. 힌트 종류가 여러 개면 아이템 ID가 가장 작은 것부터 쓴다.
     * 수량 확인(quantity > 0)과 차감이 한 문장이라 동시 요청에도 수량이 음수가 되지 않는다.
     *
     * @return 썼으면 1, 남은 힌트가 없으면 0
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE user_items
            SET quantity = quantity - 1
            WHERE user_id = :userId
              AND quantity > 0
              AND item_id IN (SELECT id FROM items WHERE item_type = 'HINT')
            ORDER BY item_id
            LIMIT 1
            """, nativeQuery = true)
    int useOneHint(@Param("userId") Long userId);

    /** 가진 힌트 아이템의 총 수량 (힌트 종류가 여러 개면 합친다) */
    @Query("""
            select coalesce(sum(ui.quantity), 0) from UserItem ui
            where ui.user.id = :userId
              and ui.item.itemType = com.seyoung5309.executorserver.domain.item.entity.ItemType.HINT
            """)
    long sumHintQuantity(@Param("userId") Long userId);
}
