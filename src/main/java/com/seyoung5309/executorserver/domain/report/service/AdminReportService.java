package com.seyoung5309.executorserver.domain.report.service;

import com.seyoung5309.executorserver.domain.auth.entity.User;
import com.seyoung5309.executorserver.domain.profile.entity.Profile;
import com.seyoung5309.executorserver.domain.profile.repository.ProfileRepository;
import com.seyoung5309.executorserver.domain.report.dto.AdminReportResponse;
import com.seyoung5309.executorserver.domain.report.dto.ReportStatusResponse;
import com.seyoung5309.executorserver.domain.report.entity.Report;
import com.seyoung5309.executorserver.domain.report.entity.ReportStatus;
import com.seyoung5309.executorserver.domain.report.repository.ReportRepository;
import com.seyoung5309.executorserver.domain.stage.entity.Stage;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import com.seyoung5309.executorserver.global.response.PageResponse;
import com.seyoung5309.executorserver.global.response.UserSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** 관리자 신고 처리 */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminReportService {

    private final ReportRepository reportRepository;
    private final ProfileRepository profileRepository;
    private final Clock clock;

    /**
     * 신고 목록 (최신순). 신고자와 대상 사용자는 프로필 한 번 조회로 붙인다.
     *
     * @param status null이면 전체
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminReportResponse> getReports(ReportStatus status, int page, int size) {
        Page<Report> reports = reportRepository.findForAdmin(status, PageRequest.of(page, size, ReportService.LATEST_FIRST));

        Set<Long> userIds = reports.stream()
                .flatMap(report -> Stream.of(idOf(report.getUser()), idOf(report.getTargetUser())))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Profile> profiles = userIds.isEmpty() ? Map.of()
                : profileRepository.findAllById(userIds).stream()
                        .collect(Collectors.toMap(Profile::getUserId, Function.identity()));

        return PageResponse.of(reports, report -> toResponse(report, profiles));
    }

    /**
     * 상태를 바꾼다. 앞으로만 진행하고(RECEIVED -> IN_PROGRESS -> DONE), DONE이 되면 처리 시각을 기록한다.
     * 같은 상태로 바꾸면 아무것도 바꾸지 않고 현재 상태를 돌려준다.
     *
     * @throws BusinessException NOT_FOUND, INVALID_INPUT(되돌리기)
     */
    public ReportStatusResponse changeStatus(Long reportId, ReportStatus next) {
        Report report = reportRepository.findForUpdate(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "신고를 찾을 수 없습니다."));
        ReportStatus before = report.getStatus();
        if (!report.changeStatus(next, clock.instant())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "신고 상태는 되돌릴 수 없습니다. (현재 " + before + ")");
        }
        return ReportStatusResponse.from(report);
    }

    /** 상태 문자열을 읽는다 (대소문자 무시). 비어 있으면 null(전체), 잘못된 값이면 INVALID_INPUT */
    public static ReportStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ReportStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "status는 RECEIVED, IN_PROGRESS, DONE 중 하나여야 합니다.");
        }
    }

    private AdminReportResponse toResponse(Report report, Map<Long, Profile> profiles) {
        Stage stage = report.getTargetStage();
        return new AdminReportResponse(
                report.getId(),
                new AdminReportResponse.Category(report.getCategory().getId(), report.getCategory().getName()),
                summary(report.getUser(), profiles),
                summary(report.getTargetUser(), profiles),
                stage == null ? null : new AdminReportResponse.TargetStage(stage.getId(), stage.getTitle(), stage.isOfficial()),
                report.getTitle(),
                report.getComment(),
                report.getStatus(),
                report.getCreatedAt(),
                report.getHandledAt());
    }

    private static UserSummary summary(User user, Map<Long, Profile> profiles) {
        Profile profile = user == null ? null : profiles.get(user.getId());
        return profile == null ? null : UserSummary.from(profile);
    }

    private static Long idOf(User user) {
        return user == null ? null : user.getId();
    }
}
