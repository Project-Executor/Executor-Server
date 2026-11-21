package com.seyoung5309.executorserver.domain.admin.service;

import com.seyoung5309.executorserver.domain.admin.dto.AdminCharacterRequest;
import com.seyoung5309.executorserver.domain.character.entity.GameCharacter;
import com.seyoung5309.executorserver.domain.character.repository.GameCharacterRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminCharacterServiceTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private GameCharacterRepository repository;
    private AdminCharacterService service;

    @BeforeEach
    void setUp() {
        repository = mock(GameCharacterRepository.class);
        service = new AdminCharacterService(repository);
    }

    private static void assertErrorCode(Runnable call, ErrorCode code) {
        assertThatThrownBy(call::run).isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(code);
    }

    @Test
    void 등록은_공백을_지우고_색을_대문자로_저장한다() {
        var res = service.create(new AdminCharacterRequest(" delta ", " 델타 ", " #ff9a3c ", null));

        assertThat(res.characterKey()).isEqualTo("delta");
        assertThat(res.name()).isEqualTo("델타");
        assertThat(res.tagColor()).isEqualTo("#FF9A3C");
        assertThat(res.protagonist()).isFalse();
        verify(repository).saveAndFlush(any());
    }

    @Test
    void ID나_이름이_겹치면_DUPLICATE_VALUE() {
        when(repository.existsByCharacterKeyAndIdNot("cassia", 0L)).thenReturn(true);
        assertErrorCode(() -> service.create(new AdminCharacterRequest("cassia", "카시아2", "#FF4FA3", false)), ErrorCode.DUPLICATE_VALUE);

        when(repository.existsByNameAndIdNot("카시아", 0L)).thenReturn(true);
        assertErrorCode(() -> service.create(new AdminCharacterRequest("cassia2", "카시아", "#FF4FA3", false)), ErrorCode.DUPLICATE_VALUE);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void 수정은_자기_자신과는_겹쳐도_된다() {
        GameCharacter saved = new GameCharacter("player", "집행자", "#D4D6DE", true);
        ReflectionTestUtils.setField(saved, "id", 1L);
        when(repository.findById(1L)).thenReturn(Optional.of(saved));

        var res = service.update(1L, new AdminCharacterRequest("player", "집행자", "#aabbcc", true));

        verify(repository).existsByNameAndIdNot("집행자", 1L);
        assertThat(res.tagColor()).isEqualTo("#AABBCC");
        assertThat(saved.isProtagonist()).isTrue();
    }

    @Test
    void 없는_인물은_NOT_FOUND() {
        when(repository.findById(9L)).thenReturn(Optional.empty());
        assertErrorCode(() -> service.delete(9L), ErrorCode.NOT_FOUND);
    }

    @Test
    void 색과_ID_형식을_검사한다() {
        assertThat(VALIDATOR.validate(new AdminCharacterRequest("delta", "델타", "#FF9A3C", false))).isEmpty();
        assertThat(VALIDATOR.validate(new AdminCharacterRequest("delta", "델타", "FF9A3C", false))).hasSize(1);
        assertThat(VALIDATOR.validate(new AdminCharacterRequest("delta", "델타", "#FF9A3", false))).hasSize(1);
        assertThat(VALIDATOR.validate(new AdminCharacterRequest("Delta", "델타", "#FF9A3C", false))).hasSize(1);
        assertThat(VALIDATOR.validate(new AdminCharacterRequest("del ta", "델타", "#FF9A3C", false))).hasSize(1);
    }
}
