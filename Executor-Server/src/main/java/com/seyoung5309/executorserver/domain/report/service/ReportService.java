package com.seyoung5309.executorserver.domain.report.service;

import com.seyoung5309.executorserver.domain.auth.entity.User;
import com.seyoung5309.executorserver.domain.auth.repository.UserRepository;
import com.seyoung5309.executorserver.domain.report.dto.CreateReportRequest;
import com.seyoung5309.executorserver.domain.report.dto.MyReportResponse;
import com.seyoung5309.executorserver.domain.report.dto.ReportCategoryResponse;
import com.seyoung5309.executorserver.domain.report.dto.ReportStatusResponse;
import com.seyoung5309.executorserver.domain.report.entity.Report;
import com.seyoung5309.executorserver.domain.report.entity.ReportCategory;
import com.seyoung5309.executorserver.domain.report.repository.ReportCategoryRepository;
import com.seyoung5309.executorserver.domain.report.repository.ReportRepository;
import com.seyoung5309.executorserver.domain.stage.entity.Stage;
import com.seyoung5309.executorserver.domain.stage.repository.StageRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import com.seyoung5309.executorserver.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {

    /**
     * 대상 없이 신고할 수 있는 카테고리 이름 (schema.sql 기초 데이터와 같아야 한다).
     * report_categories에 이를 표시하는 컬럼이 없어서 이름으로 구분한다.
     */
    static final Set<String> NO_TARGET_CATEGORIES = Set.of("버그 제보", "기타");

    static final Sort LATEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final ReportRepository reportRepository;
    private final ReportCategoryRepository reportCategoryRepository;
    private final UserRepository userRepository;
    private final StageRepository stageRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<ReportCategoryResponse> getCategories() {
        return reportCategoryRepository.findAllByOrderByIdAsc().stream()
                .map(category -> ReportCategoryResponse.of(category, isTargetRequired(category)))
                .toList();
    }

    /**
     * 신고한다. 대상은 사용자와 스테이지 중 하나만. 버그 제보·기타가 아니면 대상이 꼭 있어야 한다.
     * 자기 자신은 신고할 수 없고, 남의 비공개 맵은 볼 수 없는 맵이므로 없는 스테이지로 취급한다.
     *
     * @throws BusinessException NOT_FOUND(카테고리·대상), INVALID_INPUT(대상 규칙)
     */
    public ReportStatusResponse create(Long userId, CreateReportRequest request) {
        ReportCategory category = reportCategoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "신고 카테고리를 찾을 수 없습니다."));

        if (request.targetUserId() != null && request.targetStageId() != null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "신고 대상은 사용자와 스테이지 중 하나만 지정해 주세요.");
        }
        if (request.targetUserId() == null && request.targetStageId() == null && isTargetRequired(category)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "'" + category.getName() + "' 신고는 대상 사용자나 스테이지를 지정해야 합니다.");
        }

        Report report = new Report(userRepository.getReferenceById(userId), category,
                findTargetUser(userId, request.targetUserId()), findTargetStage(userId, request.targetStageId()),
                request.title().strip(), request.comment().strip(), clock.instant());
        reportRepository.save(report);
        return ReportStatusResponse.from(report);
    }

    /** 내가 한 신고 (최신순) */
    @Transactional(readOnly = true)
    public PageResponse<MyReportResponse> getMyReports(Long userId, int page, int size) {
        return PageResponse.of(reportRepository.findByUserId(userId, PageRequest.of(page, size, LATEST_FIRST)),
                MyReportResponse::from);
    }

    static boolean isTargetRequired(ReportCategory category) {
        return !NO_TARGET_CATEGORIES.contains(category.getName());
    }

    private User findTargetUser(Long userId, Long targetUserId) {
        if (targetUserId == null) {
            return null;
        }
        if (targetUserId.equals(userId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "자기 자신은 신고할 수 없습니다.");
        }
        return userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "신고 대상 사용자를 찾을 수 없습니다."));
    }

    private Stage findTargetStage(Long userId, Long targetStageId) {
        if (targetStageId == null) {
            return null;
        }
        return stageRepository.findById(targetStageId)
                .filter(stage -> stage.isOfficial() || stage.isPublic()
                        || Objects.equals(stage.getCreator().getId(), userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "신고 대상 스테이지를 찾을 수 없습니다."));
    }
}
