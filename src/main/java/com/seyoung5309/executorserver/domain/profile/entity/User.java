package com.seyoung5309.executorserver.domain.profile.entity;

import com.seyoung5309.executorserver.domain.auth.entity.Account;
import com.seyoung5309.executorserver.domain.item.entity.Item;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 사용자 (프로필·코인·장착 아이템). 로그인 정보는 accounts 테이블(Account)에 있고 1:1이다 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    /** accounts.id와 같은 값 (1:1) */
    @Id
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private Account account;

    /** 닉네임 (중복 불가). 가입 직후에는 NULL이고 튜토리얼에서 정한다. 대사의 {nickname}에도 쓰인다 */
    @Column(unique = true, length = 20)
    private String nickname;
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

    /** 가입 직후의 사용자 (닉네임 없음) */
    public User(Account account) {
        this(account, null);
    }

    public User(Account account, String nickname) {
        this.account = account;
        this.nickname = nickname;
        this.coin = 0;
    }
    public void changeNickname(String nickname) {
        this.nickname = nickname;
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
