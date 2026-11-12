package com.seyoung5309.executorserver.domain.report.repository;

import com.seyoung5309.executorserver.domain.report.entity.Report;
import com.seyoung5309.executorserver.domain.report.entity.ReportStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    /** 내가 한 신고 (카테고리 포함, 정렬은 Pageable로) */
    @EntityGraph(attributePaths = "category")
    Page<Report> findByUserId(Long userId, Pageable pageable);

    /**
     * 관리자용 신고 목록 (카테고리·대상 스테이지 포함, 정렬은 Pageable로).
     *
     * @param status null이면 전체
     */
    @EntityGraph(attributePaths = {"category", "targetStage"})
    @Query(value = "select r from Report r where (:status is null or r.status = :status)",
            countQuery = "select count(r) from Report r where (:status is null or r.status = :status)")
    Page<Report> findForAdmin(@Param("status") ReportStatus status, Pageable pageable);

    /** 상태 변경용: 행 잠금으로 읽어 관리자 두 명이 동시에 바꿔도 순서대로 처리한다 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Report r where r.id = :reportId")
    Optional<Report> findForUpdate(@Param("reportId") Long reportId);
}
