package com.seyoung5309.executorserver.domain.item.repository;

import com.seyoung5309.executorserver.domain.item.entity.Item;
import com.seyoung5309.executorserver.domain.item.entity.ItemType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {

    /** 판매 중인 아이템 전체 (아이템 ID 순) */
    List<Item> findAllByIsActiveTrueOrderByIdAsc();

    /** 판매 중인 아이템 중 한 종류 (아이템 ID 순) */
    List<Item> findAllByItemTypeAndIsActiveTrueOrderByIdAsc(ItemType itemType);

    /** 판매 중인 아이템 한 건. 판매가 중단된 아이템이면 비어 있다. */
    Optional<Item> findByIdAndIsActiveTrue(Long id);
}
