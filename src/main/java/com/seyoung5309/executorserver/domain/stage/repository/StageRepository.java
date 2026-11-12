package com.seyoung5309.executorserver.domain.stage.repository;

import com.seyoung5309.executorserver.domain.stage.entity.Stage;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StageRepository extends JpaRepository<Stage, Long> {

    /** 공식 스테이지 전체를 플레이 순서(작전 순서 -> 스테이지 번호)대로. 해금 계산의 기준이 된다. */
    @Query("""
            select s from Stage s join fetch s.operation o
            where s.creator is null
            order by o.sortOrder asc, s.stageNo asc
            """)
    List<Stage> findAllOfficialInPlayOrder();

    /** 공개된 사용자 맵 (정렬은 Pageable로) */
    @Query(value = "select s from Stage s where s.creator is not null and s.isPublic = true",
            countQuery = "select count(s) from Stage s where s.creator is not null and s.isPublic = true")
    Page<Stage> findPublicUserMaps(Pageable pageable);

    /** 내가 만든 맵 전체 (최근 수정 순) */
    @Query("select s from Stage s where s.creator.id = :creatorId order by s.updatedAt desc, s.id desc")
    List<Stage> findUserMapsByCreator(@Param("creatorId") Long creatorId);

    /** 사용자 맵 수정·공개 전환·삭제용: 행 잠금으로 읽어 같은 맵에 대한 요청을 순서대로 처리한다 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "creator")
    @Query("select s from Stage s where s.id = :stageId")
    Optional<Stage> findForUpdate(@Param("stageId") Long stageId);

    boolean existsByOperationId(Long operationId);

    /** 같은 작전에 같은 번호의 스테이지가 있는지 (자기 자신 제외. 새로 만들 때는 excludeId = 0) */
    boolean existsByOperationIdAndStageNoAndIdNot(Long operationId, Integer stageNo, Long excludeId);
}
