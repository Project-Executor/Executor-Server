package com.seyoung5309.executorserver.domain.admin.service;

import com.seyoung5309.executorserver.domain.admin.dto.AdminMailRequest;
import com.seyoung5309.executorserver.domain.auth.repository.UserRepository;
import com.seyoung5309.executorserver.domain.item.entity.Item;
import com.seyoung5309.executorserver.domain.item.entity.ItemType;
import com.seyoung5309.executorserver.domain.item.repository.ItemRepository;
import com.seyoung5309.executorserver.domain.mail.repository.MailRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.seyoung5309.executorserver.domain.stage.StageFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminMailServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-12T03:00:00Z");

    private MailRepository mailRepository;
    private ItemRepository itemRepository;
    private UserRepository userRepository;
    private AdminMailService service;

    @BeforeEach
    void setUp() {
        mailRepository = mock(MailRepository.class);
        itemRepository = mock(ItemRepository.class);
        userRepository = mock(UserRepository.class);
        service = new AdminMailService(mailRepository, itemRepository, userRepository, Clock.fixed(NOW, ZoneOffset.UTC));
        when(userRepository.findAllById(any())).thenAnswer(inv -> {
            Collection<Long> ids = inv.getArgument(0);
            return ids.stream().filter(id -> id < 100).map(id -> user(id)).toList();
        });
        when(mailRepository.sendTo(any(), any(), any(), anyInt(), any(), anyInt(), any(), any()))
                .thenAnswer(inv -> ((Collection<?>) inv.getArgument(0)).size());
    }

    @Test
    void 받는_사람_중복은_한_번만_보내고_생략한_숫자는_0이다() {
        var response = service.send(request(List.of(1L, 2L, 1L), null, null, null, null));

        assertThat(response.sentCount()).isEqualTo(2);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> ids = ArgumentCaptor.forClass(Collection.class);
        verify(mailRepository).sendTo(ids.capture(), eq("보상"), isNull(), eq(0), isNull(), eq(0), isNull(), eq(NOW));
        assertThat(ids.getValue()).containsExactly(1L, 2L);
    }

    @Test
    void toAll이면_receiverIds를_무시하고_전체에게_보낸다() {
        when(mailRepository.sendToAll(any(), any(), anyInt(), any(), anyInt(), any(), any())).thenReturn(42);

        AdminMailRequest request = new AdminMailRequest(List.of(999L), true, "공지", null, 100, null, null, null);

        assertThat(service.send(request).sentCount()).isEqualTo(42);
        verify(mailRepository, never()).sendTo(any(), any(), any(), anyInt(), any(), anyInt(), any(), any());
    }

    @Test
    void 받는_사람이_없으면_INVALID_INPUT() {
        assertErrorCode(() -> service.send(request(List.of(), null, null, null, null)), ErrorCode.INVALID_INPUT);
        assertErrorCode(() -> service.send(request(null, null, null, null, null)), ErrorCode.INVALID_INPUT);
    }

    @Test
    void 없는_사용자가_있으면_NOT_FOUND이고_그_ID를_알려_준다() {
        assertThatThrownBy(() -> service.send(request(List.of(1L, 100L, 200L), null, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("[100, 200]");
        verify(mailRepository, never()).sendTo(any(), any(), any(), anyInt(), any(), anyInt(), any(), any());
    }

    @Test
    void 만료_시각은_미래여야_한다() {
        assertErrorCode(() -> service.send(request(List.of(1L), null, null, null, NOW)), ErrorCode.INVALID_INPUT);
    }

    @Test
    void 첨부_아이템_수량_규칙() {
        when(itemRepository.findById(3L)).thenReturn(Optional.of(item(3L, ItemType.SKIN)));
        when(itemRepository.findById(9L)).thenReturn(Optional.of(item(9L, ItemType.HINT)));

        // 아이템 없이 수량만
        assertErrorCode(() -> service.send(request(List.of(1L), null, null, 1, null)), ErrorCode.INVALID_INPUT);
        // 아이템이 있는데 수량 0
        assertErrorCode(() -> service.send(request(List.of(1L), null, 9L, 0, null)), ErrorCode.INVALID_INPUT);
        // 스킨 2개
        assertErrorCode(() -> service.send(request(List.of(1L), null, 3L, 2, null)), ErrorCode.INVALID_INPUT);
        // 없는 아이템
        assertErrorCode(() -> service.send(request(List.of(1L), null, 77L, 1, null)), ErrorCode.NOT_FOUND);
        // 힌트 5개는 된다
        assertThat(service.send(request(List.of(1L), null, 9L, 5, null)).sentCount()).isEqualTo(1);
    }

    private static AdminMailRequest request(List<Long> ids, Integer coin, Long itemId, Integer quantity, Instant expiresAt) {
        return new AdminMailRequest(ids, null, " 보상 ", "  ", coin, itemId, quantity, expiresAt);
    }

    private static Item item(Long id, ItemType type) {
        Item item = new Item("item" + id, null, null, type, 100);
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
