package com.seyoung5309.executorserver.domain.item.service;

import com.seyoung5309.executorserver.domain.item.dto.MyItemResponse;
import com.seyoung5309.executorserver.domain.item.dto.PurchaseResponse;
import com.seyoung5309.executorserver.domain.item.dto.ShopItemResponse;
import com.seyoung5309.executorserver.domain.item.entity.Item;
import com.seyoung5309.executorserver.domain.item.entity.ItemType;
import com.seyoung5309.executorserver.domain.item.entity.UserItem;
import com.seyoung5309.executorserver.domain.item.repository.ItemRepository;
import com.seyoung5309.executorserver.domain.item.repository.OwnedItemRow;
import com.seyoung5309.executorserver.domain.item.repository.UserItemRepository;
import com.seyoung5309.executorserver.domain.profile.entity.Profile;
import com.seyoung5309.executorserver.domain.profile.repository.ProfileRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static com.seyoung5309.executorserver.domain.stage.StageFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ItemServiceTest {

    private static final Long USER_ID = 1L;
    private static final Instant NOW = Instant.parse("2026-11-10T03:00:00Z");

    private ItemRepository itemRepository;
    private UserItemRepository userItemRepository;
    private ProfileRepository profileRepository;
    private ItemService service;

    private final Item skin = item(3L, ItemType.SKIN, "다크 모드 집행자", 300);
    private final Item theme = item(5L, ItemType.THEME, "터미널 테마", 200);
    private final Item hint = item(9L, ItemType.HINT, "힌트 로그", 30);

    @BeforeEach
    void setUp() {
        itemRepository = mock(ItemRepository.class);
        userItemRepository = mock(UserItemRepository.class);
        profileRepository = mock(ProfileRepository.class);
        service = new ItemService(itemRepository, userItemRepository, profileRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    // ---- 상점 목록 ----

    @Test
    void 상점_목록에_보유_여부와_수량이_붙는다() {
        when(itemRepository.findAllByIsActiveTrueOrderByIdAsc()).thenReturn(List.of(skin, theme, hint));
        when(userItemRepository.findOwnedRows(USER_ID))
                .thenReturn(List.of(new OwnedItemRow(3L, 1), new OwnedItemRow(9L, 2)));

        List<ShopItemResponse> items = service.getShopItems(USER_ID, null);

        assertThat(items).extracting(ShopItemResponse::itemId, ShopItemResponse::owned, ShopItemResponse::quantity)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(3L, true, 1),
                        org.assertj.core.groups.Tuple.tuple(5L, false, 0),
                        org.assertj.core.groups.Tuple.tuple(9L, true, 2));
    }

    @Test
    void 종류를_지정하면_그_종류만_조회한다() {
        when(itemRepository.findAllByItemTypeAndIsActiveTrueOrderByIdAsc(ItemType.HINT)).thenReturn(List.of(hint));

        assertThat(service.getShopItems(USER_ID, ItemType.HINT)).extracting(ShopItemResponse::itemId)
                .containsExactly(9L);
        verify(itemRepository, never()).findAllByIsActiveTrueOrderByIdAsc();
    }

    // ---- 구매: 스킨·테마 ----

    @Test
    void 스킨을_사면_코인이_빠지고_1개_지급된다() {
        givenActive(skin);
        when(userItemRepository.findQuantity(USER_ID, 3L)).thenReturn(Optional.empty(), Optional.of(1));
        when(profileRepository.spendCoin(USER_ID, 300)).thenReturn(1);
        when(userItemRepository.insertIfAbsent(USER_ID, 3L, NOW)).thenReturn(1);
        when(profileRepository.findCoinByUserId(USER_ID)).thenReturn(Optional.of(200));

        PurchaseResponse response = service.purchase(USER_ID, 3L, 5);

        assertThat(response).isEqualTo(new PurchaseResponse(3L, 1, 300, 200));
        verify(userItemRepository, never()).addQuantity(anyLong(), anyLong(), anyInt(), any());
    }

    @Test
    void 이미_가진_스킨은_코인을_쓰기_전에_ALREADY_OWNED() {
        givenActive(theme);
        when(userItemRepository.findQuantity(USER_ID, 5L)).thenReturn(Optional.of(1));

        assertErrorCode(() -> service.purchase(USER_ID, 5L, null), ErrorCode.ALREADY_OWNED);
        verify(profileRepository, never()).spendCoin(anyLong(), anyInt());
    }

    @Test
    void 동시_요청으로_지급이_0건이면_ALREADY_OWNED로_롤백된다() {
        givenActive(skin);
        when(userItemRepository.findQuantity(USER_ID, 3L)).thenReturn(Optional.empty());
        when(profileRepository.spendCoin(USER_ID, 300)).thenReturn(1);
        when(userItemRepository.insertIfAbsent(USER_ID, 3L, NOW)).thenReturn(0);

        assertErrorCode(() -> service.purchase(USER_ID, 3L, null), ErrorCode.ALREADY_OWNED);
    }

    @Test
    void 코인이_모자라면_NOT_ENOUGH_COIN이고_지급하지_않는다() {
        givenActive(skin);
        when(userItemRepository.findQuantity(USER_ID, 3L)).thenReturn(Optional.empty());
        when(profileRepository.spendCoin(USER_ID, 300)).thenReturn(0);

        assertErrorCode(() -> service.purchase(USER_ID, 3L, null), ErrorCode.NOT_ENOUGH_COIN);
        verify(userItemRepository, never()).insertIfAbsent(anyLong(), anyLong(), any());
    }

    // ---- 구매: 힌트 ----

    @Test
    void 힌트는_수량만큼_코인이_빠지고_보유_수량에_더해진다() {
        givenActive(hint);
        when(profileRepository.spendCoin(USER_ID, 90)).thenReturn(1);
        when(userItemRepository.findQuantity(USER_ID, 9L)).thenReturn(Optional.of(5));
        when(profileRepository.findCoinByUserId(USER_ID)).thenReturn(Optional.of(310));

        PurchaseResponse response = service.purchase(USER_ID, 9L, 3);

        assertThat(response).isEqualTo(new PurchaseResponse(9L, 5, 90, 310));
        verify(userItemRepository).addQuantity(USER_ID, 9L, 3, NOW);
    }

    @Test
    void 힌트_수량을_빼면_1개() {
        givenActive(hint);
        when(profileRepository.spendCoin(USER_ID, 30)).thenReturn(1);
        when(userItemRepository.findQuantity(USER_ID, 9L)).thenReturn(Optional.of(1));
        when(profileRepository.findCoinByUserId(USER_ID)).thenReturn(Optional.of(0));

        assertThat(service.purchase(USER_ID, 9L, null).spentCoin()).isEqualTo(30);
        verify(userItemRepository).addQuantity(USER_ID, 9L, 1, NOW);
    }

    @Test
    void 힌트_수량이_1에서_99를_벗어나면_INVALID_INPUT() {
        givenActive(hint);

        assertErrorCode(() -> service.purchase(USER_ID, 9L, 0), ErrorCode.INVALID_INPUT);
        assertErrorCode(() -> service.purchase(USER_ID, 9L, 100), ErrorCode.INVALID_INPUT);
        assertErrorCode(() -> service.purchase(USER_ID, 9L, -1), ErrorCode.INVALID_INPUT);
        verify(profileRepository, never()).spendCoin(anyLong(), anyInt());
    }

    @Test
    void 가격_곱하기_수량이_int를_넘으면_NOT_ENOUGH_COIN() {
        Item expensive = item(10L, ItemType.HINT, "비싼 힌트", Integer.MAX_VALUE);
        givenActive(expensive);

        assertErrorCode(() -> service.purchase(USER_ID, 10L, 2), ErrorCode.NOT_ENOUGH_COIN);
        verify(profileRepository, never()).spendCoin(anyLong(), anyInt());
    }

    @Test
    void 없거나_판매_중단된_아이템은_NOT_FOUND() {
        when(itemRepository.findByIdAndIsActiveTrue(99L)).thenReturn(Optional.empty());

        assertErrorCode(() -> service.purchase(USER_ID, 99L, null), ErrorCode.NOT_FOUND);
    }

    // ---- 내 보유 아이템 ----

    @Test
    void 보유_아이템에_장착_여부가_표시된다() {
        Profile profile = new Profile(user(USER_ID), "debugger");
        profile.equipSkin(skin);
        when(profileRepository.findById(USER_ID)).thenReturn(Optional.of(profile));
        when(userItemRepository.findMyItems(USER_ID, null)).thenReturn(List.of(
                userItem(skin, 1), userItem(theme, 1), userItem(hint, 4)));

        List<MyItemResponse> items = service.getMyItems(USER_ID, null);

        assertThat(items).extracting(MyItemResponse::itemId, MyItemResponse::equipped, MyItemResponse::quantity)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(3L, true, 1),
                        org.assertj.core.groups.Tuple.tuple(5L, false, 1),
                        org.assertj.core.groups.Tuple.tuple(9L, false, 4));
        assertThat(items.get(0).acquiredAt()).isEqualTo(NOW);
    }

    @Test
    void 종류_문자열은_대소문자를_가리지_않고_비어_있으면_전체() {
        assertThat(ItemService.parseType("hint")).isEqualTo(ItemType.HINT);
        assertThat(ItemService.parseType(" Skin ")).isEqualTo(ItemType.SKIN);
        assertThat(ItemService.parseType(null)).isNull();
        assertThat(ItemService.parseType("")).isNull();
        assertErrorCode(() -> ItemService.parseType("weapon"), ErrorCode.INVALID_INPUT);
    }

    private void givenActive(Item item) {
        when(itemRepository.findByIdAndIsActiveTrue(item.getId())).thenReturn(Optional.of(item));
    }

    private static Item item(Long id, ItemType type, String name, int price) {
        Item item = BeanUtils.instantiateClass(Item.class);
        ReflectionTestUtils.setField(item, "id", id);
        ReflectionTestUtils.setField(item, "itemType", type);
        ReflectionTestUtils.setField(item, "name", name);
        ReflectionTestUtils.setField(item, "price", price);
        ReflectionTestUtils.setField(item, "isActive", true);
        return item;
    }

    private static UserItem userItem(Item item, int quantity) {
        UserItem userItem = BeanUtils.instantiateClass(UserItem.class);
        ReflectionTestUtils.setField(userItem, "item", item);
        ReflectionTestUtils.setField(userItem, "quantity", quantity);
        ReflectionTestUtils.setField(userItem, "acquiredAt", NOW);
        return userItem;
    }

    private void assertErrorCode(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
