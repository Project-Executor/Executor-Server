package com.seyoung5309.executorserver.domain.mail.repository;

import com.seyoung5309.executorserver.domain.mail.entity.Mail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MailRepository extends JpaRepository<Mail, Long> {

    /** 만료되지 않은 내 우편 (첨부 아이템 포함, 정렬은 Pageable로) */
    @EntityGraph(attributePaths = "rewardItem")
    @Query(value = """
            select m from Mail m
            where m.receiver.id = :userId and (m.expiresAt is null or m.expiresAt > :now)
            """, countQuery = """
            select count(m) from Mail m
            where m.receiver.id = :userId and (m.expiresAt is null or m.expiresAt > :now)
            """)
    Page<Mail> findVisible(@Param("userId") Long userId, @Param("now") Instant now, Pageable pageable);

    /** 우편 한 통 (첨부 아이템 포함) */
    @EntityGraph(attributePaths = "rewardItem")
    @Query("select m from Mail m where m.id = :mailId")
    Optional<Mail> findWithItemById(@Param("mailId") Long mailId);

    /** 지금 받을 수 있는 내 우편 (첨부 아이템 포함, 오래된 순) */
    @EntityGraph(attributePaths = "rewardItem")
    @Query("""
            select m from Mail m
            where m.receiver.id = :userId and m.claimedAt is null
              and (m.expiresAt is null or m.expiresAt > :now)
            order by m.id asc
            """)
    List<Mail> findClaimable(@Param("userId") Long userId, @Param("now") Instant now);

    /**
     * 아직 받지 않았고 만료되지 않은 우편만 수령 처리한다 (조건부 UPDATE).
     * 같은 우편을 동시에 여러 번 받으려 해도 한 번만 1이 나온다.
     *
     * @return 수령 처리했으면 1, 이미 받았거나 만료됐으면 0
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Mail m set m.claimedAt = :now
            where m.id = :mailId and m.receiver.id = :userId and m.claimedAt is null
              and (m.expiresAt is null or m.expiresAt > :now)
            """)
    int markClaimed(@Param("mailId") Long mailId, @Param("userId") Long userId, @Param("now") Instant now);

    /** 지정한 사용자들에게 같은 우편을 한 통씩 보낸다 (없는 사용자 ID는 건너뛴다) */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO mails (receiver_id, title, content, reward_coin, reward_item_id, reward_item_quantity,
                               expires_at, created_at)
            SELECT u.id, :title, :content, :rewardCoin, :rewardItemId, :rewardItemQuantity, :expiresAt, :now
            FROM users u
            WHERE u.id IN (:receiverIds)
            """, nativeQuery = true)
    int sendTo(@Param("receiverIds") Collection<Long> receiverIds, @Param("title") String title,
               @Param("content") String content, @Param("rewardCoin") int rewardCoin,
               @Param("rewardItemId") Long rewardItemId, @Param("rewardItemQuantity") int rewardItemQuantity,
               @Param("expiresAt") Instant expiresAt, @Param("now") Instant now);

    /** 모든 사용자에게 같은 우편을 한 통씩 보낸다 */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO mails (receiver_id, title, content, reward_coin, reward_item_id, reward_item_quantity,
                               expires_at, created_at)
            SELECT u.id, :title, :content, :rewardCoin, :rewardItemId, :rewardItemQuantity, :expiresAt, :now
            FROM users u
            """, nativeQuery = true)
    int sendToAll(@Param("title") String title, @Param("content") String content,
                  @Param("rewardCoin") int rewardCoin, @Param("rewardItemId") Long rewardItemId,
                  @Param("rewardItemQuantity") int rewardItemQuantity, @Param("expiresAt") Instant expiresAt,
                  @Param("now") Instant now);
}
