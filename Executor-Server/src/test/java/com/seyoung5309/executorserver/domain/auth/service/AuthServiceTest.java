package com.seyoung5309.executorserver.domain.auth.service;

import com.seyoung5309.executorserver.domain.auth.dto.SignupRequest;
import com.seyoung5309.executorserver.domain.auth.dto.SignupResponse;
import com.seyoung5309.executorserver.domain.auth.entity.Role;
import com.seyoung5309.executorserver.domain.auth.entity.User;
import com.seyoung5309.executorserver.domain.auth.repository.UserRepository;
import com.seyoung5309.executorserver.domain.profile.entity.Profile;
import com.seyoung5309.executorserver.domain.profile.repository.ProfileRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.SQLIntegrityConstraintViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepository;
    private ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        profileRepository = mock(ProfileRepository.class);
        authService = new AuthService(userRepository, profileRepository, passwordEncoder);

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
