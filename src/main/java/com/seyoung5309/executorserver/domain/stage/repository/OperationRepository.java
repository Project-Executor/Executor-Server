package com.seyoung5309.executorserver.domain.stage.repository;

import com.seyoung5309.executorserver.domain.stage.entity.Operation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OperationRepository extends JpaRepository<Operation, Long> {

    /** 작전 전체를 플레이 순서(레이어 순서 -> 레이어 안 작전 순서)대로, 레이어와 함께 */
    @Query("select o from Operation o join fetch o.layer l order by l.sortOrder asc, o.sortOrder asc")
    List<Operation> findAllInPlayOrder();

    boolean existsByNameAndIdNot(String name, Long id);

    /** 같은 레이어 안에서 순서가 겹치는지 */
    boolean existsByLayerIdAndSortOrderAndIdNot(Long layerId, int sortOrder, Long id);

    boolean existsByLayerId(Long layerId);

    long countByLayerId(Long layerId);
}
