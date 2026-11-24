package com.seyoung5309.executorserver.domain.profile.service;

import com.seyoung5309.executorserver.domain.profile.entity.UserAvatar;
import com.seyoung5309.executorserver.domain.profile.repository.UserAvatarRepository;
import com.seyoung5309.executorserver.domain.profile.repository.UserRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AvatarServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-24T03:00:00Z");
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a, 1, 2, 3};
    private static final byte[] JPEG = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe0, 1, 2};

    private UserAvatarRepository avatarRepository;
    private UserRepository userRepository;
    private AvatarService service;

    @BeforeEach
    void setUp() {
        avatarRepository = mock(UserAvatarRepository.class);
        userRepository = mock(UserRepository.class);
        service = new AvatarService(avatarRepository, userRepository, Clock.fixed(NOW, ZoneOffset.UTC));
        when(userRepository.existsById(1L)).thenReturn(true);
    }

    @Test
    void 처음_올리면_새로_저장하고_지금_시각을_버전으로_돌려준다() {
        when(avatarRepository.findById(1L)).thenReturn(Optional.empty());

        Instant version = service.upload(1L, Base64.getEncoder().encodeToString(PNG));

        ArgumentCaptor<UserAvatar> saved = ArgumentCaptor.forClass(UserAvatar.class);
        verify(avatarRepository).save(saved.capture());
        assertThat(saved.getValue().getImage()).isEqualTo(PNG);
        assertThat(saved.getValue().getContentType()).isEqualTo("image/png");
        assertThat(version).isEqualTo(NOW);
    }

    @Test
    void 이미_있으면_그림과_시각을_바꾼다() {
        UserAvatar old = new UserAvatar(1L, PNG, "image/png", Instant.parse("2026-11-01T00:00:00Z"));
        when(avatarRepository.findById(1L)).thenReturn(Optional.of(old));

        service.upload(1L, "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(JPEG));

        assertThat(old.getImage()).isEqualTo(JPEG);
        assertThat(old.getContentType()).isEqualTo("image/jpeg");
        assertThat(old.getUpdatedAt()).isEqualTo(NOW);
        verify(avatarRepository, never()).save(any());
    }

    @Test
    void PNG나_JPEG가_아니면_거절한다() {
        String gif = Base64.getEncoder().encodeToString("GIF89a....".getBytes());
        assertError(() -> service.upload(1L, gif), "PNG 또는 JPEG");
    }

    @Test
    void Base64가_아니면_거절한다() {
        assertError(() -> service.upload(1L, "!!not-base64!!"), "Base64");
    }

    @Test
    void 크기가_512KB를_넘으면_거절한다() {
        byte[] big = new byte[AvatarService.MAX_BYTES + 1];
        System.arraycopy(PNG, 0, big, 0, PNG.length);
        assertError(() -> service.upload(1L, Base64.getEncoder().encodeToString(big)), "512KB");
    }

    @Test
    void 사진이_없으면_조회는_NOT_FOUND이고_버전은_null() {
        when(avatarRepository.findById(2L)).thenReturn(Optional.empty());
        when(avatarRepository.findUpdatedAt(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(2L)).isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);
        assertThat(service.getVersion(2L)).isNull();
    }

    @Test
    void 지우기는_사진이_없어도_오류가_아니다() {
        when(avatarRepository.findById(1L)).thenReturn(Optional.empty());

        service.delete(1L);

        verify(avatarRepository, never()).delete(any());
    }

    private static void assertError(Runnable call, String message) {
        assertThatThrownBy(call::run).isInstanceOf(BusinessException.class)
                .hasMessageContaining(message)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
    }
}
