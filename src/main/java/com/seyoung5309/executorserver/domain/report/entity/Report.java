package com.seyoung5309.executorserver.domain.report.entity;

import com.seyoung5309.executorserver.domain.auth.entity.User;
import com.seyoung5309.executorserver.domain.stage.entity.Stage;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 신고자 (탈퇴 시 NULL) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ReportCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id")
    private User targetUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_stage_id")
    private Stage targetStage;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String comment;

    /** DB는 VARCHAR(20) + CHECK 이므로 VARCHAR로 매핑 */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** DONE이 된 시각 */
    private Instant handledAt;

    /** 접수 상태(RECEIVED)로 만든다. 대상 규칙(카테고리별 필수 여부 등)은 서비스에서 검사한다 */
    public Report(User user, ReportCategory category, User targetUser, Stage targetStage,
                  String title, String comment, Instant now) {
        this.user = user;
        this.category = category;
        this.targetUser = targetUser;
        this.targetStage = targetStage;
        this.title = title;
        this.comment = comment;
        this.status = ReportStatus.RECEIVED;
        this.createdAt = now;
    }

    /**
     * 상태를 바꾼다. 앞으로만 진행한다 (RECEIVED -> IN_PROGRESS -> DONE, RECEIVED -> DONE 가능).
     * 같은 상태로 바꾸면 아무것도 하지 않는다.
     *
     * @return 되돌리려 하면 false
     */
    public boolean changeStatus(ReportStatus next, Instant now) {
        if (next == status) {
            return true;
        }
        if (next.ordinal() < status.ordinal()) {
            return false;
        }
        this.status = next;
        if (next == ReportStatus.DONE) {
            this.handledAt = now;
        }
        return true;
    }
}
