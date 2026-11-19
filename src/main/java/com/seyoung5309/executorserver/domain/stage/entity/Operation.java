package com.seyoung5309.executorserver.domain.stage.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 작전 (스테이지 상위 분류, 스토리 소주제). 반드시 하나의 레이어(대주제) 안에 있다 */
@Entity
@Table(name = "operations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Operation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "layer_id", nullable = false)
    private Layer layer;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    private String description;

    /** 레이어 안에서의 표시 순서 (튜토리얼 = 0). 같은 레이어 안에서 겹칠 수 없다 */
    @Column(nullable = false)
    private int sortOrder;

    public Operation(Layer layer, String name, String description, int sortOrder) {
        update(layer, name, description, sortOrder);
    }

    public void update(Layer layer, String name, String description, int sortOrder) {
        this.layer = layer;
        this.name = name;
        this.description = description;
        this.sortOrder = sortOrder;
    }

    /** 레이어 ID. 프록시를 초기화하지 않고 ID만 꺼낸다 */
    public Long getLayerId() {
        return layer == null ? null : layer.getId();
    }
}
