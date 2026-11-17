package com.seyoung5309.executorserver.domain.admin.service;

import com.seyoung5309.executorserver.domain.auth.entity.Account;
import com.seyoung5309.executorserver.domain.auth.entity.Role;
import com.seyoung5309.executorserver.domain.profile.entity.User;
import com.seyoung5309.executorserver.domain.item.repository.UserItemRepository;
import com.seyoung5309.executorserver.domain.profile.repository.UserRepository;
import com.seyoung5309.executorserver.domain.stage.repository.StarSummary;
import com.seyoung5309.executorserver.domain.stage.repository.UserStageRepository;
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
    private UserItemRepository userItemRepository;
    private UserStageRepository userStageRepository;
    private AdminUserService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userItemRepository = mock(UserItemRepository.class);
        userStageRepository = mock(UserStageRepository.class);
        service = new AdminUserService(userRepository, userItemRepository, userStageRepository);
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

    @Test
    void ID_조회는_중복과_null을_빼고_한_번에_찾는다() {
        when(userRepository.findAllWithAccountByIdIn(any())).thenReturn(List.of());
        java.util.List<Long> ids = new java.util.ArrayList<>(List.of(3L, 1L, 3L));
        ids.add(null);

        service.lookup(ids);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Collection<Long>> captor = ArgumentCaptor.forClass(java.util.Collection.class);
        verify(userRepository).findAllWithAccountByIdIn(captor.capture());
        assertThat(captor.getValue()).containsExactly(3L, 1L);
    }

    @Test
    void ID_조회가_비었거나_너무_많으면_INVALID_INPUT() {
        List<Long> tooMany = java.util.stream.LongStream.rangeClosed(1, AdminUserService.LOOKUP_MAX + 1).boxed().toList();
        for (List<Long> ids : java.util.Arrays.asList(null, List.<Long>of(), tooMany)) {
            assertThatThrownBy(() -> service.lookup(ids))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        }
        verify(userRepository, never()).findAllWithAccountByIdIn(any());
    }

    // ---------- 상세 ----------

    @Test
    void 상세는_별_합계와_보유_아이템을_담는다() {
        User profile = profile(7L, Role.USER, "집행자");
        when(userRepository.findWithUserById(7L)).thenReturn(java.util.Optional.of(profile));
        when(userStageRepository.summarizeOfficialStars(7L)).thenReturn(new StarSummary(3, 2, 1));
        when(userItemRepository.findMyItems(7L, null)).thenReturn(List.of());

        var detail = service.getDetail(7L);

        assertThat(detail.email()).isEqualTo("user7@example.com");
        assertThat(detail.clearedStageCount()).isEqualTo(3);
        assertThat(detail.totalStars()).isEqualTo(6);
        assertThat(detail.items()).isEmpty();
    }

    @Test
    void 없는_사용자_상세는_NOT_FOUND() {
        when(userRepository.findWithUserById(9L)).thenReturn(java.util.Optional.empty());
        assertErrorCode(() -> service.getDetail(9L), ErrorCode.NOT_FOUND);
    }

    // ---------- 코인 ----------

    @Test
    void 코인을_더하면_조정한_뒤의_코인을_돌려준다() {
        when(userRepository.existsById(7L)).thenReturn(true);
        when(userRepository.findCoinByUserId(7L)).thenReturn(java.util.Optional.of(150));

        var res = service.adjustCoin(7L, 100);

        verify(userRepository).addCoin(7L, 100);
        verify(userRepository, never()).spendCoin(anyLong(), anyInt());
        assertThat(res.coin()).isEqualTo(150);
    }

    @Test
    void 코인을_빼면_조건부로_차감하고_모자라면_NOT_ENOUGH_COIN() {
        when(userRepository.existsById(7L)).thenReturn(true);
        when(userRepository.spendCoin(7L, 30)).thenReturn(1);
        when(userRepository.findCoinByUserId(7L)).thenReturn(java.util.Optional.of(20));
        assertThat(service.adjustCoin(7L, -30).coin()).isEqualTo(20);

        when(userRepository.spendCoin(7L, 500)).thenReturn(0);
        assertThatThrownBy(() -> service.adjustCoin(7L, -500))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_ENOUGH_COIN);
                    assertThat(e.getMessage()).contains("보유 20");
                });
        verify(userRepository, never()).addCoin(anyLong(), anyInt());
    }

    @Test
    void 코인_0이나_없는_사용자는_바꾸지_않는다() {
        assertErrorCode(() -> service.adjustCoin(7L, 0), ErrorCode.INVALID_INPUT);
        when(userRepository.existsById(9L)).thenReturn(false);
        assertErrorCode(() -> service.adjustCoin(9L, 10), ErrorCode.NOT_FOUND);
        verify(userRepository, never()).addCoin(anyLong(), anyInt());
        verify(userRepository, never()).spendCoin(anyLong(), anyInt());
    }

    // ---------- 권한 ----------

    @Test
    void 권한을_바꾼다() {
        User profile = profile(7L, Role.USER, "집행자");
        when(userRepository.findWithUserById(7L)).thenReturn(java.util.Optional.of(profile));

        var res = service.changeRole(1L, 7L, Role.ADMIN);

        assertThat(res.role()).isEqualTo(Role.ADMIN);
        assertThat(profile.getAccount().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void 자기_권한은_바꿀_수_없다() {
        assertErrorCode(() -> service.changeRole(1L, 1L, Role.USER), ErrorCode.INVALID_INPUT);
        verify(userRepository, never()).findWithUserById(any());
    }

    private static User profile(Long id, Role role, String nickname) {
        Account account = user(id);
        ReflectionTestUtils.setField(account, "role", role);
        User profile = new User(account, nickname);
        ReflectionTestUtils.setField(profile, "userId", id);
        return profile;
    }

    private static void assertErrorCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, ErrorCode code) {
        assertThatThrownBy(call).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(code));
    }
}
