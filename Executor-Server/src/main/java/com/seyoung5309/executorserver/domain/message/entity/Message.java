package com.seyoung5309.executorserver.domain.message.entity;

import com.seyoung5309.executorserver.domain.auth.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** 사용자끼리 주고받는 메시지 (시스템 -> 사용자 보상은 우편 mails) */
@Entity
@Table(name = "messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(nullable = false, length = 500)
    private String content;

    /** 받는 사람이 읽었는지 */
    @Column(nullable = false)
    private boolean isRead;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Message(User sender, User receiver, String content, Instant createdAt) {
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.isRead = false;
        this.createdAt = createdAt;
    }
}
