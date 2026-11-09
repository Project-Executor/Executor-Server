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
}
