package com.seyoung5309.executorserver.domain.stage.repository;

import com.seyoung5309.executorserver.domain.stage.entity.Layer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LayerRepository extends JpaRepository<Layer, Long> {

    List<Layer> findAllByOrderBySortOrderAsc();

    boolean existsByNameAndIdNot(String name, Long id);

    boolean existsBySortOrderAndIdNot(int sortOrder, Long id);
}
