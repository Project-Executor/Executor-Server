package com.seyoung5309.executorserver.domain.message.service;

import com.seyoung5309.executorserver.domain.auth.repository.UserRepository;
import com.seyoung5309.executorserver.domain.friend.repository.FriendRepository;
import com.seyoung5309.executorserver.domain.message.dto.MessageRoomResponse;
import com.seyoung5309.executorserver.domain.message.dto.SendMessageResponse;
import com.seyoung5309.executorserver.domain.message.entity.Message;
import com.seyoung5309.executorserver.domain.message.repository.MessageRepository;
import com.seyoung5309.executorserver.domain.message.repository.RoomRow;
import com.seyoung5309.executorserver.domain.profile.entity.Profile;
import com.seyoung5309.executorserver.domain.profile.repository.ProfileRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static com.seyoung5309.executorserver.domain.stage.StageFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MessageServiceTest {

    private static final Long ME = 1L;
    private static final Long FRIEND = 2L;
    private static final Instant NOW = Instant.parse("2026-11-12T03:00:00Z");

    private MessageRepository messageRepository;
    private FriendRepository friendRepository;
    private ProfileRepository profileRepository;
    private MessageService service;

    @BeforeEach
    void setUp() {
        messageRepository = mock(MessageRepository.class);
        friendRepository = mock(FriendRepository.class);
        profileRepository = mock(ProfileRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.getReferenceById(anyLong())).thenAnswer(inv -> user(inv.getArgument(0)));
        service = new MessageService(messageRepository, friendRepository, profileRepository, userRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void 친구에게_보내면_저장되고_ID와_시각이_나온다() {
        when(friendRepository.areFriends(ME, FRIEND)).thenReturn(true);
        when(messageRepository.save(any())).thenAnswer(inv -> {
            Message saved = inv.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 89L);
            return saved;
        });

        SendMessageResponse response = service.send(ME, FRIEND, "18턴!");

        assertThat(response).isEqualTo(new SendMessageResponse(89L, NOW));
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo("18턴!");
        assertThat(captor.getValue().isRead()).isFalse();
        assertThat(captor.getValue().getReceiver().getId()).isEqualTo(FRIEND);
    }

    @Test
    void 친구가_아니면_NOT_FRIEND이고_저장하지_않는다() {
        when(friendRepository.areFriends(ME, 3L)).thenReturn(false);

        assertErrorCode(() -> service.send(ME, 3L, "hi"), ErrorCode.NOT_FRIEND);
        verify(messageRepository, never()).save(any());
    }

    @Test
    void 자기_자신에게는_보낼_수_없다() {
        assertErrorCode(() -> service.send(ME, ME, "hi"), ErrorCode.INVALID_INPUT);
        verify(friendRepository, never()).areFriends(anyLong(), anyLong());
    }

    @Test
    void 대화_상대_목록은_쿼리_순서대로_마지막_메시지와_안_읽은_수를_붙인다() {
        when(messageRepository.findRooms(ME)).thenReturn(List.of(room(3L, 20L, 0), room(FRIEND, 10L, 2)));
        when(messageRepository.findAllById(any())).thenReturn(List.of(
                message(10L, FRIEND, ME, "보스 몇 턴?", NOW.minusSeconds(60)), message(20L, ME, 3L, "ㅎㅇ", NOW)));
        when(profileRepository.findAllById(any())).thenReturn(List.of(profile(FRIEND, "nullptr"), profile(3L, "segfault")));

        List<MessageRoomResponse> rooms = service.getRooms(ME);

        assertThat(rooms).extracting(r -> r.partner().nickname(), MessageRoomResponse::lastMessage,
                        MessageRoomResponse::unreadCount)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("segfault", "ㅎㅇ", 0L),
                        org.assertj.core.groups.Tuple.tuple("nullptr", "보스 몇 턴?", 2L));
        assertThat(rooms.get(1).lastMessageAt()).isEqualTo(NOW.minusSeconds(60));
    }

    @Test
    void 대화가_없으면_빈_목록이고_추가_조회를_하지_않는다() {
        when(messageRepository.findRooms(ME)).thenReturn(List.of());

        assertThat(service.getRooms(ME)).isEmpty();
        verify(profileRepository, never()).findAllById(any());
    }

    @Test
    void 대화를_조회하면_먼저_읽음_처리하고_최신순으로_가져온다() {
        Page<Message> page = new PageImpl<>(List.of(message(10L, FRIEND, ME, "a", NOW)));
        when(messageRepository.findConversation(eq(ME), eq(FRIEND), any())).thenReturn(page);

        var response = service.getConversation(ME, FRIEND, 1, 30);

        InOrder order = inOrder(messageRepository);
        order.verify(messageRepository).markAsRead(ME, FRIEND);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        order.verify(messageRepository).findConversation(eq(ME), eq(FRIEND), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(30);
        assertThat(pageable.getValue().getSort())
                .isEqualTo(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        assertThat(response.content()).extracting(m -> m.senderId()).containsExactly(FRIEND);
    }

    @Test
    void 안_읽은_수() {
        when(messageRepository.countByReceiverIdAndIsReadFalse(ME)).thenReturn(3L);

        assertThat(service.getUnreadCount(ME).count()).isEqualTo(3L);
    }

    private static RoomRow room(Long partnerId, Long lastMessageId, long unread) {
        return new RoomRow() {
            public Long getPartnerId() { return partnerId; }
            public Long getLastMessageId() { return lastMessageId; }
            public Long getUnreadCount() { return unread; }
        };
    }

    private static Message message(Long id, Long senderId, Long receiverId, String content, Instant at) {
        Message message = new Message(user(senderId), user(receiverId), content, at);
        ReflectionTestUtils.setField(message, "id", id);
        return message;
    }

    private static Profile profile(Long userId, String nickname) {
        Profile profile = new Profile(user(userId), nickname);
        ReflectionTestUtils.setField(profile, "userId", userId);
        return profile;
    }

    private static void assertErrorCode(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
