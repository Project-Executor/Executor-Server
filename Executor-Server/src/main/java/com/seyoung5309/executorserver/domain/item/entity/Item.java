package com.seyoung5309.executorserver.domain.item.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    private String image;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemType itemType;

    @Column(nullable = false)
    private int price;

    /** 판매 중 여부 (삭제 대신 false로) */
    @Column(nullable = false)
    private boolean isActive;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** 새 아이템은 판매 중으로 등록된다 */
    public Item(String name, String image, String description, ItemType itemType, int price) {
        update(name, image, description, itemType, price, true);
    }

    /** 판매를 멈출 때는 삭제하지 않고 isActive = false로 바꾼다 */
    public void update(String name, String image, String description, ItemType itemType, int price,
                       boolean isActive) {
        this.name = name;
        this.image = image;
        this.description = description;
        this.itemType = itemType;
        this.price = price;
        this.isActive = isActive;
    }
}
