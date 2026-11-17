package com.seyoung5309.executorserver.domain.admin.service;

import com.seyoung5309.executorserver.domain.admin.dto.AdminNoticeRequest;
import com.seyoung5309.executorserver.domain.admin.dto.AdminNoticeResponse;
import com.seyoung5309.executorserver.domain.notice.entity.Notice;
import com.seyoung5309.executorserver.domain.notice.repository.NoticeRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import com.seyoung5309.executorserver.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** 공지 관리. 숨김·예약·종료 공지까지 모두 다룬다 */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminNoticeService {

    /** 최근 작성순 */
    static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "id");

    private final NoticeRepository noticeRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<AdminNoticeResponse> getAll(int page, int size) {
        Instant now = clock.instant();
        return PageResponse.of(noticeRepository.findAll(PageRequest.of(page, size, NEWEST_FIRST)),
                notice -> AdminNoticeResponse.of(notice, now));
    }

    /** @throws BusinessException NOT_FOUND */
    @Transactional(readOnly = true)
    public AdminNoticeResponse get(Long noticeId) {
        return AdminNoticeResponse.of(find(noticeId), clock.instant());
    }

    /** publishedAt을 생략하면 지금 게시한다. @throws BusinessException INVALID_INPUT (종료가 게시보다 앞) */
    public AdminNoticeResponse create(AdminNoticeRequest request) {
        Instant now = clock.instant();
        Instant publishedAt = seconds(request.publishedAt() == null ? now : request.publishedAt());
        Instant endsAt = checkEndsAt(publishedAt, request.endsAt());
        Notice notice = noticeRepository.save(new Notice(request.category(), request.title().strip(),
                request.content().strip(), Boolean.TRUE.equals(request.isPinned()),
                !Boolean.FALSE.equals(request.isVisible()), publishedAt, endsAt));
        return AdminNoticeResponse.of(notice, now);
    }

    /** publishedAt을 생략하면 기존 값을 그대로 둔다. @throws BusinessException NOT_FOUND, INVALID_INPUT */
    public AdminNoticeResponse update(Long noticeId, AdminNoticeRequest request) {
        Notice notice = find(noticeId);
        Instant publishedAt = request.publishedAt() == null ? notice.getPublishedAt() : seconds(request.publishedAt());
        Instant endsAt = checkEndsAt(publishedAt, request.endsAt());
        notice.update(request.category(), request.title().strip(), request.content().strip(),
                Boolean.TRUE.equals(request.isPinned()), !Boolean.FALSE.equals(request.isVisible()),
                publishedAt, endsAt);
        noticeRepository.flush(); // updatedAt을 응답에 담기 위해
        return AdminNoticeResponse.of(notice, clock.instant());
    }

    /** @throws BusinessException NOT_FOUND */
    public void delete(Long noticeId) {
        noticeRepository.delete(find(noticeId));
    }

    private Notice find(Long noticeId) {
        return noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "공지를 찾을 수 없습니다."));
    }

    private static Instant checkEndsAt(Instant publishedAt, Instant endsAt) {
        if (endsAt == null) {
            return null;
        }
        Instant ends = seconds(endsAt);
        if (!ends.isAfter(publishedAt)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "endsAt은 publishedAt보다 뒤여야 합니다.");
        }
        return ends;
    }

    /** DB 컬럼이 초 단위라 저장 전에 맞춘다 (응답 값과 나중에 읽은 값이 같도록) */
    private static Instant seconds(Instant value) {
        return value.truncatedTo(ChronoUnit.SECONDS);
    }
}
