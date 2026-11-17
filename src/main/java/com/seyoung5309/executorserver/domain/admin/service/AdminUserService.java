package com.seyoung5309.executorserver.domain.admin.service;

import com.seyoung5309.executorserver.domain.admin.dto.AdminUserResponse;
import com.seyoung5309.executorserver.domain.auth.entity.Role;
import com.seyoung5309.executorserver.domain.profile.repository.UserRepository;
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

/** 관리자 유저 조회 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    /** 최근 가입순 */
    static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "userId");

    /** ID로 한 번에 조회할 수 있는 수 */
    public static final int LOOKUP_MAX = 100;

    private final UserRepository userRepository;

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
