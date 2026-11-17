package com.seyoung5309.executorserver.domain.notice.service;

import com.seyoung5309.executorserver.domain.notice.dto.NoticeDetailResponse;
import com.seyoung5309.executorserver.domain.notice.dto.NoticeSummaryResponse;
import com.seyoung5309.executorserver.domain.notice.entity.Notice;
import com.seyoung5309.executorserver.domain.notice.repository.NoticeRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import com.seyoung5309.executorserver.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/** 게임 공지 조회. 숨긴 공지, 게시 전(예약)이거나 끝난 공지는 보이지 않는다 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final Clock clock;

    /** 보이는 공지 목록 (상단 고정 먼저, 최신순) */
    public PageResponse<NoticeSummaryResponse> getNotices(int page, int size) {
        return PageResponse.of(noticeRepository.findShown(clock.instant(), PageRequest.of(page, size)),
                NoticeSummaryResponse::from);
    }

    /** @throws BusinessException NOT_FOUND (없거나 지금 보이지 않는 공지) */
    public NoticeDetailResponse getNotice(Long noticeId) {
        Instant now = clock.instant();
        Notice notice = noticeRepository.findById(noticeId)
                .filter(n -> n.isShownAt(now))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "공지를 찾을 수 없습니다."));
        return NoticeDetailResponse.from(notice);
    }
}
