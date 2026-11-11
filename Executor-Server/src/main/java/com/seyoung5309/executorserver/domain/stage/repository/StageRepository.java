package com.seyoung5309.executorserver.domain.stage.repository;

import com.seyoung5309.executorserver.domain.stage.entity.Stage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StageRepository extends JpaRepository<Stage, Long> {

    /** 공식 스테이지 전체를 플레이 순서(작전 순서 -> 스테이지 번호)대로. 해금 계산의 기준이 된다. */
    @Query("""
            select s from Stage s join fetch s.operation o
            where s.creator is null
            order by o.sortOrder asc, s.stageNo asc
            """)
    List<Stage> findAllOfficialInPlayOrder();

    boolean existsByOperationId(Long operationId);

    /** 같은 작전에 같은 번호의 스테이지가 있는지 (자기 자신 제외. 새로 만들 때는 excludeId = 0) */
    boolean existsByOperationIdAndStageNoAndIdNot(Long operationId, Integer stageNo, Long excludeId);
}
