package com.seyoung5309.executorserver.domain.mail.service;

import com.seyoung5309.executorserver.domain.item.entity.Item;
import com.seyoung5309.executorserver.domain.item.entity.ItemType;
import com.seyoung5309.executorserver.domain.item.repository.UserItemRepository;
import com.seyoung5309.executorserver.domain.mail.dto.ClaimAllResponse;
import com.seyoung5309.executorserver.domain.mail.dto.ClaimResponse;
import com.seyoung5309.executorserver.domain.mail.entity.Mail;
import com.seyoung5309.executorserver.domain.mail.repository.MailRepository;
import com.seyoung5309.executorserver.domain.profile.repository.ProfileRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MailServiceTest {

    private static final Long ME = 1L;
    private static final Instant NOW = Instant.parse("2026-11-12T03:00:00Z");

    private MailRepository mailRepository;
    private ProfileRepository profileRepository;
    private UserItemRepository userItemRepository;
    private MailService service;

    private final Item skin = item(3L, ItemType.SKIN, 300);
    private final Item hint = item(9L, ItemType.HINT, 30);

    @BeforeEach
    void setUp() {
        mailRepository = mock(MailRepository.class);
        profileRepository = mock(ProfileRepository.class);
        userItemRepository = mock(UserItemRepository.class);
        service = new MailService(mailRepository, profileRepository, userItemRepository, Clock.fixed(NOW, ZoneOffset.UTC));
        when(profileRepository.findCoinByUserId(ME)).thenReturn(Optional.of(750));
        when(mailRepository.markClaimed(anyLong(), eq(ME), eq(NOW))).thenReturn(1);
    }

    @Test
    void 코인만_있는_우편은_코인만_더한다() {
        given(mail(15L, ME, 100, null, 0));

        ClaimResponse response = service.claim(ME, 15L);

        assertThat(response).isEqualTo(new ClaimResponse(15L, 100, 0, null, 750));
        verify(profileRepository).addCoin(ME, 100);
        verifyNoInteractions(userItemRepository);
    }

    @Test
    void 힌트는_수량만큼_더한다() {
        given(mail(15L, ME, 0, hint, 3));

        ClaimResponse response = service.claim(ME, 15L);

        assertThat(response.rewardItem()).isEqualTo(new ClaimResponse.GrantedItem(9L, ItemType.HINT, 3));
        verify(userItemRepository).addQuantity(ME, 9L, 3, NOW);
    }

    @Test
    void 없는_스킨은_아이템으로_받고_코인을_먼저_더해_잠금_순서를_맞춘다() {
        given(mail(15L, ME, 50, skin, 1));
        when(userItemRepository.insertIfAbsent(ME, 3L, NOW)).thenReturn(1);

        ClaimResponse response = service.claim(ME, 15L);

        assertThat(response.rewardItem()).isEqualTo(new ClaimResponse.GrantedItem(3L, ItemType.SKIN, 1));
        assertThat(response.convertedCoin()).isZero();
        InOrder order = inOrder(mailRepository, profileRepository, userItemRepository);
        order.verify(mailRepository).markClaimed(15L, ME, NOW);
        order.verify(profileRepository).addCoin(ME, 50);
        order.verify(userItemRepository).insertIfAbsent(ME, 3L, NOW);
    }

    @Test
    void 이미_가진_스킨은_가격만큼_코인으로_바꾼다() {
        given(mail(15L, ME, 100, skin, 1));
        when(userItemRepository.insertIfAbsent(ME, 3L, NOW)).thenReturn(0);

        ClaimResponse response = service.claim(ME, 15L);

        assertThat(response).isEqualTo(new ClaimResponse(15L, 100, 300, null, 750));
        verify(profileRepository).addCoin(ME, 300);
    }

    @Test
    void 남의_우편은_FORBIDDEN이고_수령_처리하지_않는다() {
        given(mail(15L, 2L, 100, null, 0));

        assertErrorCode(() -> service.claim(ME, 15L), ErrorCode.FORBIDDEN);
        verify(mailRepository, never()).markClaimed(anyLong(), anyLong(), any());
    }

    @Test
    void 없는_우편은_NOT_FOUND() {
        when(mailRepository.findWithItemById(99L)).thenReturn(Optional.empty());

        assertErrorCode(() -> service.claim(ME, 99L), ErrorCode.NOT_FOUND);
    }

    @Test
    void 이미_받은_우편은_ALREADY_CLAIMED이고_보상을_주지_않는다() {
        Mail claimed = mail(15L, ME, 100, null, 0);
        ReflectionTestUtils.setField(claimed, "claimedAt", NOW.minusSeconds(10));
        given(claimed);
        when(mailRepository.markClaimed(15L, ME, NOW)).thenReturn(0);

        assertErrorCode(() -> service.claim(ME, 15L), ErrorCode.ALREADY_CLAIMED);
        verify(profileRepository, never()).addCoin(anyLong(), anyInt());
    }

    @Test
    void 만료된_우편은_MAIL_EXPIRED() {
        Mail expired = mail(15L, ME, 100, null, 0);
        ReflectionTestUtils.setField(expired, "expiresAt", NOW);
        given(expired);
        when(mailRepository.markClaimed(15L, ME, NOW)).thenReturn(0);

        assertErrorCode(() -> service.claim(ME, 15L), ErrorCode.MAIL_EXPIRED);
    }

    @Test
    void 조회_뒤_다른_요청이_먼저_받았으면_ALREADY_CLAIMED() {
        given(mail(15L, ME, 100, null, 0));
        when(mailRepository.markClaimed(15L, ME, NOW)).thenReturn(0);

        assertErrorCode(() -> service.claim(ME, 15L), ErrorCode.ALREADY_CLAIMED);
    }

    @Test
    void 모두_받기는_받은_것만_합치고_같은_아이템은_수량을_더한다() {
        Mail coinAndHint = mail(1L, ME, 50, hint, 2);
        Mail ownedSkin = mail(2L, ME, 0, skin, 1);
        Mail moreHint = mail(3L, ME, 10, hint, 3);
        Mail raced = mail(4L, ME, 999, null, 0);
        when(mailRepository.findClaimable(ME, NOW)).thenReturn(List.of(coinAndHint, ownedSkin, moreHint, raced));
        when(mailRepository.markClaimed(4L, ME, NOW)).thenReturn(0);
        when(userItemRepository.insertIfAbsent(ME, 3L, NOW)).thenReturn(0);

        ClaimAllResponse response = service.claimAll(ME);

        assertThat(response).isEqualTo(new ClaimAllResponse(3, 60, 300,
                List.of(new ClaimAllResponse.ItemQuantity(9L, 5)), 750));
        verify(profileRepository, never()).addCoin(ME, 999);
    }

    @Test
    void 받을_우편이_없으면_0통() {
        when(mailRepository.findClaimable(ME, NOW)).thenReturn(List.of());

        assertThat(service.claimAll(ME)).isEqualTo(new ClaimAllResponse(0, 0, 0, List.of(), 750));
    }

    private void given(Mail mail) {
        when(mailRepository.findWithItemById(mail.getId())).thenReturn(Optional.of(mail));
    }

    private static Mail mail(Long id, Long receiverId, int coin, Item item, int quantity) {
        Mail mail = BeanUtils.instantiateClass(Mail.class);
        ReflectionTestUtils.setField(mail, "id", id);
        ReflectionTestUtils.setField(mail, "receiver", user(receiverId));
        ReflectionTestUtils.setField(mail, "title", "보상");
        ReflectionTestUtils.setField(mail, "rewardCoin", coin);
        ReflectionTestUtils.setField(mail, "rewardItem", item);
        ReflectionTestUtils.setField(mail, "rewardItemQuantity", quantity);
        return mail;
    }

    private static Item item(Long id, ItemType type, int price) {
        Item item = new Item("item" + id, null, null, type, price);
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }

    private static void assertErrorCode(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
