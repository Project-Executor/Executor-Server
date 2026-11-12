package com.seyoung5309.executorserver.domain.report.service;

import com.seyoung5309.executorserver.domain.auth.repository.UserRepository;
import com.seyoung5309.executorserver.domain.profile.entity.Profile;
import com.seyoung5309.executorserver.domain.profile.repository.ProfileRepository;
import com.seyoung5309.executorserver.domain.report.dto.AdminReportResponse;
import com.seyoung5309.executorserver.domain.report.dto.CreateReportRequest;
import com.seyoung5309.executorserver.domain.report.dto.ReportCategoryResponse;
import com.seyoung5309.executorserver.domain.report.entity.Report;
import com.seyoung5309.executorserver.domain.report.entity.ReportCategory;
import com.seyoung5309.executorserver.domain.report.entity.ReportStatus;
import com.seyoung5309.executorserver.domain.report.repository.ReportCategoryRepository;
import com.seyoung5309.executorserver.domain.report.repository.ReportRepository;
import com.seyoung5309.executorserver.domain.stage.repository.StageRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static com.seyoung5309.executorserver.domain.stage.StageFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReportServiceTest {

    private static final Long ME = 1L;
    private static final Instant NOW = Instant.parse("2026-11-12T03:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final ReportCategory nickname = category(1L, "부적절한 닉네임");
    private final ReportCategory badMap = category(2L, "불량 맵");
    private final ReportCategory bug = category(3L, "버그 제보");
    private final ReportCategory etc = category(4L, "기타");

    @Nested
    class 신고하기 {
        private ReportRepository reportRepository;
        private ReportCategoryRepository categoryRepository;
        private UserRepository userRepository;
        private StageRepository stageRepository;
        private ReportService service;

        @BeforeEach
        void setUp() {
            reportRepository = mock(ReportRepository.class);
            categoryRepository = mock(ReportCategoryRepository.class);
            userRepository = mock(UserRepository.class);
            stageRepository = mock(StageRepository.class);
            service = new ReportService(reportRepository, categoryRepository, userRepository, stageRepository, CLOCK);
            for (ReportCategory c : List.of(nickname, badMap, bug, etc)) {
                when(categoryRepository.findById(c.getId())).thenReturn(Optional.of(c));
            }
            when(userRepository.getReferenceById(anyLong())).thenAnswer(inv -> user(inv.getArgument(0)));
            when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
        }

        @Test
        void 카테고리마다_대상_필요_여부가_붙는다() {
            when(categoryRepository.findAllByOrderByIdAsc()).thenReturn(List.of(nickname, badMap, bug, etc));

            assertThat(service.getCategories()).extracting(ReportCategoryResponse::targetRequired)
                    .containsExactly(true, true, false, false);
        }

        @Test
        void 사용자를_신고하면_RECEIVED로_접수된다() {
            var response = service.create(ME, request(1L, 2L, null));

            assertThat(response.status()).isEqualTo(ReportStatus.RECEIVED);
            ArgumentCaptor<Report> saved = ArgumentCaptor.forClass(Report.class);
            verify(reportRepository).save(saved.capture());
            assertThat(saved.getValue().getTargetUser().getId()).isEqualTo(2L);
            assertThat(saved.getValue().getTitle()).isEqualTo("제목");
            assertThat(saved.getValue().getCreatedAt()).isEqualTo(NOW);
        }

        @Test
        void 버그_제보와_기타는_대상_없이_신고할_수_있다() {
            service.create(ME, request(3L, null, null));
            service.create(ME, request(4L, null, null));

            verify(reportRepository, times(2)).save(any());
        }

        @Test
        void 그_밖의_카테고리는_대상이_없으면_INVALID_INPUT() {
            assertErrorCode(() -> service.create(ME, request(2L, null, null)), ErrorCode.INVALID_INPUT);
            verify(reportRepository, never()).save(any());
        }

        @Test
        void 대상을_둘_다_지정하면_INVALID_INPUT() {
            assertErrorCode(() -> service.create(ME, request(1L, 2L, 20L)), ErrorCode.INVALID_INPUT);
        }

        @Test
        void 자기_자신은_신고할_수_없다() {
            assertErrorCode(() -> service.create(ME, request(1L, ME, null)), ErrorCode.INVALID_INPUT);
        }

        @Test
        void 없는_카테고리나_대상은_NOT_FOUND() {
            assertErrorCode(() -> service.create(ME, request(99L, 2L, null)), ErrorCode.NOT_FOUND);
            assertErrorCode(() -> service.create(ME, request(1L, 99L, null)), ErrorCode.NOT_FOUND);
            assertErrorCode(() -> service.create(ME, request(2L, null, 99L)), ErrorCode.NOT_FOUND);
        }

        @Test
        void 남의_비공개_맵은_없는_스테이지로_보고_공개_맵과_내_맵은_신고할_수_있다() {
            when(stageRepository.findById(30L)).thenReturn(Optional.of(userMap(30L, user(7L), false)));
            when(stageRepository.findById(31L)).thenReturn(Optional.of(userMap(31L, user(7L), true)));
            when(stageRepository.findById(32L)).thenReturn(Optional.of(userMap(32L, user(ME), false)));
            when(stageRepository.findById(20L)).thenReturn(Optional.of(officialStage(20L, operation(1L, "op", 1), 1)));

            assertErrorCode(() -> service.create(ME, request(2L, null, 30L)), ErrorCode.NOT_FOUND);
            service.create(ME, request(2L, null, 31L));
            service.create(ME, request(2L, null, 32L));
            service.create(ME, request(2L, null, 20L));
            verify(reportRepository, times(3)).save(any());
        }

        private CreateReportRequest request(Long categoryId, Long targetUserId, Long targetStageId) {
            return new CreateReportRequest(categoryId, targetUserId, targetStageId, " 제목 ", " 내용 ");
        }
    }

    @Nested
    class 관리자 {
        private ReportRepository reportRepository;
        private ProfileRepository profileRepository;
        private AdminReportService service;

        @BeforeEach
        void setUp() {
            reportRepository = mock(ReportRepository.class);
            profileRepository = mock(ProfileRepository.class);
            service = new AdminReportService(reportRepository, profileRepository, CLOCK);
        }

        @Test
        void 상태는_앞으로만_진행하고_DONE이면_처리_시각을_기록한다() {
            Report report = report(7L, ReportStatus.RECEIVED);
            when(reportRepository.findForUpdate(7L)).thenReturn(Optional.of(report));

            assertThat(service.changeStatus(7L, ReportStatus.IN_PROGRESS).handledAt()).isNull();
            var done = service.changeStatus(7L, ReportStatus.DONE);

            assertThat(done.status()).isEqualTo(ReportStatus.DONE);
            assertThat(done.handledAt()).isEqualTo(NOW);
            assertErrorCode(() -> service.changeStatus(7L, ReportStatus.IN_PROGRESS), ErrorCode.INVALID_INPUT);
            assertErrorCode(() -> service.changeStatus(7L, ReportStatus.RECEIVED), ErrorCode.INVALID_INPUT);
        }

        @Test
        void 접수에서_바로_완료로_갈_수_있고_같은_상태는_그대로_둔다() {
            Report report = report(7L, ReportStatus.RECEIVED);
            when(reportRepository.findForUpdate(7L)).thenReturn(Optional.of(report));

            service.changeStatus(7L, ReportStatus.DONE);
            ReflectionTestUtils.setField(report, "handledAt", NOW.minusSeconds(60));

            assertThat(service.changeStatus(7L, ReportStatus.DONE).handledAt()).isEqualTo(NOW.minusSeconds(60));
        }

        @Test
        void 없는_신고는_NOT_FOUND() {
            when(reportRepository.findForUpdate(99L)).thenReturn(Optional.empty());

            assertErrorCode(() -> service.changeStatus(99L, ReportStatus.DONE), ErrorCode.NOT_FOUND);
        }

        @Test
        void 목록에_신고자와_대상을_붙이고_탈퇴한_사용자는_null() {
            Report withUsers = report(7L, ReportStatus.RECEIVED);
            ReflectionTestUtils.setField(withUsers, "user", user(1L));
            ReflectionTestUtils.setField(withUsers, "targetUser", user(2L));
            Report withdrawn = report(8L, ReportStatus.DONE);
            ReflectionTestUtils.setField(withdrawn, "targetStage", userMap(30L, user(7L), true));
            when(reportRepository.findForAdmin(isNull(), any())).thenReturn(new PageImpl<>(List.of(withUsers, withdrawn)));
            when(profileRepository.findAllById(any())).thenReturn(List.of(profile(1L, "reporter"), profile(2L, "target")));

            var page = service.getReports(null, 0, 20);

            AdminReportResponse first = page.content().get(0);
            assertThat(first.reporter().nickname()).isEqualTo("reporter");
            assertThat(first.targetUser().nickname()).isEqualTo("target");
            AdminReportResponse second = page.content().get(1);
            assertThat(second.reporter()).isNull();
            assertThat(second.targetStage()).isEqualTo(new AdminReportResponse.TargetStage(30L, "user-map-30", false));
        }

        @Test
        void 상태_문자열은_대소문자를_가리지_않고_비어_있으면_전체() {
            assertThat(AdminReportService.parseStatus("in_progress")).isEqualTo(ReportStatus.IN_PROGRESS);
            assertThat(AdminReportService.parseStatus(null)).isNull();
            assertErrorCode(() -> AdminReportService.parseStatus("CLOSED"), ErrorCode.INVALID_INPUT);
        }

        private Report report(Long id, ReportStatus status) {
            Report report = new Report(null, bug, null, null, "제목", "내용", NOW.minusSeconds(3600));
            ReflectionTestUtils.setField(report, "id", id);
            ReflectionTestUtils.setField(report, "status", status);
            return report;
        }
    }

    private static ReportCategory category(Long id, String name) {
        ReportCategory category = BeanUtils.instantiateClass(ReportCategory.class);
        ReflectionTestUtils.setField(category, "id", id);
        ReflectionTestUtils.setField(category, "name", name);
        return category;
    }

    private static Profile profile(Long userId, String nickname) {
        Profile profile = new Profile(user(userId), nickname);
        ReflectionTestUtils.setField(profile, "userId", userId);
        return profile;
    }

    private static void assertErrorCode(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
