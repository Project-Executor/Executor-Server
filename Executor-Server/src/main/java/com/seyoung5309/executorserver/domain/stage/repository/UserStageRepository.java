package com.seyoung5309.executorserver.domain.stage.repository;

import com.seyoung5309.executorserver.domain.stage.entity.UserStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserStageRepository extends JpaRepository<UserStage, Long> {

    Optional<UserStage> findByUserIdAndStageId(Long userId, Long stageId);

    /** 사용자가 클리어한 공식 스테이지 ID (해금 계산용) */
    @Query("""
            select s.id from UserStage us join us.stage s
            where us.user.id = :userId and s.creator is null and us.firstClearedAt is not null
            """)
    Set<Long> findClearedOfficialStageIds(@Param("userId") Long userId);

    /** 사용자의 공식 스테이지 기록 전체 */
    @Query("""
            select us from UserStage us join us.stage s
            where us.user.id = :userId and s.creator is null
            """)
    List<UserStage> findAllOfficialByUserId(@Param("userId") Long userId);

    /**
     * 공식 스테이지 기준 클리어 수와 별 개수.
     * 별 1 = 클리어, 별 2 = bestMoves <= parMoves, 별 3 = 숨겨진 요소. 튜토리얼은 별 1만 센다.
     */
    @Query("""
            select new com.seyoung5309.executorserver.domain.stage.repository.StarSummary(
                count(us),
                coalesce(sum(case when s.stageType <> com.seyoung5309.executorserver.domain.stage.entity.StageType.TUTORIAL
                                   and us.bestMoves <= s.parMoves then 1 else 0 end), 0),
                coalesce(sum(case when s.stageType <> com.seyoung5309.executorserver.domain.stage.entity.StageType.TUTORIAL
                                   and us.isHiddenFound = true then 1 else 0 end), 0)
            )
            from UserStage us join us.stage s
            where us.user.id = :userId
              and us.firstClearedAt is not null
              and s.creator is null
            """)
    StarSummary summarizeOfficialStars(@Param("userId") Long userId);
}
