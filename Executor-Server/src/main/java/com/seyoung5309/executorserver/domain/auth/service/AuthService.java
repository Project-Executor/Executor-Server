package com.seyoung5309.executorserver.domain.auth.service;

import com.seyoung5309.executorserver.domain.auth.dto.SignupRequest;
import com.seyoung5309.executorserver.domain.auth.dto.SignupResponse;
import com.seyoung5309.executorserver.domain.auth.entity.User;
import com.seyoung5309.executorserver.domain.auth.repository.UserRepository;
import com.seyoung5309.executorserver.domain.profile.entity.Profile;
import com.seyoung5309.executorserver.domain.profile.repository.ProfileRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입. users와 profiles를 같은 트랜잭션에서 만든다.
     *
     * @throws BusinessException DUPLICATE_EMAIL, DUPLICATE_NICKNAME
     */
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        String email = normalizeEmail(request.email());
        String nickname = request.nickname().trim();
        if (nickname.length() < 2) {
            // "  a  "처럼 공백 때문에 길이 검사를 통과한 경우
            throw new BusinessException(ErrorCode.INVALID_INPUT, "닉네임은 2~20자여야 합니다.");
        }

        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (profileRepository.existsByNickname(nickname)) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        try {
            User user = userRepository.saveAndFlush(new User(email, passwordEncoder.encode(request.password())));
            profileRepository.saveAndFlush(new Profile(user, nickname));
            return new SignupResponse(user.getId(), email, nickname);
        } catch (DataIntegrityViolationException e) {
            // 위의 중복 확인과 저장 사이에 다른 요청이 같은 값으로 먼저 가입한 경우
            throw toDuplicateException(e);
        }
    }

    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(normalizeEmail(email));
    }

    /** 이메일은 앞뒤 공백을 지우고 소문자로 저장·비교한다 */
    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private BusinessException toDuplicateException(DataIntegrityViolationException e) {
        String message = String.valueOf(e.getMostSpecificCause().getMessage());
        if (message.contains("uk_profiles_nickname")) {
            return new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }
        if (message.contains("uk_users_email")) {
            return new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        throw e;
    }
}
