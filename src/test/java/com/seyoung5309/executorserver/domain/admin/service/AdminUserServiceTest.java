package com.seyoung5309.executorserver.domain.admin.service;

import com.seyoung5309.executorserver.domain.auth.entity.Account;
import com.seyoung5309.executorserver.domain.auth.entity.Role;
import com.seyoung5309.executorserver.domain.profile.entity.User;
import com.seyoung5309.executorserver.domain.profile.repository.UserRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static com.seyoung5309.executorserver.domain.stage.StageFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminUserServiceTest {

    private UserRepository userRepository;
    private AdminUserService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        service = new AdminUserService(userRepository);
        when(userRepository.searchForAdmin(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
    }

    @Test
    void 검색어가_없으면_조건_없이_최근_가입순으로_찾는다() {
        service.search("  ", null, 0, 20);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).searchForAdmin(isNull(), isNull(), isNull(), pageable.capture());
        assertThat(pageable.getValue().getSort()).isEqualTo(AdminUserService.NEWEST_FIRST);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void 숫자_검색어는_ID로도_찾는다() {
        service.search(" 42 ", Role.ADMIN, 1, 10);

        verify(userRepository).searchForAdmin(eq("42"), eq(42L), eq(Role.ADMIN), any());
    }

    @Test
    void 문자_검색어는_LIKE_이스케이프하고_ID로는_찾지_않는다() {
        service.search("a_b%", null, 0, 20);

        verify(userRepository).searchForAdmin(eq("a\\_b\\%"), isNull(), isNull(), any());
    }

    @Test
    void 응답에_이메일과_권한이_들어간다() {
        Account account = user(7L);
        ReflectionTestUtils.setField(account, "role", Role.ADMIN);
        User profile = new User(account, "운영자");
        ReflectionTestUtils.setField(profile, "userId", 7L);
        when(userRepository.searchForAdmin(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(profile)));

        var item = service.search(null, null, 0, 20).content().get(0);

        assertThat(item.userId()).isEqualTo(7L);
        assertThat(item.email()).isEqualTo(account.getEmail());
        assertThat(item.nickname()).isEqualTo("운영자");
        assertThat(item.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void role은_대소문자를_무시하고_잘못된_값이면_INVALID_INPUT() {
        assertThat(AdminUserService.parseRole("admin")).isEqualTo(Role.ADMIN);
        assertThat(AdminUserService.parseRole("")).isNull();
        assertThatThrownBy(() -> AdminUserService.parseRole("OWNER"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void ID로_읽지_않는_값() {
        assertThat(AdminUserService.parseId("0")).isNull();
        assertThat(AdminUserService.parseId("-3")).isNull();
        assertThat(AdminUserService.parseId("12a")).isNull();
        assertThat(AdminUserService.parseId("1234567890123456789")).isNull();
    }
}
