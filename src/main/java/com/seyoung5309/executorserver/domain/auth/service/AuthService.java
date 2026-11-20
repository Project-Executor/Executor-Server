package com.seyoung5309.executorserver.domain.auth.service;

import com.seyoung5309.executorserver.domain.auth.dto.LoginRequest;
import com.seyoung5309.executorserver.domain.auth.dto.LoginResponse;
import com.seyoung5309.executorserver.domain.auth.dto.SignupRequest;
import com.seyoung5309.executorserver.domain.auth.dto.SignupResponse;
import com.seyoung5309.executorserver.domain.auth.dto.TokenResponse;
import com.seyoung5309.executorserver.domain.auth.entity.Account;
import com.seyoung5309.executorserver.domain.auth.repository.AccountRepository;
import com.seyoung5309.executorserver.domain.profile.entity.User;
import com.seyoung5309.executorserver.domain.profile.repository.UserRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import com.seyoung5309.executorserver.global.security.JwtProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Service
@Transactional(readOnly = true)
public class AuthService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final Clock clock;

    /** 없는 이메일로 로그인할 때도 비밀번호 비교를 한 번 수행해서 응답 시간으로 가입 여부를 알 수 없게 한다 */
    private final String dummyPasswordHash;

    public AuthService(AccountRepository accountRepository,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtProvider jwtProvider,
                       RefreshTokenStore refreshTokenStore,
                       Clock clock) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.refreshTokenStore = refreshTokenStore;
        this.clock = clock;
        this.dummyPasswordHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    /**
     * 회원가입 (이메일 + 비밀번호). accounts와 users를 같은 트랜잭션에서 만든다.
     * 닉네임은 비워 두고, 첫 로그인 후 튜토리얼에서 PATCH /profiles/me로 정한다.
     *
     * @throws BusinessException DUPLICATE_EMAIL, INVALID_INPUT
     */
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        String email = normalizeEmail(request.email());
        if (accountRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        try {
            Account user = accountRepository.saveAndFlush(new Account(email, passwordEncoder.encode(request.password())));
            userRepository.saveAndFlush(new User(user));
            return new SignupResponse(user.getId(), email);
        } catch (DataIntegrityViolationException e) {
            // 위의 중복 확인과 저장 사이에 다른 요청이 같은 값으로 먼저 가입한 경우
            throw toDuplicateException(e);
        }
    }

    public boolean isEmailAvailable(String email) {
        return !accountRepository.existsByEmail(normalizeEmail(email));
    }

    /**
     * 로그인. Access Token과 Refresh Token을 발급하고 Refresh Token을 Redis에 저장한다.
     * 이메일이 없을 때와 비밀번호가 틀릴 때 같은 오류를 내서 가입 여부를 알 수 없게 한다.
     *
     * @throws BusinessException INVALID_CREDENTIALS, ACCOUNT_SUSPENDED
     */
    public LoginResponse login(LoginRequest request) {
        Account user = accountRepository.findByEmail(normalizeEmail(request.email())).orElse(null);
        if (user == null) {
            passwordEncoder.matches(request.password(), dummyPasswordHash);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        // 비밀번호가 맞을 때만 정지 여부를 알려 준다 (틀린 비밀번호로 정지 여부를 알아낼 수 없게)
        checkNotSuspended(user);

        User profile = userRepository.findById(user.getId())
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
                        profile.getNickname() != null
                )
        );
    }

    /**
     * 토큰 재발급. Refresh Token도 새 것으로 바꾸고(Rotation), 이전 Refresh Token은 쓸 수 없게 된다.
     *
     * @throws BusinessException INVALID_REFRESH_TOKEN (만료·위조·이미 사용·로그아웃·탈퇴한 경우), ACCOUNT_SUSPENDED
     */
    public TokenResponse reissue(String refreshToken) {
        Long userId = jwtProvider.parseRefreshToken(refreshToken);

        // 권한(role)이 바뀌었을 수 있으므로 DB에서 다시 읽는다. 탈퇴한 계정이면 거부
        Account user = accountRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        // 정지되면 다음 재발급부터 막힌다 (이미 받은 Access Token은 만료될 때까지, 최대 30분 유효)
        checkNotSuspended(user);

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

    /**
     * 회원 탈퇴. 비밀번호를 다시 확인한 뒤 accounts 행을 지운다.
     * 프로필·기록·아이템·친구·메시지·메일·제작 맵은 DB의 ON DELETE CASCADE로 함께 지워지고,
     * 신고 기록은 ON DELETE SET NULL로 남는다.
     *
     * @throws BusinessException INVALID_CREDENTIALS(비밀번호 불일치), UNAUTHORIZED(이미 없는 계정)
     */
    @Transactional
    public void withdraw(Long userId, String password) {
        Account user = accountRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        accountRepository.delete(user);
        accountRepository.flush();

        // Redis 삭제가 실패해도 탈퇴는 진행한다. 남은 Refresh Token은 재발급 시 계정이 없어서 거부된다.
        try {
            refreshTokenStore.delete(userId);
        } catch (RuntimeException e) {
            log.warn("탈퇴 후 Refresh Token 삭제 실패: userId={}", userId, e);
        }
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
        if (message.contains("uk_users_nickname")) {
            return new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }
        if (message.contains("uk_accounts_email")) {
            return new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        throw e;
    }

    private void checkNotSuspended(Account account) {
        if (account.isSuspendedAt(clock.instant())) {
            throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED, suspendedMessage(account));
        }
    }

    /** 예: "정지된 계정입니다. (2026-11-20 18:00까지, 한국 시간) 사유: 욕설" */
    static String suspendedMessage(Account account) {
        String period = Account.PERMANENT.equals(account.getSuspendedUntil())
                ? "영구 정지"
                : KST_MINUTE.format(account.getSuspendedUntil()) + "까지, 한국 시간";
        String reason = account.getSuspendReason() == null ? "" : " 사유: " + account.getSuspendReason();
        return "정지된 계정입니다. (" + period + ")" + reason;
    }

    private static final DateTimeFormatter KST_MINUTE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("Asia/Seoul"));
}
