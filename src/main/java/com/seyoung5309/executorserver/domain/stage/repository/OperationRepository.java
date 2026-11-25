package com.seyoung5309.executorserver.domain.stage.repository;

import com.seyoung5309.executorserver.domain.stage.entity.Operation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OperationRepository extends JpaRepository<Operation, Long> {

    /** 작전 전체를 플레이 순서(레이어 순서 -> 레이어 안 작전 순서)대로, 레이어와 함께 */
    @Query("select o from Operation o join fetch o.layer l order by l.sortOrder asc, o.sortOrder asc")
    List<Operation> findAllInPlayOrder();

    /** 게임에 보이는 작전만 (비공개 레이어 = sortOrder 음수 제외), 플레이 순서대로 */
    @Query("select o from Operation o join fetch o.layer l where l.sortOrder >= 0 order by l.sortOrder asc, o.sortOrder asc")
    List<Operation> findAllVisibleInPlayOrder();

    boolean existsByNameAndIdNot(String name, Long id);

    /** 같은 레이어 안에서 순서가 겹치는지 */
    // 주의: Operation에 getLayerId()가 있어서 "ByLayerId"로 쓰면 Spring Data가 없는 필드 layerId로 읽고 실행할 때 실패한다.
    // "Layer_Id"로 layer.id임을 밝힌다.
    boolean existsByLayer_IdAndSortOrderAndIdNot(Long layerId, int sortOrder, Long id);

    boolean existsByLayer_Id(Long layerId);

    long countByLayer_Id(Long layerId);
}
