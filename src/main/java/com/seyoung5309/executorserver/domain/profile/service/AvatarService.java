package com.seyoung5309.executorserver.domain.profile.service;

import com.seyoung5309.executorserver.domain.profile.entity.UserAvatar;
import com.seyoung5309.executorserver.domain.profile.repository.UserAvatarRepository;
import com.seyoung5309.executorserver.domain.profile.repository.UserRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Base64;

/**
 * 프로필 사진. 게임이 자른 그림(PNG/JPEG)을 그대로 저장하고 돌려준다.
 * 그림 내용은 검사하지 않고 파일 머리(시그니처)와 크기만 본다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AvatarService {

    /** 디코딩한 그림 최대 크기 (256x256 PNG는 보통 30~150KB) */
    static final int MAX_BYTES = 512 * 1024;

    private final UserAvatarRepository avatarRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    /** @return 사진이 있으면 바꾼 시각(게임의 캐시 버전), 없으면 null */
    public Instant getVersion(Long userId) {
        return avatarRepository.findUpdatedAt(userId).orElse(null);
    }

    /**
     * 사진을 올린다 (있으면 바꾼다).
     *
     * @return 새 버전 (바꾼 시각)
     * @throws BusinessException INVALID_INPUT(Base64가 아님, PNG·JPEG가 아님, 512KB 초과), NOT_FOUND(프로필 없음)
     */
    @Transactional
    public Instant upload(Long userId, String base64) {
        byte[] image = decode(base64);
        String contentType = contentType(image);
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "프로필을 찾을 수 없습니다.");
        }
        Instant now = clock.instant();
        avatarRepository.findById(userId).ifPresentOrElse(
                avatar -> avatar.change(image, contentType, now),
                () -> avatarRepository.save(new UserAvatar(userId, image, contentType, now)));
        return now;
    }

    /** 사진을 지운다 (기본 사진으로). 없어도 오류가 아니다 */
    @Transactional
    public void delete(Long userId) {
        avatarRepository.findById(userId).ifPresent(avatarRepository::delete);
    }

    /** @throws BusinessException NOT_FOUND(사진 없음) */
    public UserAvatar get(Long userId) {
        return avatarRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "프로필 사진이 없습니다."));
    }

    static byte[] decode(String base64) {
        String data = base64.strip();
        int comma = data.indexOf(',');
        if (data.startsWith("data:") && comma > 0) {
            data = data.substring(comma + 1); // data:image/png;base64,... 형식도 받는다
        }
        // 원본 크기를 먼저 어림해서 아주 큰 입력은 디코딩 전에 거절한다
        if ((long) data.length() * 3 / 4 > MAX_BYTES + 3) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "사진은 512KB 이하여야 합니다.");
        }
        byte[] image;
        try {
            image = Base64.getDecoder().decode(data);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "image는 Base64 문자열이어야 합니다.");
        }
        if (image.length > MAX_BYTES) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "사진은 512KB 이하여야 합니다.");
        }
        return image;
    }

    /** 파일 머리로 종류를 정한다. PNG·JPEG가 아니면 거절 */
    static String contentType(byte[] image) {
        if (image.length >= 8 && (image[0] & 0xff) == 0x89 && image[1] == 'P' && image[2] == 'N' && image[3] == 'G') {
            return "image/png";
        }
        if (image.length >= 3 && (image[0] & 0xff) == 0xff && (image[1] & 0xff) == 0xd8 && (image[2] & 0xff) == 0xff) {
            return "image/jpeg";
        }
        throw new BusinessException(ErrorCode.INVALID_INPUT, "사진은 PNG 또는 JPEG여야 합니다.");
    }
}
