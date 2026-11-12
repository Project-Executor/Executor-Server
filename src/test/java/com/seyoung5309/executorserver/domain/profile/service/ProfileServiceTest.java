package com.seyoung5309.executorserver.domain.profile.service;

import com.seyoung5309.executorserver.domain.auth.entity.User;
import com.seyoung5309.executorserver.domain.friend.repository.FriendRepository;
import com.seyoung5309.executorserver.domain.item.entity.Item;
import com.seyoung5309.executorserver.domain.item.entity.ItemType;
import com.seyoung5309.executorserver.domain.item.entity.UserItem;
import com.seyoung5309.executorserver.domain.item.repository.UserItemRepository;
import com.seyoung5309.executorserver.domain.profile.dto.EquipmentRequest;
import com.seyoung5309.executorserver.domain.profile.dto.EquipmentResponse;
import com.seyoung5309.executorserver.domain.profile.dto.MyProfileResponse;
import com.seyoung5309.executorserver.domain.profile.dto.ProfileDetailResponse;
import com.seyoung5309.executorserver.domain.profile.dto.UpdateProfileRequest;
import com.seyoung5309.executorserver.domain.profile.entity.Profile;
import com.seyoung5309.executorserver.domain.profile.repository.ProfileRepository;
import com.seyoung5309.executorserver.domain.stage.repository.StarSummary;
import com.seyoung5309.executorserver.domain.stage.repository.UserStageRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ProfileServiceTest {

    private ProfileRepository profileRepository;
    private UserItemRepository userItemRepository;
    private UserStageRepository userStageRepository;
    private FriendRepository friendRepository;
    private ProfileService profileService;

    private Profile me;

    @BeforeEach
    void setUp() {
        profileRepository = mock(ProfileRepository.class);
        userItemRepository = mock(UserItemRepository.class);
        userStageRepository = mock(UserStageRepository.class);
        friendRepository = mock(FriendRepository.class);
        profileService = new ProfileService(profileRepository, userItemRepository, userStageRepository, friendRepository);

        me = profile(1L, "me@example.com", "debugger");
        when(profileRepository.findWithUserById(1L)).thenReturn(Optional.of(me));
    }

    // ---------- 내 프로필 ----------

    @Test
    void 내_프로필에는_이메일과_권한과_코인이_들어간다() {
        MyProfileResponse response = profileService.getMyProfile(1L);

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("me@example.com");
        assertThat(response.nickname()).isEqualTo("debugger");
        assertThat(response.coin()).isZero();
        assertThat(response.equippedSkinId()).isNull();
    }

    @Test
    void 탈퇴해서_프로필이_없으면_UNAUTHORIZED() {
        when(profileRepository.findWithUserById(9L)).thenReturn(Optional.empty());

        assertErrorCode(() -> profileService.getMyProfile(9L), ErrorCode.UNAUTHORIZED);
    }

    // ---------- 수정 ----------

    @Test
    void 보낸_필드만_바뀌고_공백은_지워진다() {
        MyProfileResponse response = profileService.updateMyProfile(1L,
                new UpdateProfileRequest(null, "  오집행  ", " 버그 잡는 중 "));

        assertThat(response.nickname()).isEqualTo("debugger");
        assertThat(response.characterName()).isEqualTo("오집행");
        assertThat(response.comment()).isEqualTo("버그 잡는 중");
    }

    @Test
    void 자기소개에_빈_문자열을_보내면_지워진다() {
        me.changeComment("이전 소개");

        MyProfileResponse response = profileService.updateMyProfile(1L, new UpdateProfileRequest(null, null, "   "));

        assertThat(response.comment()).isNull();
    }

    @Test
    void 닉네임을_바꿀_수_있다() {
        MyProfileResponse response = profileService.updateMyProfile(1L, new UpdateProfileRequest("newname", null, null));

        assertThat(response.nickname()).isEqualTo("newname");
    }

    @Test
    void 지금_닉네임과_같으면_중복_검사를_하지_않는다() {
        profileService.updateMyProfile(1L, new UpdateProfileRequest("debugger", null, null));

        verify(profileRepository, never()).existsByNickname(any());
    }

    @Test
    void 다른_사람이_쓰는_닉네임이면_DUPLICATE_NICKNAME() {
        when(profileRepository.existsByNickname("taken")).thenReturn(true);

        assertErrorCode(() -> profileService.updateMyProfile(1L, new UpdateProfileRequest("taken", null, null)),
                ErrorCode.DUPLICATE_NICKNAME);
        assertThat(me.getNickname()).isEqualTo("debugger");
    }

    @Test
    void 동시에_같은_닉네임으로_바꿔서_DB_제약에_걸리면_DUPLICATE_NICKNAME() {
        doThrow(new DataIntegrityViolationException("dup")).when(profileRepository).flush();

        assertErrorCode(() -> profileService.updateMyProfile(1L, new UpdateProfileRequest("newname", null, null)),
                ErrorCode.DUPLICATE_NICKNAME);
    }

    @Test
    void 공백뿐인_집행자_이름은_INVALID_INPUT() {
        assertErrorCode(() -> profileService.updateMyProfile(1L, new UpdateProfileRequest(null, "   ", null)),
                ErrorCode.INVALID_INPUT);
    }

    @Test
    void 공백을_지우면_한_글자인_닉네임은_INVALID_INPUT() {
        assertErrorCode(() -> profileService.updateMyProfile(1L, new UpdateProfileRequest(" a ", null, null)),
                ErrorCode.INVALID_INPUT);
    }

    // ---------- 장착 ----------

    @Test
    void 보유한_스킨과_테마를_장착한다() {
        own(1L, item(3L, ItemType.SKIN), 1);
        own(1L, item(4L, ItemType.THEME), 1);

        EquipmentResponse response = profileService.updateEquipment(1L, new EquipmentRequest(3L, 4L));

        assertThat(response).isEqualTo(new EquipmentResponse(3L, 4L));
    }

    @Test
    void null이면_장착을_해제한다() {
        me.equipSkin(item(3L, ItemType.SKIN));

        EquipmentResponse response = profileService.updateEquipment(1L, new EquipmentRequest(null, null));

        assertThat(response).isEqualTo(new EquipmentResponse(null, null));
    }

    @Test
    void 보유하지_않은_아이템은_FORBIDDEN() {
        when(userItemRepository.findByUserIdAndItemId(1L, 3L)).thenReturn(Optional.empty());

        assertErrorCode(() -> profileService.updateEquipment(1L, new EquipmentRequest(3L, null)), ErrorCode.FORBIDDEN);
    }

    @Test
    void 스킨_자리에_테마를_넣으면_INVALID_INPUT() {
        own(1L, item(4L, ItemType.THEME), 1);

        assertErrorCode(() -> profileService.updateEquipment(1L, new EquipmentRequest(4L, null)), ErrorCode.INVALID_INPUT);
    }

    @Test
    void 힌트는_장착할_수_없다() {
        own(1L, item(9L, ItemType.HINT), 3);

        assertErrorCode(() -> profileService.updateEquipment(1L, new EquipmentRequest(null, 9L)), ErrorCode.INVALID_INPUT);
    }

    // ---------- 다른 사용자 프로필 ----------

    @Test
    void 다른_사용자_프로필에는_클리어_수와_별_합계와_친구_여부가_들어간다() {
        Profile other = profile(12L, "other@example.com", "nullptr");
        other.changeComment("안녕하세요");
        when(profileRepository.findById(12L)).thenReturn(Optional.of(other));
        when(userStageRepository.summarizeOfficialStars(12L)).thenReturn(new StarSummary(7, 6, 5));
        when(friendRepository.areFriends(1L, 12L)).thenReturn(true);

        ProfileDetailResponse response = profileService.getProfile(1L, 12L);

        assertThat(response).isEqualTo(new ProfileDetailResponse(12L, "nullptr", "안녕하세요", null, 7, 18, true));
    }

    @Test
    void 없는_사용자면_NOT_FOUND() {
        when(profileRepository.findById(99L)).thenReturn(Optional.empty());

        assertErrorCode(() -> profileService.getProfile(1L, 99L), ErrorCode.NOT_FOUND);
    }

    // ---------- 검색 ----------

    @Test
    void 검색어의_LIKE_특수문자는_글자_그대로_찾는다() {
        assertThat(ProfileService.escapeLike("a_b%c\\d")).isEqualTo("a\\_b\\%c\\\\d");
    }

    @Test
    void 검색은_클라이언트가_보낸_정렬_조건을_버린다() {
        Page<Profile> empty = new PageImpl<>(List.of());
        when(profileRepository.searchByNickname(any(), anyLong(), any())).thenReturn(empty);

        profileService.searchByNickname(1L, " bug ", PageRequest.of(2, 10, Sort.by("user.password")));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(profileRepository).searchByNickname(eq("bug"), eq(1L), captor.capture());
        assertThat(captor.getValue().getSort().isSorted()).isFalse();
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(10);
    }

    // ---------- helpers ----------

    private Profile profile(Long userId, String email, String nickname) {
        User user = new User(email, "hash");
        ReflectionTestUtils.setField(user, "id", userId);
        Profile profile = new Profile(user, nickname);
        ReflectionTestUtils.setField(profile, "userId", userId);
        return profile;
    }

    private Item item(Long id, ItemType type) {
        Item item = BeanUtils.instantiateClass(Item.class);
        ReflectionTestUtils.setField(item, "id", id);
        ReflectionTestUtils.setField(item, "itemType", type);
        return item;
    }

    private void own(Long userId, Item item, int quantity) {
        UserItem userItem = BeanUtils.instantiateClass(UserItem.class);
        ReflectionTestUtils.setField(userItem, "item", item);
        ReflectionTestUtils.setField(userItem, "quantity", quantity);
        when(userItemRepository.findByUserIdAndItemId(userId, item.getId())).thenReturn(Optional.of(userItem));
    }

    private void assertErrorCode(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
