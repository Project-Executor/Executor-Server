package com.seyoung5309.executorserver.domain.item.service;

import com.seyoung5309.executorserver.domain.item.dto.MyItemResponse;
import com.seyoung5309.executorserver.domain.item.dto.PurchaseResponse;
import com.seyoung5309.executorserver.domain.item.dto.ShopItemResponse;
import com.seyoung5309.executorserver.domain.item.entity.Item;
import com.seyoung5309.executorserver.domain.item.entity.ItemType;
import com.seyoung5309.executorserver.domain.item.repository.ItemRepository;
import com.seyoung5309.executorserver.domain.item.repository.OwnedItemRow;
import com.seyoung5309.executorserver.domain.item.repository.UserItemRepository;
import com.seyoung5309.executorserver.domain.profile.entity.Profile;
import com.seyoung5309.executorserver.domain.profile.repository.ProfileRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {

    static final int MAX_HINT_PURCHASE = 99;

    private final ItemRepository itemRepository;
    private final UserItemRepository userItemRepository;
    private final ProfileRepository profileRepository;
    private final Clock clock;

    /**
     * 판매 중인 아이템 목록 (아이템 ID 순). 각 아이템에 내 보유 여부·수량을 붙인다.
     *
     * @param itemType null이면 전체
     */
    public List<ShopItemResponse> getShopItems(Long userId, ItemType itemType) {
        List<Item> items = itemType == null
                ? itemRepository.findAllByIsActiveTrueOrderByIdAsc()
                : itemRepository.findAllByItemTypeAndIsActiveTrueOrderByIdAsc(itemType);
        Map<Long, Integer> owned = userItemRepository.findOwnedRows(userId).stream()
                .collect(Collectors.toMap(OwnedItemRow::itemId, OwnedItemRow::quantity));

        return items.stream()
                .map(item -> ShopItemResponse.of(item, owned.getOrDefault(item.getId(), 0)))
                .toList();
    }

    /**
     * 코인으로 아이템을 산다. 코인 차감과 지급은 한 트랜잭션이라, 지급에 실패하면 차감도 되돌려진다.
     * <p>
     * 같은 사용자의 구매가 동시에 여러 건 들어와도 코인 차감 UPDATE가 프로필 행을 잠그므로 순서대로 처리된다.
     * 그래서 코인은 음수가 되지 않고, 스킨·테마는 한 번만 지급된다.
     *
     * @param requestedQuantity null이면 1. 스킨·테마는 무시하고 1개
     * @throws BusinessException NOT_FOUND(없거나 판매 중단), INVALID_INPUT(힌트 수량 범위), NOT_ENOUGH_COIN, ALREADY_OWNED
     */
    @Transactional
    public PurchaseResponse purchase(Long userId, Long itemId, Integer requestedQuantity) {
        Item item = itemRepository.findByIdAndIsActiveTrue(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "판매 중인 아이템을 찾을 수 없습니다."));
        boolean stackable = item.getItemType() == ItemType.HINT;
        int quantity = stackable ? validateHintQuantity(requestedQuantity) : 1;

        // 스킨·테마를 이미 가졌으면 코인을 쓰기 전에 알려 준다 (코인이 모자라도 ALREADY_OWNED가 먼저).
        // 동시에 들어온 요청은 아래 insertIfAbsent에서 한 번 더 막는다.
        if (!stackable && isOwned(userId, itemId)) {
            throw alreadyOwned();
        }

        int cost = totalCost(item.getPrice(), quantity);
        if (profileRepository.spendCoin(userId, cost) == 0) {
            throw new BusinessException(ErrorCode.NOT_ENOUGH_COIN, "코인이 부족합니다. (필요: " + cost + "코인)");
        }

        if (stackable) {
            userItemRepository.addQuantity(userId, itemId, quantity, clock.instant());
        } else if (userItemRepository.insertIfAbsent(userId, itemId, clock.instant()) == 0) {
            throw alreadyOwned(); // 예외로 트랜잭션이 롤백되어 위에서 뺀 코인도 되돌아온다
        }

        int ownedQuantity = userItemRepository.findQuantity(userId, itemId).orElseThrow();
        int remainingCoin = profileRepository.findCoinByUserId(userId).orElseThrow();
        return new PurchaseResponse(itemId, ownedQuantity, cost, remainingCoin);
    }

    /**
     * 내 보유 아이템 (아이템 ID 순). 다 쓴 힌트(수량 0)는 빠진다.
     *
     * @param itemType null이면 전체
     */
    public List<MyItemResponse> getMyItems(Long userId, ItemType itemType) {
        Profile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "프로필을 찾을 수 없습니다."));
        Long skinId = profile.getEquippedSkinId();
        Long themeId = profile.getEquippedThemeId();

        return userItemRepository.findMyItems(userId, itemType).stream()
                .map(userItem -> {
                    Long id = userItem.getItem().getId();
                    return MyItemResponse.of(userItem, Objects.equals(id, skinId) || Objects.equals(id, themeId));
                })
                .toList();
    }

    /** 종류 문자열을 읽는다 (대소문자 무시). 비어 있으면 null(전체), 잘못된 값이면 INVALID_INPUT */
    public static ItemType parseType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ItemType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "type은 SKIN, THEME, HINT 중 하나여야 합니다.");
        }
    }

    private boolean isOwned(Long userId, Long itemId) {
        return userItemRepository.findQuantity(userId, itemId).map(q -> q > 0).orElse(false);
    }

    private static int validateHintQuantity(Integer requested) {
        int quantity = requested == null ? 1 : requested;
        if (quantity < 1 || quantity > MAX_HINT_PURCHASE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "힌트는 한 번에 1~" + MAX_HINT_PURCHASE + "개까지 살 수 있습니다.");
        }
        return quantity;
    }

    /** 가격 × 수량. int 범위를 넘으면 그만큼 코인을 가질 수 없으므로 코인 부족으로 처리한다. */
    private static int totalCost(int price, int quantity) {
        long cost = (long) price * quantity;
        if (cost > Integer.MAX_VALUE) {
            throw new BusinessException(ErrorCode.NOT_ENOUGH_COIN, "코인이 부족합니다.");
        }
        return (int) cost;
    }

    private static BusinessException alreadyOwned() {
        return new BusinessException(ErrorCode.ALREADY_OWNED, "이미 가지고 있는 아이템입니다.");
    }
}
