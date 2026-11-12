package com.seyoung5309.executorserver.domain.message.service;

import com.seyoung5309.executorserver.domain.auth.repository.UserRepository;
import com.seyoung5309.executorserver.domain.friend.repository.FriendRepository;
import com.seyoung5309.executorserver.domain.message.dto.MessageResponse;
import com.seyoung5309.executorserver.domain.message.dto.MessageRoomResponse;
import com.seyoung5309.executorserver.domain.message.dto.SendMessageResponse;
import com.seyoung5309.executorserver.domain.message.dto.UnreadCountResponse;
import com.seyoung5309.executorserver.domain.message.entity.Message;
import com.seyoung5309.executorserver.domain.message.repository.MessageRepository;
import com.seyoung5309.executorserver.domain.message.repository.RoomRow;
import com.seyoung5309.executorserver.domain.profile.entity.Profile;
import com.seyoung5309.executorserver.domain.profile.repository.ProfileRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import com.seyoung5309.executorserver.global.response.PageResponse;
import com.seyoung5309.executorserver.global.response.UserSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MessageService {

    /** 대화 조회 정렬: 최신순 (같은 시각이면 나중에 저장된 것 먼저). 클라이언트가 보낸 정렬은 쓰지 않는다. */
    private static final Sort LATEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final MessageRepository messageRepository;
    private final FriendRepository friendRepository;
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    /**
     * 친구에게 메시지를 보낸다.
     * 친구가 아니면(없는 사용자 포함) NOT_FRIEND — 사용자가 있는지 없는지를 따로 알려 주지 않는다.
     *
     * @throws BusinessException INVALID_INPUT(자기 자신), NOT_FRIEND
     */
    public SendMessageResponse send(Long senderId, Long receiverId, String content) {
        if (senderId.equals(receiverId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "자기 자신에게는 메시지를 보낼 수 없습니다.");
        }
        if (!friendRepository.areFriends(senderId, receiverId)) {
            throw new BusinessException(ErrorCode.NOT_FRIEND);
        }
        Message message = messageRepository.save(new Message(userRepository.getReferenceById(senderId),
                userRepository.getReferenceById(receiverId), content, clock.instant()));
        return new SendMessageResponse(message.getId(), message.getCreatedAt());
    }

    /**
     * 대화 상대 목록 (마지막 메시지가 최근인 순). 친구를 끊은 상대와의 대화도 남아 있다.
     */
    @Transactional(readOnly = true)
    public List<MessageRoomResponse> getRooms(Long userId) {
        List<RoomRow> rows = messageRepository.findRooms(userId);
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<Long, Message> lastMessages = messageRepository.findAllById(rows.stream().map(RoomRow::getLastMessageId).toList())
                .stream().collect(Collectors.toMap(Message::getId, Function.identity()));
        Map<Long, Profile> partners = profileRepository.findAllById(rows.stream().map(RoomRow::getPartnerId).toList())
                .stream().collect(Collectors.toMap(Profile::getUserId, Function.identity()));

        return rows.stream()
                .filter(row -> partners.containsKey(row.getPartnerId()) && lastMessages.containsKey(row.getLastMessageId()))
                .map(row -> {
                    Message last = lastMessages.get(row.getLastMessageId());
                    return new MessageRoomResponse(UserSummary.from(partners.get(row.getPartnerId())),
                            last.getContent(), last.getCreatedAt(), row.getUnreadCount());
                })
                .toList();
    }

    /**
     * 상대와 주고받은 메시지 (최신순, 페이지). 조회하면 상대가 나에게 보낸 메시지를 먼저 읽음 처리하므로
     * 응답의 isRead에도 바로 반영된다. 친구를 끊은 뒤에도 기존 대화는 볼 수 있다.
     */
    public PageResponse<MessageResponse> getConversation(Long userId, Long partnerId, int page, int size) {
        messageRepository.markAsRead(userId, partnerId);
        return PageResponse.of(
                messageRepository.findConversation(userId, partnerId, PageRequest.of(page, size, LATEST_FIRST)),
                MessageResponse::from);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(Long userId) {
        return new UnreadCountResponse(messageRepository.countByReceiverIdAndIsReadFalse(userId));
    }
}
