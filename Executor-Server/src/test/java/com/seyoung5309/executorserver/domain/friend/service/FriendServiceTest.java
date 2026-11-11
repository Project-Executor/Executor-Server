package com.seyoung5309.executorserver.domain.friend.service;

import com.seyoung5309.executorserver.domain.auth.repository.UserRepository;
import com.seyoung5309.executorserver.domain.friend.dto.FriendRequestStatusResponse;
import com.seyoung5309.executorserver.domain.friend.dto.FriendResponse;
import com.seyoung5309.executorserver.domain.friend.entity.FriendRequest;
import com.seyoung5309.executorserver.domain.friend.entity.FriendRequestStatus;
import com.seyoung5309.executorserver.domain.friend.repository.FriendRepository;
import com.seyoung5309.executorserver.domain.friend.repository.FriendRequestRepository;
import com.seyoung5309.executorserver.domain.friend.repository.FriendRow;
import com.seyoung5309.executorserver.domain.friend.repository.RequestPair;
import com.seyoung5309.executorserver.domain.profile.entity.Profile;
import com.seyoung5309.executorserver.domain.profile.repository.ProfileRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

class FriendServiceTest {

    private static final Long ME = 1L;
    private static final Long OTHER = 2L;
    private static final Instant OLD = Instant.parse("2026-11-01T00:00:00Z");
    private static final Instant NOW = Instant.parse("2026-11-12T03:00:00Z");

    private FriendRepository friendRepository;
    private FriendRequestRepository friendRequestRepository;
    private ProfileRepository profileRepository;
    private UserRepository userRepository;
    private FriendService service;

    @BeforeEach
    void setUp() {
        friendRepository = mock(FriendRepository.class);
        friendRequestRepository = mock(FriendRequestRepository.class);
        profileRepository = mock(ProfileRepository.class);
        userRepository = mock(UserRepository.class);
        service = new FriendService(friendRepository, friendRequestRepository, profileRepository, userRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
        when(profileRepository.lockPair(anyLong(), anyLong())).thenReturn(List.of(1L, 2L));
        when(friendRepository.areFriends(anyLong(), anyLong())).thenCallRealMethod();
        when(friendRequestRepository.findForUpdate(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(anyLong())).thenAnswer(inv -> user(inv.getArgument(0)));
        when(friendRequestRepository.save(any())).thenAnswer(inv -> {
            FriendRequest saved = inv.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 30L);
            return saved;
        });
    }

    // ---- 요청 보내기 ----

    @Test
    void 처음_보내면_새_PENDING_요청이_생긴다() {
        FriendRequestStatusResponse response = service.send(ME, OTHER);

        assertThat(response).isEqualTo(new FriendRequestStatusResponse(30L, FriendRequestStatus.PENDING));
        verify(profileRepository).lockPair(ME, OTHER);
        verify(friendRepository, never()).insertPair(anyLong(), anyLong(), any());
    }

    @Test
    void 자기_자신에게는_보낼_수_없고_잠금도_하지_않는다() {
        assertErrorCode(() -> service.send(ME, ME), ErrorCode.INVALID_INPUT);
        verify(profileRepository, never()).lockPair(anyLong(), anyLong());
    }

    @Test
    void 없는_사용자에게_보내면_NOT_FOUND() {
        when(profileRepository.lockPair(ME, 99L)).thenReturn(List.of(ME));

        assertErrorCode(() -> service.send(ME, 99L), ErrorCode.NOT_FOUND);
    }

    @Test
    void 이미_친구면_ALREADY_FRIEND() {
        when(friendRepository.existsByUserIdAndFriendId(1L, 2L)).thenReturn(true);

        assertErrorCode(() -> service.send(OTHER, ME), ErrorCode.ALREADY_FRIEND);
    }

    @Test
    void 상대가_보낸_대기_중_요청이_있으면_바로_친구가_된다() {
        FriendRequest reverse = request(31L, OTHER, ME, FriendRequestStatus.PENDING);
        when(friendRequestRepository.findForUpdate(OTHER, ME)).thenReturn(Optional.of(reverse));

        FriendRequestStatusResponse response = service.send(ME, OTHER);

        assertThat(response).isEqualTo(new FriendRequestStatusResponse(31L, FriendRequestStatus.ACCEPTED));
        assertThat(reverse.getRespondedAt()).isEqualTo(NOW);
        verify(friendRepository).insertPair(1L, 2L, NOW);
        verify(friendRequestRepository, never()).save(any());
    }

    @Test
    void 이미_대기_중인_요청이_있으면_REQUEST_ALREADY_EXISTS() {
        when(friendRequestRepository.findForUpdate(ME, OTHER))
                .thenReturn(Optional.of(request(30L, ME, OTHER, FriendRequestStatus.PENDING)));

        assertErrorCode(() -> service.send(ME, OTHER), ErrorCode.REQUEST_ALREADY_EXISTS);
    }

    @Test
    void 거절된_요청을_다시_보내면_같은_행이_PENDING으로_돌아간다() {
        FriendRequest rejected = request(30L, ME, OTHER, FriendRequestStatus.REJECTED);
        ReflectionTestUtils.setField(rejected, "respondedAt", OLD);
        when(friendRequestRepository.findForUpdate(ME, OTHER)).thenReturn(Optional.of(rejected));

        FriendRequestStatusResponse response = service.send(ME, OTHER);

        assertThat(response).isEqualTo(new FriendRequestStatusResponse(30L, FriendRequestStatus.PENDING));
        assertThat(rejected.getCreatedAt()).isEqualTo(NOW);
        assertThat(rejected.getRespondedAt()).isNull();
        verify(friendRequestRepository, never()).save(any());
    }

    @Test
    void 상대의_거절된_요청은_무시하고_새로_보낸다() {
        when(friendRequestRepository.findForUpdate(OTHER, ME))
                .thenReturn(Optional.of(request(31L, OTHER, ME, FriendRequestStatus.REJECTED)));

        assertThat(service.send(ME, OTHER).status()).isEqualTo(FriendRequestStatus.PENDING);
        verify(friendRepository, never()).insertPair(anyLong(), anyLong(), any());
    }

    // ---- 수락 / 거절 / 취소 ----

    @Test
    void 수락하면_친구_관계가_작은_ID_큰_ID_순서로_생긴다() {
        givenRequest(request(30L, OTHER, ME, FriendRequestStatus.PENDING));

        assertThat(service.accept(ME, 30L).status()).isEqualTo(FriendRequestStatus.ACCEPTED);
        verify(profileRepository).lockPair(OTHER, ME);
        verify(friendRepository).insertPair(1L, 2L, NOW);
    }

    @Test
    void 거절하면_친구_관계는_생기지_않는다() {
        givenRequest(request(30L, OTHER, ME, FriendRequestStatus.PENDING));

        assertThat(service.reject(ME, 30L).status()).isEqualTo(FriendRequestStatus.REJECTED);
        verify(friendRepository, never()).insertPair(anyLong(), anyLong(), any());
    }

    @Test
    void 나에게_온_요청이_아니면_수락_거절할_수_없다() {
        givenRequest(request(30L, ME, OTHER, FriendRequestStatus.PENDING));

        assertErrorCode(() -> service.accept(ME, 30L), ErrorCode.FORBIDDEN);
        assertErrorCode(() -> service.reject(ME, 30L), ErrorCode.FORBIDDEN);
        verify(profileRepository, never()).lockPair(anyLong(), anyLong());
    }

    @Test
    void 이미_처리된_요청은_REQUEST_ALREADY_HANDLED() {
        givenRequest(request(30L, OTHER, ME, FriendRequestStatus.REJECTED));

        assertErrorCode(() -> service.accept(ME, 30L), ErrorCode.REQUEST_ALREADY_HANDLED);
        verify(friendRepository, never()).insertPair(anyLong(), anyLong(), any());
    }

    @Test
    void 잠금을_기다리는_사이에_취소된_요청은_REQUEST_ALREADY_HANDLED() {
        when(friendRequestRepository.findPair(30L)).thenReturn(Optional.of(new RequestPair(OTHER, ME)));
        when(friendRequestRepository.findByIdForUpdate(30L)).thenReturn(Optional.empty());

        assertErrorCode(() -> service.accept(ME, 30L), ErrorCode.REQUEST_ALREADY_HANDLED);
    }

    @Test
    void 없는_요청은_NOT_FOUND() {
        when(friendRequestRepository.findPair(99L)).thenReturn(Optional.empty());

        assertErrorCode(() -> service.accept(ME, 99L), ErrorCode.NOT_FOUND);
        assertErrorCode(() -> service.cancel(ME, 99L), ErrorCode.NOT_FOUND);
    }

    @Test
    void 내가_보낸_대기_중_요청은_취소할_수_있고_남의_요청은_FORBIDDEN() {
        FriendRequest mine = request(30L, ME, OTHER, FriendRequestStatus.PENDING);
        givenRequest(mine);

        service.cancel(ME, 30L);
        verify(friendRequestRepository).delete(mine);

        assertErrorCode(() -> service.cancel(OTHER, 30L), ErrorCode.FORBIDDEN);
    }

    // ---- 친구 ----

    @Test
    void 친구를_끊으면_두_사람_사이의_요청도_지운다() {
        when(friendRepository.deletePair(1L, 2L)).thenReturn(1);

        service.deleteFriend(OTHER, ME);

        verify(friendRequestRepository).deleteBetween(OTHER, ME);
    }

    @Test
    void 친구가_아니면_삭제는_NOT_FOUND() {
        when(friendRepository.deletePair(1L, 2L)).thenReturn(0);

        assertErrorCode(() -> service.deleteFriend(ME, OTHER), ErrorCode.NOT_FOUND);
        verify(friendRequestRepository, never()).deleteBetween(anyLong(), anyLong());
    }

    @Test
    void 친구_목록은_닉네임_순이다() {
        when(friendRepository.findFriendRows(ME)).thenReturn(List.of(new FriendRow(2L, OLD), new FriendRow(3L, NOW)));
        when(profileRepository.findAllById(any())).thenReturn(List.of(profile(2L, "zeta"), profile(3L, "alpha")));

        List<FriendResponse> friends = service.getFriends(ME);

        assertThat(friends).extracting(f -> f.user().nickname()).containsExactly("alpha", "zeta");
        assertThat(friends.get(1).since()).isEqualTo(OLD);
    }

    private void givenRequest(FriendRequest request) {
        RequestPair pair = new RequestPair(request.getSender().getId(), request.getReceiver().getId());
        when(friendRequestRepository.findPair(request.getId())).thenReturn(Optional.of(pair));
        when(friendRequestRepository.findByIdForUpdate(request.getId())).thenReturn(Optional.of(request));
    }

    private static FriendRequest request(Long id, Long senderId, Long receiverId, FriendRequestStatus status) {
        FriendRequest request = FriendRequest.pending(user(senderId), user(receiverId), OLD);
        ReflectionTestUtils.setField(request, "id", id);
        ReflectionTestUtils.setField(request, "status", status);
        return request;
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
