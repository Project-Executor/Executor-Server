package com.seyoung5309.executorserver.domain.notice.repository;

import com.seyoung5309.executorserver.domain.notice.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    /** 게임에 보이는 공지 (상단 고정 먼저, 그다음 게시 시각 최신순). Notice.isShownAt과 같은 조건 */
    @Query(value = """
            select n from Notice n
            where n.isVisible = true
              and n.publishedAt <= :now
              and (n.endsAt is null or n.endsAt > :now)
            order by n.isPinned desc, n.publishedAt desc, n.id desc
            """,
            countQuery = """
            select count(n) from Notice n
            where n.isVisible = true
              and n.publishedAt <= :now
              and (n.endsAt is null or n.endsAt > :now)
            """)
    Page<Notice> findShown(@Param("now") Instant now, Pageable pageable);
}
