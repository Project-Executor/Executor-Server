package com.seyoung5309.executorserver.domain.stage.repository;

import com.seyoung5309.executorserver.domain.stage.entity.UserStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserStageRepository extends JpaRepository<UserStage, Long> {

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
