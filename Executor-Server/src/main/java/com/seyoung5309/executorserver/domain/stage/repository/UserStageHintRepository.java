package com.seyoung5309.executorserver.domain.stage.repository;

import com.seyoung5309.executorserver.domain.stage.entity.UserStageHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface UserStageHintRepository extends JpaRepository<UserStageHint, Long> {

    /**
     * 힌트를 본 것으로 기록한다. 이미 본 힌트면 아무것도 하지 않는다 (UNIQUE(user_id, stage_id, hint_index)).
     * userId, stageId는 존재하는 값이어야 한다 (INSERT IGNORE는 FK 오류도 무시한다).
     *
     * @return 처음 봤으면 1, 이미 본 힌트면 0
     */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT IGNORE INTO user_stage_hints (user_id, stage_id, hint_index, viewed_at)
            VALUES (:userId, :stageId, :hintIndex, :now)
            """, nativeQuery = true)
    int insertIfAbsent(@Param("userId") Long userId, @Param("stageId") Long stageId,
                       @Param("hintIndex") int hintIndex, @Param("now") Instant now);

    /** 이 스테이지에서 내가 본 힌트 번호 (오름차순) */
    @Query("""
            select h.hintIndex from UserStageHint h
            where h.user.id = :userId and h.stage.id = :stageId
            order by h.hintIndex
            """)
    List<Integer> findHintIndexes(@Param("userId") Long userId, @Param("stageId") Long stageId);

    /** 스테이지의 힌트 열람 기록을 모두 지운다 (사용자 맵을 수정하면 힌트 내용이 바뀔 수 있다) */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from UserStageHint h where h.stage.id = :stageId")
    int deleteByStageId(@Param("stageId") Long stageId);
}
