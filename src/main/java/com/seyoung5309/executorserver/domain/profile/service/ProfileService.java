package com.seyoung5309.executorserver.domain.profile.service;

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
import com.seyoung5309.executorserver.global.response.PageResponse;
import com.seyoung5309.executorserver.global.response.UserSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserItemRepository userItemRepository;
    private final UserStageRepository userStageRepository;
    private final FriendRepository friendRepository;

    public boolean isNicknameAvailable(String nickname) {
        return !profileRepository.existsByNickname(nickname.trim());
    }

    public MyProfileResponse getMyProfile(Long userId) {
        return MyProfileResponse.from(findMyProfile(userId));
    }

    /**
     * 보낸 필드만 수정한다.
     *
     * @throws BusinessException DUPLICATE_NICKNAME, INVALID_INPUT
     */
    @Transactional
    public MyProfileResponse updateMyProfile(Long userId, UpdateProfileRequest request) {
        Profile profile = findMyProfile(userId);

        if (request.nickname() != null) {
            String nickname = request.nickname().trim();
            if (nickname.length() < 2) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "닉네임은 2~20자여야 합니다.");
            }
            if (!nickname.equals(profile.getNickname())) {
                if (profileRepository.existsByNickname(nickname)) {
                    throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
                }
                profile.changeNickname(nickname);
            }
        }

        if (request.characterName() != null) {
            String characterName = request.characterName().trim();
            if (characterName.isEmpty()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "집행자 이름은 1~20자여야 합니다.");
            }
            profile.changeCharacterName(characterName);
        }

        if (request.comment() != null) {
            String comment = request.comment().trim();
            profile.changeComment(comment.isEmpty() ? null : comment);
        }

        try {
            profileRepository.flush();
        } catch (DataIntegrityViolationException e) {
            // 중복 확인과 저장 사이에 다른 사용자가 같은 닉네임을 먼저 가져간 경우
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }
        return MyProfileResponse.from(profile);
    }

    /**
     * 스킨·테마 장착. null이면 해제한다.
     *
     * @throws BusinessException FORBIDDEN(보유하지 않은 아이템), INVALID_INPUT(종류가 맞지 않음)
     */
    @Transactional
    public EquipmentResponse updateEquipment(Long userId, EquipmentRequest request) {
        Profile profile = findMyProfile(userId);

        profile.equipSkin(findOwnedItem(userId, request.skinItemId(), ItemType.SKIN));
        profile.equipTheme(findOwnedItem(userId, request.themeItemId(), ItemType.THEME));

        return new EquipmentResponse(profile.getEquippedSkinId(), profile.getEquippedThemeId());
    }

    /**
     * 다른 사용자의 공개 프로필. 내 ID를 넣으면 내 공개 프로필이 나온다(isFriend = false).
     *
     * @throws BusinessException NOT_FOUND
     */
    public ProfileDetailResponse getProfile(Long viewerId, Long targetUserId) {
        Profile profile = profileRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        StarSummary stars = userStageRepository.summarizeOfficialStars(targetUserId);

        return new ProfileDetailResponse(
                profile.getUserId(),
                profile.getNickname(),
                profile.getComment(),
                profile.getEquippedSkinId(),
                stars.clearedCount(),
                stars.totalStars(),
                friendRepository.areFriends(viewerId, targetUserId)
        );
    }

    /**
     * 닉네임에 검색어가 포함된 사용자 (나 자신 제외, 닉네임 오름차순).
     * 클라이언트가 보낸 sort는 무시한다. (sort=user.password 같은 정렬로 정보가 새는 것을 막기 위함)
     */
    public PageResponse<UserSummary> searchByNickname(Long viewerId, String keyword, Pageable pageable) {
        Pageable pageOnly = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return PageResponse.of(
                profileRepository.searchByNickname(escapeLike(keyword.trim()), viewerId, pageOnly),
                UserSummary::from
        );
    }

    private Profile findMyProfile(Long userId) {
        // 토큰은 유효하지만 탈퇴로 프로필이 없는 경우
        return profileRepository.findWithUserById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    /** itemId가 null이면 null(장착 해제). 아니면 보유 중이고 종류가 맞는 아이템을 돌려준다. */
    private Item findOwnedItem(Long userId, Long itemId, ItemType expectedType) {
        if (itemId == null) {
            return null;
        }
        UserItem owned = userItemRepository.findByUserIdAndItemId(userId, itemId)
                .filter(userItem -> userItem.getQuantity() > 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "보유하지 않은 아이템입니다."));
        Item item = owned.getItem();
        if (item.getItemType() != expectedType) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    expectedType + " 자리에 " + item.getItemType() + " 아이템을 장착할 수 없습니다.");
        }
        return item;
    }

    /** LIKE 검색에서 %, _, \ 를 글자 그대로 찾도록 이스케이프한다 */
    static String escapeLike(String keyword) {
        return keyword
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
