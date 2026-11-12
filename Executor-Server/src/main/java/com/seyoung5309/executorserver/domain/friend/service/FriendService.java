package com.seyoung5309.executorserver.domain.friend.service;

import com.seyoung5309.executorserver.domain.auth.repository.UserRepository;
import com.seyoung5309.executorserver.domain.friend.dto.FriendRequestItemResponse;
import com.seyoung5309.executorserver.domain.friend.dto.FriendRequestStatusResponse;
import com.seyoung5309.executorserver.domain.friend.dto.FriendResponse;
import com.seyoung5309.executorserver.domain.friend.entity.FriendRequest;
import com.seyoung5309.executorserver.domain.friend.repository.FriendRepository;
import com.seyoung5309.executorserver.domain.friend.repository.FriendRequestRepository;
import com.seyoung5309.executorserver.domain.friend.repository.FriendRow;
import com.seyoung5309.executorserver.domain.friend.repository.RequestPair;
import com.seyoung5309.executorserver.domain.profile.entity.Profile;
import com.seyoung5309.executorserver.domain.profile.repository.ProfileRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import com.seyoung5309.executorserver.global.response.UserSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 친구와 친구 요청.
 * <p>
 * 같은 두 사람 사이의 상태를 바꾸는 작업(요청, 수락, 거절, 취소, 삭제)은 먼저 두 사람의 프로필 행을 ID 순서대로 잠그고 시작한다.
 * 그래서 A->B, B->A 요청이 동시에 오거나 수락과 취소가 동시에 와도 순서대로 처리된다.
 * <p>
 * 친구를 끊으면 두 사람 사이의 요청 기록도 지워서, 다시 친구 요청을 처음부터 보낼 수 있게 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FriendService {

    private final FriendRepository friendRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    // ---- 친구 ----

    /** 내 친구 목록 (닉네임 순) */
    @Transactional(readOnly = true)
    public List<FriendResponse> getFriends(Long userId) {
        List<FriendRow> rows = friendRepository.findFriendRows(userId);
        Map<Long, Profile> profiles = profilesById(rows.stream().map(FriendRow::friendUserId).toList());

        return rows.stream()
                .filter(row -> profiles.containsKey(row.friendUserId()))
                .map(row -> new FriendResponse(UserSummary.from(profiles.get(row.friendUserId())), row.since()))
                .sorted(Comparator.comparing(friend -> friend.user().nickname()))
                .toList();
    }

    /**
     * 친구를 끊는다. 두 사람 사이의 요청 기록도 함께 지운다.
     *
     * @throws BusinessException NOT_FOUND(친구가 아님)
     */
    public void deleteFriend(Long userId, Long friendUserId) {
        lockPair(userId, friendUserId);
        if (friendRepository.deletePair(Math.min(userId, friendUserId), Math.max(userId, friendUserId)) == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "친구가 아닌 사용자입니다.");
        }
        friendRequestRepository.deleteBetween(userId, friendUserId);
    }

    // ---- 친구 요청 ----

    /**
     * 친구 요청을 보낸다.
     * <ul>
     *     <li>상대가 나에게 보낸 대기 중 요청이 있으면 그 요청을 수락하고 바로 친구가 된다 (status = ACCEPTED)</li>
     *     <li>내가 보냈다가 거절된 요청이 있으면 같은 행을 다시 PENDING으로 되돌린다</li>
     * </ul>
     *
     * @throws BusinessException INVALID_INPUT(자기 자신), NOT_FOUND, ALREADY_FRIEND, REQUEST_ALREADY_EXISTS
     */
    public FriendRequestStatusResponse send(Long senderId, Long receiverId) {
        if (senderId.equals(receiverId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "자기 자신에게는 친구 요청을 보낼 수 없습니다.");
        }
        if (lockPair(senderId, receiverId) < 2) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }
        if (friendRepository.areFriends(senderId, receiverId)) {
            throw new BusinessException(ErrorCode.ALREADY_FRIEND);
        }
        Instant now = clock.instant();

        // 잠금 뒤의 읽기는 모두 잠금 읽기로 한다 (findByIdForUpdate 설명 참고)
        Optional<FriendRequest> reverse = friendRequestRepository.findForUpdate(receiverId, senderId)
                .filter(FriendRequest::isPending);
        if (reverse.isPresent()) {
            return FriendRequestStatusResponse.from(acceptAndBefriend(reverse.get(), now));
        }

        FriendRequest request = friendRequestRepository.findForUpdate(senderId, receiverId)
                .map(existing -> {
                    if (existing.isPending()) {
                        throw new BusinessException(ErrorCode.REQUEST_ALREADY_EXISTS);
                    }
                    existing.resend(now);
                    return existing;
                })
                .orElseGet(() -> friendRequestRepository.save(FriendRequest.pending(
                        userRepository.getReferenceById(senderId), userRepository.getReferenceById(receiverId), now)));
        friendRequestRepository.flush();
        return FriendRequestStatusResponse.from(request);
    }

    /** 나에게 온 대기 중 요청 (최근 순). user는 보낸 사람 */
    @Transactional(readOnly = true)
    public List<FriendRequestItemResponse> getReceived(Long userId) {
        List<FriendRequest> requests = friendRequestRepository.findReceivedPending(userId);
        return toItems(requests, request -> request.getSender().getId());
    }

    /** 내가 보낸 대기 중 요청 (최근 순). user는 받는 사람 */
    @Transactional(readOnly = true)
    public List<FriendRequestItemResponse> getSent(Long userId) {
        List<FriendRequest> requests = friendRequestRepository.findSentPending(userId);
        return toItems(requests, request -> request.getReceiver().getId());
    }

    /**
     * 나에게 온 대기 중 요청을 수락한다. 같은 트랜잭션에서 친구 관계를 만든다.
     *
     * @throws BusinessException NOT_FOUND, FORBIDDEN(나에게 온 요청이 아님), REQUEST_ALREADY_HANDLED
     */
    public FriendRequestStatusResponse accept(Long userId, Long requestId) {
        FriendRequest request = lockReceivedPending(userId, requestId);
        return FriendRequestStatusResponse.from(acceptAndBefriend(request, clock.instant()));
    }

    /** @throws BusinessException NOT_FOUND, FORBIDDEN, REQUEST_ALREADY_HANDLED */
    public FriendRequestStatusResponse reject(Long userId, Long requestId) {
        FriendRequest request = lockReceivedPending(userId, requestId);
        request.reject(clock.instant());
        friendRequestRepository.flush();
        return FriendRequestStatusResponse.from(request);
    }

    /**
     * 내가 보낸 대기 중 요청을 취소한다 (행 삭제).
     *
     * @throws BusinessException NOT_FOUND, FORBIDDEN(내가 보낸 요청이 아님), REQUEST_ALREADY_HANDLED
     */
    public void cancel(Long userId, Long requestId) {
        RequestPair pair = findPair(requestId);
        if (!pair.senderId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "내가 보낸 요청만 취소할 수 있습니다.");
        }
        FriendRequest request = lockAndLoadPending(pair, requestId);
        friendRequestRepository.delete(request);
        friendRequestRepository.flush();
    }

    // ---- 내부 ----

    private FriendRequest acceptAndBefriend(FriendRequest request, Instant now) {
        request.accept(now);
        friendRequestRepository.flush();
        Long a = request.getSender().getId();
        Long b = request.getReceiver().getId();
        friendRepository.insertPair(Math.min(a, b), Math.max(a, b), now);
        return request;
    }

    private FriendRequest lockReceivedPending(Long userId, Long requestId) {
        RequestPair pair = findPair(requestId);
        if (!pair.receiverId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "나에게 온 요청만 처리할 수 있습니다.");
        }
        return lockAndLoadPending(pair, requestId);
    }

    /**
     * 두 사람을 잠근 뒤 요청을 다시 읽는다. 잠금을 기다리는 사이에 처리되거나 지워졌을 수 있기 때문이다.
     * 앞에서 findPair로 이미 한 번 읽었으므로 일반 SELECT로는 그때의 스냅샷이 보인다. 그래서 잠금 읽기로 다시 읽는다.
     */
    private FriendRequest lockAndLoadPending(RequestPair pair, Long requestId) {
        lockPair(pair.senderId(), pair.receiverId());
        FriendRequest request = friendRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REQUEST_ALREADY_HANDLED, "이미 취소된 요청입니다."));
        if (!request.isPending()) {
            throw new BusinessException(ErrorCode.REQUEST_ALREADY_HANDLED);
        }
        return request;
    }

    /** 요청의 보낸 사람·받는 사람만 읽는다 (엔티티는 잠근 뒤에 읽어야 최신 상태를 본다) */
    private RequestPair findPair(Long requestId) {
        return friendRequestRepository.findPair(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "친구 요청을 찾을 수 없습니다."));
    }

    /** @return 잠근 프로필 수 (둘 다 있으면 2) */
    private int lockPair(Long a, Long b) {
        return profileRepository.lockPair(a, b).size();
    }

    private List<FriendRequestItemResponse> toItems(List<FriendRequest> requests,
                                                    Function<FriendRequest, Long> otherUserId) {
        Map<Long, Profile> profiles = profilesById(requests.stream().map(otherUserId).toList());
        return requests.stream()
                .filter(request -> profiles.containsKey(otherUserId.apply(request)))
                .map(request -> new FriendRequestItemResponse(request.getId(),
                        UserSummary.from(profiles.get(otherUserId.apply(request))), request.getCreatedAt()))
                .toList();
    }

    private Map<Long, Profile> profilesById(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return profileRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(Profile::getUserId, Function.identity()));
    }
}
