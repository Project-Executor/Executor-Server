package com.seyoung5309.executorserver.domain.stage.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 작전 (스테이지 상위 분류) */
@Entity
@Table(name = "operations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Operation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    private String description;

    /** 표시 순서 (튜토리얼 = 0) */
    @Column(nullable = false, unique = true)
    private int sortOrder;
}
