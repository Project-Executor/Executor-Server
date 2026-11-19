package com.seyoung5309.executorserver.domain.stage.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 레이어 (스토리 대주제). 작전(Operation, 소주제)을 묶는다.
 * 플레이 순서는 레이어 sortOrder -> 작전 sortOrder(레이어 안) -> 스테이지 번호. 튜토리얼 레이어가 0이다.
 */
@Entity
@Table(name = "layers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Layer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    private String description;

    /** 표시·플레이 순서 (튜토리얼 = 0). 레이어끼리 겹칠 수 없다 */
    @Column(nullable = false, unique = true)
    private int sortOrder;

    public Layer(String name, String description, int sortOrder) {
        update(name, description, sortOrder);
    }

    public void update(String name, String description, int sortOrder) {
        this.name = name;
        this.description = description;
        this.sortOrder = sortOrder;
    }
}
