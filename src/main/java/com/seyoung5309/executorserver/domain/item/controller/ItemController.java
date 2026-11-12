package com.seyoung5309.executorserver.domain.item.controller;

import com.seyoung5309.executorserver.domain.item.dto.MyItemResponse;
import com.seyoung5309.executorserver.domain.item.dto.PurchaseRequest;
import com.seyoung5309.executorserver.domain.item.dto.PurchaseResponse;
import com.seyoung5309.executorserver.domain.item.dto.ShopItemResponse;
import com.seyoung5309.executorserver.domain.item.service.ItemService;
import com.seyoung5309.executorserver.global.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "상점", description = "아이템 목록, 구매, 보유 아이템")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @Operation(summary = "상점 아이템 목록",
            description = "판매 중인 아이템만. type(SKIN, THEME, HINT)을 빼면 전체. 각 아이템에 보유 여부와 수량이 붙는다.")
    @GetMapping("/items")
    public List<ShopItemResponse> getShopItems(@LoginUser Long userId,
                                               @RequestParam(required = false) String type) {
        return itemService.getShopItems(userId, ItemService.parseType(type));
    }

    @Operation(summary = "아이템 구매",
            description = "스킨·테마는 quantity를 무시하고 1개, 이미 있으면 409 ALREADY_OWNED. "
                    + "힌트는 1~99개(본문을 빼면 1개). 코인이 모자라면 409 NOT_ENOUGH_COIN.")
    @PostMapping("/items/{itemId}/purchase")
    public PurchaseResponse purchase(@LoginUser Long userId, @PathVariable Long itemId,
                                     @RequestBody(required = false) PurchaseRequest request) {
        return itemService.purchase(userId, itemId, request == null ? null : request.quantity());
    }

    @Operation(summary = "내 보유 아이템",
            description = "type(SKIN, THEME, HINT)을 빼면 전체. 다 쓴 힌트(수량 0)는 빠진다. equipped는 장착 중인 스킨·테마.")
    @GetMapping("/users/me/items")
    public List<MyItemResponse> getMyItems(@LoginUser Long userId,
                                           @RequestParam(required = false) String type) {
        return itemService.getMyItems(userId, ItemService.parseType(type));
    }
}
