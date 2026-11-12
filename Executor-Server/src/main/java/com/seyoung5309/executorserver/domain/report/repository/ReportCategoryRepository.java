package com.seyoung5309.executorserver.domain.report.repository;

import com.seyoung5309.executorserver.domain.report.entity.ReportCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportCategoryRepository extends JpaRepository<ReportCategory, Long> {

    List<ReportCategory> findAllByOrderByIdAsc();
}
