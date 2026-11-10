package com.seyoung5309.executorserver.domain.guidebook.repository;

import com.seyoung5309.executorserver.domain.guidebook.entity.Guidebook;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuidebookRepository extends JpaRepository<Guidebook, Long> {
}
