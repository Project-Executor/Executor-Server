package com.seyoung5309.executorserver.domain.auth.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 가입 비밀번호: 8~64자, 영문과 숫자를 함께 */
class SignupRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private boolean valid(String password) {
        return validator.validate(new SignupRequest("a@b.com", password)).isEmpty();
    }

    @Test
    void 영문과_숫자가_함께_있으면_통과한다() {
        assertThat(valid("abcd1234")).isTrue();
        assertThat(valid("Password123!")).isTrue();
    }

    @Test
    void 영문만_또는_숫자만이면_거절한다() {
        assertThat(valid("abcdefgh")).isFalse();
        assertThat(valid("12345678")).isFalse();
    }

    @Test
    void 여덟_자보다_짧으면_거절한다() {
        assertThat(valid("ab12")).isFalse();
    }
}
