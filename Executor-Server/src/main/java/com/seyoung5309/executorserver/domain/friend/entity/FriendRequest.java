package com.seyoung5309.executorserver.domain.friend.entity;

import com.seyoung5309.executorserver.domain.auth.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * 친구 요청. (sender, receiver) 쌍마다 한 행만 있다.
 * 거절된 요청을 다시 보내면 새 행을 만들지 않고 같은 행을 PENDING으로 되돌린다.
 */
@Entity
@Table(name = "friend_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FriendRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    /** DB는 VARCHAR(20) + CHECK 이므로 VARCHAR로 매핑 */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private FriendRequestStatus status;

    /** 요청한 시각. 거절된 요청을 다시 보내면 그 시각으로 바뀐다 */
    @Column(nullable = false)
    private Instant createdAt;

    private Instant respondedAt;

    public static FriendRequest pending(User sender, User receiver, Instant now) {
        FriendRequest request = new FriendRequest();
        request.sender = sender;
        request.receiver = receiver;
        request.resend(now);
        return request;
    }

    /** 거절됐던(또는 예전에 수락됐던) 요청을 다시 대기 상태로 */
    public void resend(Instant now) {
        this.status = FriendRequestStatus.PENDING;
        this.createdAt = now;
        this.respondedAt = null;
    }

    public void accept(Instant now) {
        this.status = FriendRequestStatus.ACCEPTED;
        this.respondedAt = now;
    }

    public void reject(Instant now) {
        this.status = FriendRequestStatus.REJECTED;
        this.respondedAt = now;
    }

    public boolean isPending() {
        return status == FriendRequestStatus.PENDING;
    }
}
