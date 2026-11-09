package com.seyoung5309.executorserver.domain.auth.service;

import com.seyoung5309.executorserver.domain.auth.dto.LoginRequest;
import com.seyoung5309.executorserver.domain.auth.dto.LoginResponse;
import com.seyoung5309.executorserver.domain.auth.dto.SignupRequest;
import com.seyoung5309.executorserver.domain.auth.dto.SignupResponse;
import com.seyoung5309.executorserver.domain.auth.dto.TokenResponse;
import com.seyoung5309.executorserver.domain.auth.entity.User;
import com.seyoung5309.executorserver.domain.auth.repository.UserRepository;
import com.seyoung5309.executorserver.domain.profile.entity.Profile;
import com.seyoung5309.executorserver.domain.profile.repository.ProfileRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import com.seyoung5309.executorserver.global.security.JwtProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenStore refreshTokenStore;

    /** 없는 이메일로 로그인할 때도 비밀번호 비교를 한 번 수행해서 응답 시간으로 가입 여부를 알 수 없게 한다 */
    private final String dummyPasswordHash;

    public AuthService(UserRepository userRepository,
                       ProfileRepository profileRepository,
                       PasswordEncoder passwordEncoder,
                       JwtProvider jwtProvider,
                       RefreshTokenStore refreshTokenStore) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.refreshTokenStore = refreshTokenStore;
        this.dummyPasswordHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    /**
     * 회원가입. users와 profiles를 같은 트랜잭션에서 만든다.
     *
     * @throws BusinessException DUPLICATE_EMAIL, DUPLICATE_NICKNAME, INVALID_INPUT
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

    /**
     * 로그인. Access Token과 Refresh Token을 발급하고 Refresh Token을 Redis에 저장한다.
     * 이메일이 없을 때와 비밀번호가 틀릴 때 같은 오류를 내서 가입 여부를 알 수 없게 한다.
     *
     * @throws BusinessException INVALID_CREDENTIALS
     */
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email())).orElse(null);
        if (user == null) {
            passwordEncoder.matches(request.password(), dummyPasswordHash);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        Profile profile = profileRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalStateException("프로필이 없는 계정입니다: userId=" + user.getId()));

        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());
        refreshTokenStore.save(user.getId(), refreshToken, refreshTtl());

        return new LoginResponse(
                accessToken,
                refreshToken,
                jwtProvider.getAccessTokenValiditySeconds(),
                new LoginResponse.LoginUserInfo(
                        user.getId(),
                        profile.getNickname(),
                        user.getRole(),
                        profile.getCharacterName() != null
                )
        );
    }

    /**
     * 토큰 재발급. Refresh Token도 새 것으로 바꾸고(Rotation), 이전 Refresh Token은 쓸 수 없게 된다.
     *
     * @throws BusinessException INVALID_REFRESH_TOKEN (만료·위조·이미 사용·로그아웃·탈퇴한 경우)
     */
    public TokenResponse reissue(String refreshToken) {
        Long userId = jwtProvider.parseRefreshToken(refreshToken);

        // 권한(role)이 바뀌었을 수 있으므로 DB에서 다시 읽는다. 탈퇴한 계정이면 거부
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        String newRefreshToken = jwtProvider.createRefreshToken(userId);
        if (!refreshTokenStore.rotate(userId, refreshToken, newRefreshToken, refreshTtl())) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        return new TokenResponse(
                jwtProvider.createAccessToken(userId, user.getRole()),
                newRefreshToken,
                jwtProvider.getAccessTokenValiditySeconds()
        );
    }

    /**
     * 로그아웃. Refresh Token을 지운다.
     * 이미 발급된 Access Token은 만료(최대 30분)될 때까지 유효하므로 클라이언트에서도 버려야 한다.
     */
    public void logout(Long userId) {
        refreshTokenStore.delete(userId);
    }

    /** 이메일은 앞뒤 공백을 지우고 소문자로 저장·비교한다 */
    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private Duration refreshTtl() {
        return Duration.ofSeconds(jwtProvider.getRefreshTokenValiditySeconds());
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
