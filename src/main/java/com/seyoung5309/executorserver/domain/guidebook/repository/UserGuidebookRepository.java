package com.seyoung5309.executorserver.domain.guidebook.repository;

import com.seyoung5309.executorserver.domain.guidebook.entity.UserGuidebook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserGuidebookRepository extends JpaRepository<UserGuidebook, Long> {

    @Query("""
            select new com.seyoung5309.executorserver.domain.guidebook.repository.UnlockedGuidebookRow(
                ug.guidebook.id, ug.unlockedAt)
            from UserGuidebook ug
            where ug.user.id = :userId
            """)
    List<UnlockedGuidebookRow> findUnlockedByUserId(@Param("userId") Long userId);

    Optional<UserGuidebook> findByUserIdAndGuidebookId(Long userId, Long guidebookId);

    /**
     * 도감을 해금한다. 이미 해금됐으면 아무것도 하지 않는다 (UNIQUE(user_id, guidebook_id)).
     * guidebookId는 존재하는 값이어야 한다 (INSERT IGNORE는 FK 오류도 무시하므로 호출 전에 확인할 것).
     *
     * @return 새로 해금했으면 1, 이미 해금돼 있었으면 0
     */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT IGNORE INTO user_guidebooks (user_id, guidebook_id, unlocked_at)
            VALUES (:userId, :guidebookId, :now)
            """, nativeQuery = true)
    int unlock(@Param("userId") Long userId, @Param("guidebookId") Long guidebookId, @Param("now") Instant now);
}
