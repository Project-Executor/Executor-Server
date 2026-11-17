package com.seyoung5309.executorserver.domain.notice.entity;

import com.seyoung5309.executorserver.global.jpa.CreatedAtSeconds;
import com.seyoung5309.executorserver.global.jpa.UpdatedAtSeconds;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 공지사항. 게임에는 숨기지 않았고(isVisible) 게시 기간(publishedAt ~ endsAt) 안인 공지만 보인다.
 */
@Entity
@Table(name = "notices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NoticeCategory category;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 상단 고정 */
    @Column(nullable = false)
    private boolean isPinned;

    /** false면 숨김 */
    @Column(nullable = false)
    private boolean isVisible;

    /** 게시 시각. 미래면 예약 게시 */
    @Column(nullable = false)
    private Instant publishedAt;

    /** 게시 종료 시각. null이면 계속 */
    private Instant endsAt;

    @CreatedAtSeconds
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdatedAtSeconds
    @Column(nullable = false)
    private Instant updatedAt;

    public Notice(NoticeCategory category, String title, String content, boolean isPinned, boolean isVisible,
                  Instant publishedAt, Instant endsAt) {
        update(category, title, content, isPinned, isVisible, publishedAt, endsAt);
    }

    public void update(NoticeCategory category, String title, String content, boolean isPinned, boolean isVisible,
                       Instant publishedAt, Instant endsAt) {
        this.category = category;
        this.title = title;
        this.content = content;
        this.isPinned = isPinned;
        this.isVisible = isVisible;
        this.publishedAt = publishedAt;
        this.endsAt = endsAt;
    }

    /** now 시점에 게임에 보이는지 */
    public boolean isShownAt(Instant now) {
        return isVisible && !publishedAt.isAfter(now) && (endsAt == null || endsAt.isAfter(now));
    }
}
