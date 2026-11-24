package com.seyoung5309.executorserver.domain.event.repository;

import com.seyoung5309.executorserver.domain.event.entity.EventRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventRecordRepository extends JpaRepository<EventRecord, Long> {

    /** 이름이 이미 있는지 (DB 정렬 규칙상 대소문자를 구분하지 않는다) */
    boolean existsByStageIdAndName(Long stageId, String name);

    long countByStageId(Long stageId);

    /** 적을수록 위 (턴수). 같으면 먼저 올린 기록이 위 */
    @Query("select r from EventRecord r where r.stageId = :stageId order by r.score asc, r.createdAt asc, r.id asc")
    List<EventRecord> findTopAscending(@Param("stageId") Long stageId, Pageable pageable);

    /** 많을수록 위 (타자). 같으면 먼저 올린 기록이 위 */
    @Query("select r from EventRecord r where r.stageId = :stageId order by r.score desc, r.createdAt asc, r.id asc")
    List<EventRecord> findTopDescending(@Param("stageId") Long stageId, Pageable pageable);

    @Query("select count(r) from EventRecord r where r.stageId = :stageId and r.score < :score")
    long countBetterAscending(@Param("stageId") Long stageId, @Param("score") int score);

    @Query("select count(r) from EventRecord r where r.stageId = :stageId and r.score > :score")
    long countBetterDescending(@Param("stageId") Long stageId, @Param("score") int score);

    @Modifying
    @Query("delete from EventRecord r where r.stageId = :stageId")
    int deleteAllByStage(@Param("stageId") Long stageId);
}
