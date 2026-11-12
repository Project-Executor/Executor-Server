package com.seyoung5309.executorserver.domain.mail.entity;

import com.seyoung5309.executorserver.domain.auth.entity.User;
import com.seyoung5309.executorserver.domain.item.entity.Item;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.seyoung5309.executorserver.global.jpa.CreatedAtSeconds;

import java.time.Instant;

/** 우편 (시스템 -> 사용자 보상) */
@Entity
@Table(name = "mails")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Mail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private int rewardCoin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_item_id")
    private Item rewardItem;

    @Column(nullable = false)
    private int rewardItemQuantity;

    /** NULL = 미수령 */
    private Instant claimedAt;

    /** NULL = 무기한 */
    private Instant expiresAt;

    @CreatedAtSeconds
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
