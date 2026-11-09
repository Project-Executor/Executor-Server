package com.seyoung5309.executorserver.domain.profile.entity;

import com.seyoung5309.executorserver.domain.auth.entity.User;
import com.seyoung5309.executorserver.domain.item.entity.Item;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Profile {

    /** users.id와 같은 값 (1:1) */
    @Id
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, unique = true, length = 20)
    private String nickname;

    /** 튜토리얼에서 입력하는 집행자 이름 (중복 허용) */
    @Column(length = 20)
    private String characterName;

    private String comment;

    @Column(nullable = false)
    private int coin;

    /** NULL = 기본 스킨 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipped_skin_id")
    private Item equippedSkin;

    /** NULL = 기본 테마 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipped_theme_id")
    private Item equippedTheme;

    public Profile(User user, String nickname) {
        this.user = user;
        this.nickname = nickname;
        this.coin = 0;
    }
}
