package com.seyoung5309.executorserver.domain.guidebook.entity;

import com.seyoung5309.executorserver.domain.stage.entity.Stage;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "guidebooks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Guidebook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 처음 등장하는 스테이지 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id")
    private Stage stage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GuidebookCategory category;

    /** 예: NullPointerException */
    @Column(nullable = false, unique = true)
    private String title;

    private String image;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String comment;

    /** 언어 (예: Java). 없으면 null */
    @Column(length = 50)
    private String language;

    /** 코드 예시 (여러 줄). 없으면 null */
    @Column(columnDefinition = "TEXT")
    private String code;

    /** 대처 방법 한 줄 (예: 쓰기 전에 s != null 인지 확인하기). 없으면 null */
    private String remedy;

    /** 맵에서의 모습 (예: 지나갈 수 없는 칸). 없으면 null */
    @Column(name = "map_note")
    private String mapNote;

    public Guidebook(Stage stage, GuidebookCategory category, String title, String image, String comment) {
        update(stage, category, title, image, comment);
    }

    public void update(Stage stage, GuidebookCategory category, String title, String image, String comment) {
        this.stage = stage;
        this.category = category;
        this.title = title;
        this.image = image;
        this.comment = comment;
    }

    /** 도감 상세의 추가 설명 (언어, 코드 예시, 대처, 맵에서). 빈 값은 null로 */
    public void describe(String language, String code, String remedy, String mapNote) {
        this.language = language;
        this.code = code;
        this.remedy = remedy;
        this.mapNote = mapNote;
    }
}
