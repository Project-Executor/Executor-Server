package com.seyoung5309.executorserver.domain.stage.repository;

import com.seyoung5309.executorserver.domain.stage.entity.UserStage;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserStageRepository extends JpaRepository<UserStage, Long> {

    /** 스테이지별 클리어한 사용자 수 (클리어한 사람이 없는 스테이지는 빠진다) */
    @Query("""
            select new com.seyoung5309.executorserver.domain.stage.repository.StageClearCount(us.stage.id, count(us))
            from UserStage us
            where us.stage.id in :stageIds and us.firstClearedAt is not null
            group by us.stage.id
            """)
    List<StageClearCount> countClears(@Param("stageIds") Collection<Long> stageIds);

    /** 주어진 스테이지 중 내가 클리어한 것 */
    @Query("""
            select us.stage.id from UserStage us
            where us.user.id = :userId and us.stage.id in :stageIds and us.firstClearedAt is not null
            """)
    Set<Long> findClearedStageIdsAmong(@Param("userId") Long userId, @Param("stageIds") Collection<Long> stageIds);

    /** 이 사용자가 이 스테이지를 클리어했는지 (사용자 맵의 verified 판단) */
    boolean existsByUserIdAndStageIdAndFirstClearedAtIsNotNull(Long userId, Long stageId);

    /** 스테이지의 기록을 모두 지운다 (사용자 맵을 수정하면 기존 기록은 의미가 없어진다) */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from UserStage us where us.stage.id = :stageId")
    int deleteByStageId(@Param("stageId") Long stageId);

    Optional<UserStage> findByUserIdAndStageId(Long userId, Long stageId);

    /**
     * 기록 행이 없으면 만들고(도전 1회), 있으면 도전 횟수를 1 늘린다. 원자적으로 실행되고 해당 행을 잠근다.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO user_stages (user_id, stage_id, play_count, last_played_at)
            VALUES (:userId, :stageId, 1, :now)
            ON DUPLICATE KEY UPDATE play_count = play_count + 1, last_played_at = :now
            """, nativeQuery = true)
    void upsertPlay(@Param("userId") Long userId, @Param("stageId") Long stageId, @Param("now") Instant now);

    /** 기록을 행 잠금(SELECT ... FOR UPDATE)과 함께 읽는다. 같은 기록에 대한 동시 요청은 여기서 순서대로 처리된다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select us from UserStage us where us.user.id = :userId and us.stage.id = :stageId")
    Optional<UserStage> findForUpdate(@Param("userId") Long userId, @Param("stageId") Long stageId);

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
     * 스테이지 순위 (클리어한 기록만).
     * 정렬: 최소 턴 오름차순 -> 먼저 클리어한 순 -> 사용자 ID 순 (순위가 항상 하나로 정해지도록).
     */
    @Query("""
            select new com.seyoung5309.executorserver.domain.stage.repository.RankingRow(
                us.user.id, p.nickname, skin.id, us.bestMoves)
            from UserStage us, Profile p
            left join p.equippedSkin skin
            where p.userId = us.user.id
              and us.stage.id = :stageId
              and us.firstClearedAt is not null
            order by us.bestMoves asc, us.firstClearedAt asc, us.user.id asc
            """)
    List<RankingRow> findRanking(@Param("stageId") Long stageId, Pageable pageable);

    /** 지정한 사용자들(나 + 친구) 안에서의 스테이지 순위 */
    @Query("""
            select new com.seyoung5309.executorserver.domain.stage.repository.RankingRow(
                us.user.id, p.nickname, skin.id, us.bestMoves)
            from UserStage us, Profile p
            left join p.equippedSkin skin
            where p.userId = us.user.id
              and us.stage.id = :stageId
              and us.firstClearedAt is not null
              and us.user.id in :userIds
            order by us.bestMoves asc, us.firstClearedAt asc, us.user.id asc
            """)
    List<RankingRow> findRankingAmong(@Param("stageId") Long stageId,
                                      @Param("userIds") Collection<Long> userIds,
                                      Pageable pageable);

    /** 나보다 앞선 기록 수 (전체). 내 순위 = 이 값 + 1 */
    @Query("""
            select count(us) from UserStage us
            where us.stage.id = :stageId
              and us.firstClearedAt is not null
              and (us.bestMoves < :bestMoves
                   or (us.bestMoves = :bestMoves and us.firstClearedAt < :firstClearedAt)
                   or (us.bestMoves = :bestMoves and us.firstClearedAt = :firstClearedAt and us.user.id < :userId))
            """)
    long countAhead(@Param("stageId") Long stageId,
                    @Param("bestMoves") int bestMoves,
                    @Param("firstClearedAt") Instant firstClearedAt,
                    @Param("userId") Long userId);

    /** 나보다 앞선 기록 수 (지정한 사용자들 안에서) */
    @Query("""
            select count(us) from UserStage us
            where us.stage.id = :stageId
              and us.firstClearedAt is not null
              and us.user.id in :userIds
              and (us.bestMoves < :bestMoves
                   or (us.bestMoves = :bestMoves and us.firstClearedAt < :firstClearedAt)
                   or (us.bestMoves = :bestMoves and us.firstClearedAt = :firstClearedAt and us.user.id < :userId))
            """)
    long countAheadAmong(@Param("stageId") Long stageId,
                         @Param("userIds") Collection<Long> userIds,
                         @Param("bestMoves") int bestMoves,
                         @Param("firstClearedAt") Instant firstClearedAt,
                         @Param("userId") Long userId);

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
