package com.seyoung5309.executorserver.domain.admin.service;

import com.seyoung5309.executorserver.domain.admin.dto.AdminCoinResponse;
import com.seyoung5309.executorserver.domain.admin.dto.AdminUserDetailResponse;
import com.seyoung5309.executorserver.domain.admin.dto.AdminUserResponse;
import com.seyoung5309.executorserver.domain.auth.entity.Account;
import com.seyoung5309.executorserver.domain.auth.entity.Role;
import com.seyoung5309.executorserver.domain.item.repository.UserItemRepository;
import com.seyoung5309.executorserver.domain.profile.entity.User;
import com.seyoung5309.executorserver.domain.profile.repository.UserRepository;
import com.seyoung5309.executorserver.domain.stage.repository.StarSummary;
import com.seyoung5309.executorserver.domain.stage.repository.UserStageRepository;
import com.seyoung5309.executorserver.domain.profile.service.ProfileService;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import com.seyoung5309.executorserver.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** 관리자 유저 조회·관리 (상세, 코인 조정, 권한 변경) */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    /** 최근 가입순 */
    static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "userId");

    /** ID로 한 번에 조회할 수 있는 수 */
    public static final int LOOKUP_MAX = 100;

    private final UserRepository userRepository;
    private final UserItemRepository userItemRepository;
    private final UserStageRepository userStageRepository;

    /**
     * 유저 상세 (계정·코인·장착·공식 스테이지 기록·보유 아이템).
     *
     * @throws BusinessException NOT_FOUND
     */
    public AdminUserDetailResponse getDetail(Long userId) {
        User user = findUser(userId);
        StarSummary stars = userStageRepository.summarizeOfficialStars(userId);
        List<AdminUserDetailResponse.OwnedItem> items = userItemRepository.findMyItems(userId, null).stream()
                .map(AdminUserDetailResponse.OwnedItem::from)
                .toList();
        Account account = user.getAccount();
        return new AdminUserDetailResponse(
                user.getUserId(), account.getEmail(), user.getNickname(), user.getComment(), account.getRole(),
                user.getCoin(), account.getCreatedAt(), user.getEquippedSkinId(), user.getEquippedThemeId(),
                stars.clearedCount(), stars.totalStars(), items);
    }

    /**
     * 코인을 더하거나 뺀다. DB에서 바로 계산해서 동시에 코인을 쓰거나 받아도 값이 유실되지 않는다.
     *
     * @param amount 0이 아닌 값. 음수면 뺀다
     * @throws BusinessException NOT_FOUND, INVALID_INPUT(0), NOT_ENOUGH_COIN(빼면 0 미만)
     */
    @Transactional
    public AdminCoinResponse adjustCoin(Long userId, int amount) {
        if (amount == 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "amount는 0이 아니어야 합니다.");
        }
        if (!userRepository.existsById(userId)) {
            throw notFound();
        }
        if (amount > 0) {
            userRepository.addCoin(userId, amount);
        } else if (userRepository.spendCoin(userId, -amount) == 0) {
            int coin = userRepository.findCoinByUserId(userId).orElse(0);
            throw new BusinessException(ErrorCode.NOT_ENOUGH_COIN,
                    "코인이 부족합니다. (보유 " + coin + ", 빼려는 코인 " + (-amount) + ")");
        }
        return new AdminCoinResponse(userId, userRepository.findCoinByUserId(userId).orElseThrow(this::notFound));
    }

    /**
     * 권한을 바꾼다. 관리자 자신의 권한은 바꿀 수 없다 (실수로 관리자 웹에 못 들어오게 되는 것을 막는다).
     * 새 권한은 그 사용자의 다음 토큰 재발급(최대 30분 뒤)부터 적용된다.
     *
     * @throws BusinessException NOT_FOUND, INVALID_INPUT(자기 자신)
     */
    @Transactional
    public AdminUserResponse changeRole(Long adminId, Long userId, Role role) {
        if (Objects.equals(adminId, userId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "자기 자신의 권한은 바꿀 수 없습니다.");
        }
        User user = findUser(userId);
        user.getAccount().changeRole(role);
        return AdminUserResponse.from(user);
    }

    private User findUser(Long userId) {
        return userRepository.findWithUserById(userId).orElseThrow(this::notFound);
    }

    private BusinessException notFound() {
        return new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다.");
    }

    /**
     * 유저 목록 (최근 가입순). 나 자신(관리자)도 포함한다.
     *
     * @param keyword 닉네임·이메일 일부, 또는 사용자 ID. 비어 있으면 전체
     * @param role    null이면 전체
     */
    public PageResponse<AdminUserResponse> search(String keyword, Role role, int page, int size) {
        String k = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return PageResponse.of(
                userRepository.searchForAdmin(
                        k == null ? null : ProfileService.escapeLike(k),
                        k == null ? null : parseId(k),
                        role,
                        PageRequest.of(page, size, NEWEST_FIRST)),
                AdminUserResponse::from
        );
    }

    /**
     * 여러 ID의 사용자 (ID 순). 없는 ID는 결과에서 빠진다. 중복 ID는 한 번만 센다.
     *
     * @throws BusinessException INVALID_INPUT (비어 있음, LOOKUP_MAX개 초과)
     */
    public List<AdminUserResponse> lookup(List<Long> ids) {
        Set<Long> unique = ids == null ? Set.of()
                : ids.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        if (unique.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "ids를 입력해 주세요.");
        }
        if (unique.size() > LOOKUP_MAX) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "ids는 한 번에 " + LOOKUP_MAX + "개까지 조회할 수 있습니다.");
        }
        return userRepository.findAllWithAccountByIdIn(unique).stream().map(AdminUserResponse::from).toList();
    }

    /** 권한 문자열을 읽는다 (대소문자 무시). 비어 있으면 null(전체), 잘못된 값이면 INVALID_INPUT */
    public static Role parseRole(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Role.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "role은 USER, ADMIN 중 하나여야 합니다.");
        }
    }

    /** 검색어가 양의 정수면 ID로도 찾는다 */
    static Long parseId(String keyword) {
        if (!keyword.matches("\\d{1,18}")) {
            return null;
        }
        long id = Long.parseLong(keyword);
        return id > 0 ? id : null;
    }
}
