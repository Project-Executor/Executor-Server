package com.seyoung5309.executorserver.domain.stage.repository;

import com.seyoung5309.executorserver.domain.stage.entity.Operation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OperationRepository extends JpaRepository<Operation, Long> {

    List<Operation> findAllByOrderBySortOrderAsc();

    boolean existsByNameAndIdNot(String name, Long id);

    boolean existsBySortOrderAndIdNot(int sortOrder, Long id);
}
