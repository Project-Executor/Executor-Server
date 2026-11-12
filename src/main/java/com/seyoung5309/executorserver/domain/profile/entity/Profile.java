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

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public void changeCharacterName(String characterName) {
        this.characterName = characterName;
    }

    /** null이면 자기소개를 지운다 */
    public void changeComment(String comment) {
        this.comment = comment;
    }

    /** null이면 기본 스킨으로 돌아간다. 보유 여부·종류 검사는 서비스에서 한다. */
    public void equipSkin(Item skin) {
        this.equippedSkin = skin;
    }

    /** null이면 기본 테마로 돌아간다. 보유 여부·종류 검사는 서비스에서 한다. */
    public void equipTheme(Item theme) {
        this.equippedTheme = theme;
    }

    /** 장착 스킨 ID. 프록시를 초기화하지 않고 ID만 꺼낸다. */
    public Long getEquippedSkinId() {
        return equippedSkin == null ? null : equippedSkin.getId();
    }

    public Long getEquippedThemeId() {
        return equippedTheme == null ? null : equippedTheme.getId();
    }
}
