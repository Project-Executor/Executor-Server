package com.seyoung5309.executorserver.domain.guestbook.repository;

import com.seyoung5309.executorserver.domain.guestbook.entity.GuestbookEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface GuestbookRepository extends JpaRepository<GuestbookEntry, Long> {

    /** 최신 글부터 (같은 시각이면 나중에 들어온 글이 먼저) */
    @Query("select g from GuestbookEntry g order by g.createdAt desc, g.id desc")
    List<GuestbookEntry> findLatest(Pageable pageable);

    /** 관리자 목록 (최신순, 이름·내용 검색) */
    @Query("""
            select g from GuestbookEntry g
            where :q is null or g.name like concat('%', :q, '%') or g.message like concat('%', :q, '%')
            order by g.createdAt desc, g.id desc
            """)
    Page<GuestbookEntry> search(@Param("q") String q, Pageable pageable);

    long countByCreatedAtGreaterThanEqual(Instant from);

    /** 같은 기기에서 이 시각 이후에 쓴 글이 있는지 (연속 작성 제한) */
    boolean existsByDeviceIdAndCreatedAtGreaterThanEqual(String deviceId, Instant from);

    /** 이 사용자가 이 시각 이후에 쓴 글이 있는지 (하루 첫 글 보상) */
    boolean existsByUserIdAndCreatedAtGreaterThanEqual(Long userId, Instant from);
}
