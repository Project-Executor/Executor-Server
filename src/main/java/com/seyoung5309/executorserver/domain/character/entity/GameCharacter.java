package com.seyoung5309.executorserver.domain.character.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대화·이야기에 나오는 인물. 대사의 speaker(이름)나 portrait("인물ID/표정")로 찾는다.
 * 게임은 이름표 바탕색을 tagColor로 칠하고, 일러스트는 Resources/Portraits/{characterKey}/{표정}에서 찾는다.
 * protagonist면 대화창 왼쪽(주인공 자리)에 선다.
 */
@Entity
@Table(name = "characters")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 일러스트 폴더 ID (영문 소문자·숫자·-·_, 예: cassia) */
    @Column(name = "character_key", nullable = false, unique = true, length = 50)
    private String characterKey;

    /** 대사의 speaker와 같은 이름 (예: 카시아) */
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    /** 이름표 바탕색 #RRGGBB (대문자) */
    @Column(name = "tag_color", nullable = false, length = 7)
    private String tagColor;

    @Column(nullable = false)
    private boolean protagonist;

    public GameCharacter(String characterKey, String name, String tagColor, boolean protagonist) {
        update(characterKey, name, tagColor, protagonist);
    }

    public void update(String characterKey, String name, String tagColor, boolean protagonist) {
        this.characterKey = characterKey;
        this.name = name;
        this.tagColor = tagColor;
        this.protagonist = protagonist;
    }
}
