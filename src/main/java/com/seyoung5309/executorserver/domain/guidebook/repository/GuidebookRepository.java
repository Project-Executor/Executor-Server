package com.seyoung5309.executorserver.domain.guidebook.repository;

import com.seyoung5309.executorserver.domain.guidebook.entity.Guidebook;
import com.seyoung5309.executorserver.domain.guidebook.entity.GuidebookCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuidebookRepository extends JpaRepository<Guidebook, Long> {

    List<Guidebook> findAllByOrderByIdAsc();

    List<Guidebook> findAllByCategoryOrderByIdAsc(GuidebookCategory category);

    boolean existsByTitleAndIdNot(String title, Long id);
}
