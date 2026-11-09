package com.seyoung5309.executorserver.domain.auth.service;

import com.seyoung5309.executorserver.domain.auth.dto.LoginRequest;
import com.seyoung5309.executorserver.domain.auth.dto.LoginResponse;
import com.seyoung5309.executorserver.domain.auth.dto.SignupRequest;
import com.seyoung5309.executorserver.domain.auth.dto.SignupResponse;
import com.seyoung5309.executorserver.domain.auth.dto.TokenResponse;
import com.seyoung5309.executorserver.domain.auth.entity.Role;
import com.seyoung5309.executorserver.domain.auth.entity.User;
import com.seyoung5309.executorserver.domain.auth.repository.UserRepository;
import com.seyoung5309.executorserver.domain.profile.entity.Profile;
import com.seyoung5309.executorserver.domain.profile.repository.ProfileRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import com.seyoung5309.executorserver.global.security.JwtProperties;
import com.seyoung5309.executorserver.global.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.SQLIntegrityConstraintViolationException;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private static final String SECRET = "dGVzdC1vbmx5LWp3dC1zZWNyZXQta2V5LWZvci1leGVjdXRvci1zZXJ2ZXItdGVzdHM=";

    private UserRepository userRepository;
    private ProfileRepository profileRepository;
    private RefreshTokenStore refreshTokenStore;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtProvider jwtProvider = new JwtProvider(new JwtProperties(SECRET, 1800, 1_209_600));
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        profileRepository = mock(ProfileRepository.class);
        refreshTokenStore = mock(RefreshTokenStore.class);
        authService = new AuthService(userRepository, profileRepository, passwordEncoder, jwtProvider, refreshTokenStore);

        // 저장하면 DB가 ID를 채워주는 것처럼 흉내낸다
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 1L);
            return user;
        });
        when(profileRepository.saveAndFlush(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void 가입하면_이메일은_소문자로_비밀번호는_해시로_저장되고_프로필이_함께_생긴다() {
        SignupResponse response = authService.signup(new SignupRequest(" Executor@Example.COM ", "password123!", " debugger "));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("executor@example.com");
        assertThat(saved.getPassword()).isNotEqualTo("password123!");
        assertThat(passwordEncoder.matches("password123!", saved.getPassword())).isTrue();
        assertThat(saved.getRole()).isEqualTo(Role.USER);

        ArgumentCaptor<Profile> profileCaptor = ArgumentCaptor.forClass(Profile.class);
        verify(profileRepository).saveAndFlush(profileCaptor.capture());
        assertThat(profileCaptor.getValue().getNickname()).isEqualTo("debugger");
        assertThat(profileCaptor.getValue().getCoin()).isZero();

        assertThat(response).isEqualTo(new SignupResponse(1L, "executor@example.com", "debugger"));
    }

    @Test
    void 이미_가입된_이메일이면_DUPLICATE_EMAIL() {
        when(userRepository.existsByEmail("executor@example.com")).thenReturn(true);

        assertErrorCode(() -> authService.signup(new SignupRequest("EXECUTOR@example.com", "password123!", "debugger")),
                ErrorCode.DUPLICATE_EMAIL);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void 이미_쓰는_닉네임이면_DUPLICATE_NICKNAME() {
        when(profileRepository.existsByNickname("debugger")).thenReturn(true);

        assertErrorCode(() -> authService.signup(new SignupRequest("a@b.com", "password123!", "debugger")),
                ErrorCode.DUPLICATE_NICKNAME);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void 공백을_지우면_한_글자인_닉네임은_INVALID_INPUT() {
        assertErrorCode(() -> authService.signup(new SignupRequest("a@b.com", "password123!", "  a  ")),
                ErrorCode.INVALID_INPUT);
    }

    @Test
    void 동시에_같은_닉네임으로_가입해서_DB_제약에_걸리면_DUPLICATE_NICKNAME() {
        when(profileRepository.saveAndFlush(any(Profile.class))).thenThrow(uniqueViolation("uk_profiles_nickname"));

        assertErrorCode(() -> authService.signup(new SignupRequest("a@b.com", "password123!", "debugger")),
                ErrorCode.DUPLICATE_NICKNAME);
    }

    @Test
    void 동시에_같은_이메일로_가입해서_DB_제약에_걸리면_DUPLICATE_EMAIL() {
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(uniqueViolation("uk_users_email"));

        assertErrorCode(() -> authService.signup(new SignupRequest("a@b.com", "password123!", "debugger")),
                ErrorCode.DUPLICATE_EMAIL);
    }

    @Test
    void 이메일_중복_확인은_대소문자와_공백을_무시한다() {
        when(userRepository.existsByEmail("executor@example.com")).thenReturn(true);

        assertThat(authService.isEmailAvailable("  EXECUTOR@example.com ")).isFalse();
        assertThat(authService.isEmailAvailable("new@example.com")).isTrue();
    }

    // ---------- 로그인 ----------

    @Test
    void 로그인하면_두_토큰을_발급하고_Refresh_Token을_14일_TTL로_저장한다() {
        User user = existingUser(5L, "executor@example.com", "password123!");
        Profile profile = new Profile(user, "debugger");
        when(profileRepository.findById(5L)).thenReturn(Optional.of(profile));

        LoginResponse response = authService.login(new LoginRequest(" EXECUTOR@example.com", "password123!"));

        assertThat(jwtProvider.parseAccessToken(response.accessToken()).userId()).isEqualTo(5L);
        assertThat(jwtProvider.parseRefreshToken(response.refreshToken())).isEqualTo(5L);
        assertThat(response.accessTokenExpiresIn()).isEqualTo(1800);
        assertThat(response.user()).isEqualTo(new LoginResponse.LoginUserInfo(5L, "debugger", Role.USER, false));
        verify(refreshTokenStore).save(5L, response.refreshToken(), Duration.ofDays(14));
    }

    @Test
    void 집행자_이름을_입력했으면_hasCharacterName이_true() {
        User user = existingUser(5L, "executor@example.com", "password123!");
        Profile profile = new Profile(user, "debugger");
        ReflectionTestUtils.setField(profile, "characterName", "오집행");
        when(profileRepository.findById(5L)).thenReturn(Optional.of(profile));

        LoginResponse response = authService.login(new LoginRequest("executor@example.com", "password123!"));

        assertThat(response.user().hasCharacterName()).isTrue();
    }

    @Test
    void 비밀번호가_틀리면_INVALID_CREDENTIALS() {
        existingUser(5L, "executor@example.com", "password123!");

        assertErrorCode(() -> authService.login(new LoginRequest("executor@example.com", "wrong-password")),
                ErrorCode.INVALID_CREDENTIALS);
        verify(refreshTokenStore, never()).save(any(), any(), any());
    }

    @Test
    void 없는_이메일도_같은_INVALID_CREDENTIALS() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertErrorCode(() -> authService.login(new LoginRequest("nobody@example.com", "password123!")),
                ErrorCode.INVALID_CREDENTIALS);
    }

    // ---------- 재발급 ----------

    @Test
    void 재발급하면_저장된_Refresh_Token을_새_것으로_바꾸고_두_토큰을_돌려준다() {
        existingUser(5L, "executor@example.com", "password123!");
        String oldRefresh = jwtProvider.createRefreshToken(5L);
        when(refreshTokenStore.rotate(eq(5L), eq(oldRefresh), anyString(), eq(Duration.ofDays(14)))).thenReturn(true);

        TokenResponse response = authService.reissue(oldRefresh);

        assertThat(response.refreshToken()).isNotEqualTo(oldRefresh);
        assertThat(jwtProvider.parseAccessToken(response.accessToken()).userId()).isEqualTo(5L);
        verify(refreshTokenStore).rotate(5L, oldRefresh, response.refreshToken(), Duration.ofDays(14));
    }

    @Test
    void 이미_사용했거나_로그아웃한_Refresh_Token이면_INVALID_REFRESH_TOKEN() {
        existingUser(5L, "executor@example.com", "password123!");
        String usedRefresh = jwtProvider.createRefreshToken(5L);
        when(refreshTokenStore.rotate(anyLong(), anyString(), anyString(), any())).thenReturn(false);

        assertErrorCode(() -> authService.reissue(usedRefresh), ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    void 탈퇴한_계정의_Refresh_Token이면_INVALID_REFRESH_TOKEN() {
        when(userRepository.findById(5L)).thenReturn(Optional.empty());

        assertErrorCode(() -> authService.reissue(jwtProvider.createRefreshToken(5L)), ErrorCode.INVALID_REFRESH_TOKEN);
        verify(refreshTokenStore, never()).rotate(anyLong(), anyString(), anyString(), any());
    }

    @Test
    void Access_Token으로_재발급하면_INVALID_REFRESH_TOKEN() {
        String access = jwtProvider.createAccessToken(5L, Role.USER);

        assertErrorCode(() -> authService.reissue(access), ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    void 로그아웃하면_Refresh_Token을_지운다() {
        authService.logout(5L);

        verify(refreshTokenStore).delete(5L);
    }

    // ---------- 탈퇴 ----------

    @Test
    void 비밀번호가_맞으면_계정을_지우고_Refresh_Token도_지운다() {
        User user = existingUser(5L, "executor@example.com", "password123!");

        authService.withdraw(5L, "password123!");

        verify(userRepository).delete(user);
        verify(refreshTokenStore).delete(5L);
    }

    @Test
    void 탈퇴할_때_비밀번호가_틀리면_INVALID_CREDENTIALS이고_아무것도_지우지_않는다() {
        existingUser(5L, "executor@example.com", "password123!");

        assertErrorCode(() -> authService.withdraw(5L, "wrong-password"), ErrorCode.INVALID_CREDENTIALS);
        verify(userRepository, never()).delete(any());
        verify(refreshTokenStore, never()).delete(any());
    }

    @Test
    void 이미_없는_계정이면_UNAUTHORIZED() {
        when(userRepository.findById(5L)).thenReturn(Optional.empty());

        assertErrorCode(() -> authService.withdraw(5L, "password123!"), ErrorCode.UNAUTHORIZED);
    }

    @Test
    void Redis_삭제가_실패해도_탈퇴는_진행된다() {
        User user = existingUser(5L, "executor@example.com", "password123!");
        doThrow(new RuntimeException("redis down")).when(refreshTokenStore).delete(5L);

        authService.withdraw(5L, "password123!");

        verify(userRepository).delete(user);
    }

    private User existingUser(Long id, String email, String rawPassword) {
        User user = new User(email, passwordEncoder.encode(rawPassword));
        ReflectionTestUtils.setField(user, "id", id);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        return user;
    }

    private DataIntegrityViolationException uniqueViolation(String constraint) {
        return new DataIntegrityViolationException("duplicate",
                new SQLIntegrityConstraintViolationException("Duplicate entry 'x' for key '" + constraint + "'"));
    }

    private void assertErrorCode(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
